package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.GpxDocument
import com.ralphdietrich.corider.domain.model.GpxPoint
import com.ralphdietrich.corider.domain.model.GpxTrack
import com.ralphdietrich.corider.domain.model.RouteRepresentationQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteDetailsAnalyzerTest {
    @Test
    fun `creates reliable summary profile and representation quality`() {
        val points = listOf(
            point(52.0, 13.0, 20.0),
            point(52.1, 13.0, 30.0),
            point(52.2, 13.0, 10.0),
        )

        val details = RouteDetailsAnalyzer(smoothingRadius = 0).analyze(document(points))

        assertEquals(3, details.trackPointCount)
        assertTrue(details.totalDistanceMeters > 20_000.0)
        assertEquals(10.0, details.minimumElevationMeters)
        assertEquals(30.0, details.maximumElevationMeters)
        assertEquals(3, details.elevationProfile.size)
        assertEquals(RouteRepresentationQuality.GOOD, details.representation.quality)
        assertEquals(1, details.representation.shapingPointCount)
    }

    @Test
    fun `reduces elevation drawing samples without changing route statistics`() {
        val points = (0 until 100).map { index ->
            point(52.0 + index * 0.001, 13.0, index.toDouble())
        }

        val details = RouteDetailsAnalyzer(
            maximumProfileSamples = 10,
            smoothingRadius = 0,
        ).analyze(document(points))

        assertTrue(details.elevationProfile.size <= 10)
        assertEquals(0.0, details.minimumElevationMeters)
        assertEquals(99.0, details.maximumElevationMeters)
        assertEquals(100, details.trackPointCount)
    }

    @Test
    fun `does not turn unrelated standalone waypoints into stops`() {
        val track = listOf(point(52.0, 13.0), point(53.0, 10.0))
        val document = GpxDocument(
            name = null,
            tracks = listOf(GpxTrack(null, listOf(track))),
            waypoints = listOf(point(52.5, 12.0, 50.0)),
        )

        val details = RouteDetailsAnalyzer().analyze(document)

        assertNull(details.minimumElevationMeters)
        assertTrue(details.elevationProfile.isEmpty())
        assertEquals(0, details.representation.shapingPointCount)
    }

    private fun document(points: List<GpxPoint>) = GpxDocument(
        name = null,
        tracks = listOf(GpxTrack(null, listOf(points))),
    )

    private fun point(
        latitude: Double,
        longitude: Double,
        elevation: Double? = null,
    ) = GpxPoint(latitude, longitude, elevationMeters = elevation)
}
