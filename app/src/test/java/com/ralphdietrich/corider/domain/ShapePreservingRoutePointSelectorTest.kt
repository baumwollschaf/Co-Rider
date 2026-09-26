package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.GpxDocument
import com.ralphdietrich.corider.domain.model.GpxPoint
import com.ralphdietrich.corider.domain.model.GpxTrack
import org.junit.Assert.assertEquals
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
    }

    private fun document(points: List<GpxPoint>) = GpxDocument(
        name = null,
        tracks = listOf(GpxTrack(null, listOf(points))),
    )

    private fun point(latitude: Double, longitude: Double) = GpxPoint(latitude, longitude)
}
