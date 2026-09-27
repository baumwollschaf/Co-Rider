package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.GpxDocument
import com.ralphdietrich.corider.domain.model.GpxPoint
import com.ralphdietrich.corider.domain.model.RouteRepresentation
import com.ralphdietrich.corider.domain.model.RouteRepresentationQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultMapsRouteUrlFactoryTest {
    @Test
    fun `creates encoded directions URL with driving mode and ordered waypoints`() {
        val selector = RoutePointSelector { _, _ ->
            MapsRoutePoints(
                origin = GpxPoint(52.0, 13.0),
                destination = GpxPoint(53.0, 10.0),
                waypoints = listOf(GpxPoint(52.5, 12.0), GpxPoint(52.8, 11.0)),
                representation = RouteRepresentation(
                    quality = RouteRepresentationQuality.GOOD,
                    shapingPointCount = 2,
                    maximumDeviationMeters = 0.0,
                    maximumUnsupportedDistanceMeters = 0.0,
                    ignoredSegmentCount = 0,
                ),
            )
        }

        val url = DefaultMapsRouteUrlFactory(selector).createUrl(
            GpxDocument(name = null, tracks = emptyList()),
        )

        assertEquals(
            "https://www.google.com/maps/dir/?api=1" +
                "&origin=52.000000%2C13.000000" +
                "&destination=53.000000%2C10.000000" +
                "&travelmode=driving" +
                "&waypoints=52.500000%2C12.000000%7C52.800000%2C11.000000",
            url,
        )
    }
}
