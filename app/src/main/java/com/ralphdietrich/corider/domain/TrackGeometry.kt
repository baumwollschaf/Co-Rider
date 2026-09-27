package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.GpxDocument
import com.ralphdietrich.corider.domain.model.GpxPoint
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object TrackGeometry {
    fun usableSegments(document: GpxDocument): List<List<GpxPoint>> = document.tracks
        .asSequence()
        .flatMap { it.segments.asSequence() }
        .filter { it.size >= 2 }
        .toList()

    fun primarySegment(document: GpxDocument): List<GpxPoint> = usableSegments(document)
        .maxByOrNull(::pathLengthMeters)
        ?: throw RouteConversionException("The GPX file contains no usable track")

    fun pathLengthMeters(points: List<GpxPoint>): Double =
        points.zipWithNext().sumOf { (first, second) -> distanceMeters(first, second) }

    fun distanceMeters(first: GpxPoint, second: GpxPoint): Double {
        val latitudeDelta = radians(second.latitude - first.latitude)
        val longitudeDelta = radians(second.longitude - first.longitude)
        val firstLatitude = radians(first.latitude)
        val secondLatitude = radians(second.latitude)
        val haversine = sin(latitudeDelta / 2).let { it * it } +
            cos(firstLatitude) * cos(secondLatitude) *
            sin(longitudeDelta / 2).let { it * it }
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
    }

    private fun radians(degrees: Double): Double = degrees * PI / 180.0

    private const val EARTH_RADIUS_METERS = 6_371_000.0
}
