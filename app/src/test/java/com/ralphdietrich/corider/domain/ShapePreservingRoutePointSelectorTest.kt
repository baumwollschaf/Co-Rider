package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.GpxDocument
import com.ralphdietrich.corider.domain.model.GpxPoint
import com.ralphdietrich.corider.domain.model.GpxTrack
import com.ralphdietrich.corider.domain.model.RouteRepresentationQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapePreservingRoutePointSelectorTest {
    private val selector = ShapePreservingRoutePointSelector()

    @Test
    fun `selects the strongest bend instead of a fixed interval`() {
        val points = listOf(
            point(0.0, 0.0),
            point(0.0, 1.0),
            point(1.0, 1.0),
            point(1.0, 2.0),
        )

        val result = selector.select(document(points), maxWaypoints = 1)

        assertEquals(points.first(), result.origin)
        assertEquals(points.last(), result.destination)
        assertEquals(1, result.waypoints.size)
        assertEquals(point(0.0, 1.0), result.waypoints.single())
        assertTrue(result.representation.maximumDeviationMeters > 50_000.0)
        assertEquals(RouteRepresentationQuality.COMPLEX, result.representation.quality)
    }

    @Test
    fun `uses the longest continuous segment and ignores standalone POIs`() {
        val shortSegment = listOf(point(1.0, 1.0), point(1.001, 1.001))
        val longSegment = listOf(point(52.0, 13.0), point(53.0, 10.0))
        val document = GpxDocument(
            name = null,
            tracks = listOf(GpxTrack(null, listOf(shortSegment, longSegment))),
            waypoints = listOf(point(20.0, 20.0)),
        )

        val result = selector.select(document, maxWaypoints = 9)

        assertEquals(longSegment.first(), result.origin)
        assertEquals(longSegment.last(), result.destination)
        assertEquals(emptyList<GpxPoint>(), result.waypoints)
        assertEquals(1, result.representation.ignoredSegmentCount)
        assertEquals(RouteRepresentationQuality.COMPLEX, result.representation.quality)
    }

    @Test
    fun `reports good quality for a track represented within the fixed budget`() {
        val points = (0..20).map { index -> point(52.0 + index * 0.001, 13.0) }

        val result = selector.select(document(points), maxWaypoints = 9)

        assertEquals(RouteRepresentationQuality.GOOD, result.representation.quality)
        assertEquals(0.0, result.representation.maximumDeviationMeters, 0.001)
        assertEquals(9, result.waypoints.size)
        assertTrue(result.representation.maximumUnsupportedDistanceMeters < 400.0)
    }

    @Test
    fun `keeps late large deviation in the waypoint budget`() {
        val points = listOf(
            point(52.0, 13.0),
            point(52.0, 13.1),
            point(52.0, 13.2),
            point(52.0, 13.3),
            point(53.0, 13.3),
            point(52.0, 13.4),
        )

        val result = selector.select(document(points), maxWaypoints = 1)

        assertEquals(point(53.0, 13.3), result.waypoints.single())
    }

    private fun document(points: List<GpxPoint>) = GpxDocument(
        name = null,
        tracks = listOf(GpxTrack(null, listOf(points))),
    )

    private fun point(latitude: Double, longitude: Double) = GpxPoint(latitude, longitude)
}
