package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.GpxDocument
import com.ralphdietrich.corider.domain.model.GpxPoint
import java.net.URLEncoder
import java.util.Locale

class DefaultMapsRouteUrlFactory(
    private val pointSelector: RoutePointSelector = ShapePreservingRoutePointSelector(),
    private val maxWaypoints: Int = GoogleMapsRouteConstraints.MAX_INTERMEDIATE_POINTS,
) : MapsRouteUrlFactory {
    override fun createUrl(document: GpxDocument): String {
        val route = pointSelector.select(document, maxWaypoints)
        return buildString {
            append("https://www.google.com/maps/dir/?api=1")
            appendParameter("origin", route.origin.coordinate())
            appendParameter("destination", route.destination.coordinate())
            appendParameter("travelmode", "driving")
            if (route.waypoints.isNotEmpty()) {
                appendParameter("waypoints", route.waypoints.joinToString("|") { it.coordinate() })
            }
        }
    }

    private fun StringBuilder.appendParameter(name: String, value: String) {
        append('&')
        append(name)
        append('=')
        append(URLEncoder.encode(value, Charsets.UTF_8.name()))
    }

    private fun GpxPoint.coordinate(): String =
        String.format(Locale.US, "%.6f,%.6f", latitude, longitude)
}
