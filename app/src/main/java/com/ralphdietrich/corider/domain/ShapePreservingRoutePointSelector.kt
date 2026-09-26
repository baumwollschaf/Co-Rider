package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.GpxDocument
import com.ralphdietrich.corider.domain.model.GpxPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot

data class MapsRoutePoints(
    val origin: GpxPoint,
    val destination: GpxPoint,
    val waypoints: List<GpxPoint>,
)

fun interface RoutePointSelector {
    fun select(document: GpxDocument, maxWaypoints: Int): MapsRoutePoints
}

/**
 * Retains the points with the greatest geometric deviation from the progressively
 * simplified track. Unlike fixed-distance sampling, this prioritizes route-shaping bends.
 */
class ShapePreservingRoutePointSelector : RoutePointSelector {
    override fun select(document: GpxDocument, maxWaypoints: Int): MapsRoutePoints {
        require(maxWaypoints >= 0)
        val points = withoutConsecutiveDuplicates(TrackGeometry.primarySegment(document))
        if (points.size < 2) {
            throw RouteConversionException("The GPX track does not cover a usable route")
        }

        val selected = sortedSetOf(0, points.lastIndex)
        while (selected.size < maxWaypoints + 2) {
            val candidate = selected.zipWithNext()
                .mapNotNull { (start, end) -> strongestCandidate(points, start, end) }
                .maxByOrNull(Candidate::errorMeters)
                ?: break
            selected += candidate.index
        }

        val selectedPoints = selected.map(points::get)
        return MapsRoutePoints(
            origin = selectedPoints.first(),
            destination = selectedPoints.last(),
            waypoints = selectedPoints.drop(1).dropLast(1),
        )
    }

    private fun strongestCandidate(
        points: List<GpxPoint>,
        startIndex: Int,
        endIndex: Int,
    ): Candidate? {
        if (endIndex - startIndex < 2) return null
        val start = points[startIndex]
        val end = points[endIndex]
        return (startIndex + 1 until endIndex)
            .map { index -> Candidate(index, perpendicularDistance(points[index], start, end)) }
            .maxByOrNull(Candidate::errorMeters)
    }

    private fun perpendicularDistance(
        point: GpxPoint,
        start: GpxPoint,
        end: GpxPoint,
    ): Double {
        val referenceLatitude = radians((start.latitude + end.latitude + point.latitude) / 3.0)
        fun coordinates(value: GpxPoint): Pair<Double, Double> =
            radians(value.longitude) * cos(referenceLatitude) to radians(value.latitude)

        val (startX, startY) = coordinates(start)
        val (endX, endY) = coordinates(end)
        val (pointX, pointY) = coordinates(point)
        val dx = endX - startX
        val dy = endY - startY
        val denominator = dx * dx + dy * dy
        val position = if (denominator == 0.0) {
            0.0
        } else {
            ((pointX - startX) * dx + (pointY - startY) * dy) / denominator
        }.coerceIn(0.0, 1.0)
        return hypot(
            pointX - (startX + position * dx),
            pointY - (startY + position * dy),
        ) * EARTH_RADIUS_METERS
    }

    private fun withoutConsecutiveDuplicates(points: List<GpxPoint>): List<GpxPoint> =
        points.fold(mutableListOf()) { result, point ->
            if (result.lastOrNull()?.let { sameCoordinates(it, point) } != true) result += point
            result
        }

    private fun sameCoordinates(first: GpxPoint, second: GpxPoint): Boolean =
        first.latitude == second.latitude && first.longitude == second.longitude

    private fun radians(degrees: Double): Double = degrees * PI / 180.0

    private data class Candidate(val index: Int, val errorMeters: Double)

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
