package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.GpxDocument
import com.ralphdietrich.corider.domain.model.GpxPoint
import com.ralphdietrich.corider.domain.model.RouteRepresentation
import com.ralphdietrich.corider.domain.model.RouteRepresentationQuality
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot

data class MapsRoutePoints(
    val origin: GpxPoint,
    val destination: GpxPoint,
    val waypoints: List<GpxPoint>,
    val representation: RouteRepresentation,
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
        val usableSegments = TrackGeometry.usableSegments(document)
        val primarySegment = usableSegments.maxByOrNull(TrackGeometry::pathLengthMeters)
            ?: throw RouteConversionException("The GPX file contains no usable track")
        val points = withoutConsecutiveDuplicates(primarySegment)
        if (points.size < 2) {
            throw RouteConversionException("The GPX track does not cover a usable route")
        }

        val cumulativeDistances = cumulativeDistances(points)
        val selected = sortedSetOf(0, points.lastIndex)
        while (selected.size < maxWaypoints + 2) {
            val candidate = selected.zipWithNext()
                .mapNotNull { (start, end) ->
                    strongestCandidate(points, cumulativeDistances, start, end)
                }
                .maxWithOrNull(
                    compareBy<Candidate>(Candidate::errorMeters)
                        .thenBy(Candidate::unsupportedDistanceMeters),
                )
                ?: break
            selected += candidate.index
        }

        val selectedPoints = selected.map(points::get)
        val maximumDeviation = selected.zipWithNext()
            .mapNotNull { (start, end) ->
                strongestCandidate(points, cumulativeDistances, start, end)?.errorMeters
            }
            .maxOrNull()
            ?: 0.0
        val maximumUnsupportedDistance = selected.zipWithNext()
            .maxOfOrNull { (start, end) -> cumulativeDistances[end] - cumulativeDistances[start] }
            ?: 0.0
        val totalDistance = cumulativeDistances.last()
        val ignoredSegmentCount = (usableSegments.size - 1).coerceAtLeast(0)
        return MapsRoutePoints(
            origin = selectedPoints.first(),
            destination = selectedPoints.last(),
            waypoints = selectedPoints.drop(1).dropLast(1),
            representation = RouteRepresentation(
                quality = classifyQuality(
                    maximumDeviationMeters = maximumDeviation,
                    totalDistanceMeters = totalDistance,
                    ignoredSegmentCount = ignoredSegmentCount,
                ),
                shapingPointCount = (selectedPoints.size - 2).coerceAtLeast(0),
                maximumDeviationMeters = maximumDeviation,
                maximumUnsupportedDistanceMeters = maximumUnsupportedDistance,
                ignoredSegmentCount = ignoredSegmentCount,
            ),
        )
    }

    private fun cumulativeDistances(points: List<GpxPoint>): List<Double> {
        var distance = 0.0
        return points.mapIndexed { index, point ->
            if (index > 0) distance += TrackGeometry.distanceMeters(points[index - 1], point)
            distance
        }
    }

    private fun classifyQuality(
        maximumDeviationMeters: Double,
        totalDistanceMeters: Double,
        ignoredSegmentCount: Int,
    ): RouteRepresentationQuality {
        if (ignoredSegmentCount > 0) return RouteRepresentationQuality.COMPLEX
        val goodThreshold = (totalDistanceMeters * GOOD_DEVIATION_RATIO)
            .coerceIn(MINIMUM_GOOD_DEVIATION_METERS, MAXIMUM_GOOD_DEVIATION_METERS)
        val limitedThreshold = (totalDistanceMeters * LIMITED_DEVIATION_RATIO)
            .coerceIn(MINIMUM_LIMITED_DEVIATION_METERS, MAXIMUM_LIMITED_DEVIATION_METERS)
        return when {
            maximumDeviationMeters <= goodThreshold -> RouteRepresentationQuality.GOOD
            maximumDeviationMeters <= limitedThreshold -> RouteRepresentationQuality.LIMITED
            else -> RouteRepresentationQuality.COMPLEX
        }
    }

    private fun strongestCandidate(
        points: List<GpxPoint>,
        cumulativeDistances: List<Double>,
        startIndex: Int,
        endIndex: Int,
    ): Candidate? {
        if (endIndex - startIndex < 2) return null
        val start = points[startIndex]
        val end = points[endIndex]
        val intervalDistance = cumulativeDistances[endIndex] - cumulativeDistances[startIndex]
        val middleDistance = (cumulativeDistances[startIndex] + cumulativeDistances[endIndex]) / 2.0
        return (startIndex + 1 until endIndex)
            .map { index ->
                Candidate(
                    index = index,
                    errorMeters = perpendicularDistance(points[index], start, end),
                    unsupportedDistanceMeters = intervalDistance,
                    distanceFromMiddleMeters = abs(cumulativeDistances[index] - middleDistance),
                )
            }
            .maxWithOrNull(
                compareBy<Candidate>(Candidate::errorMeters)
                    .thenByDescending(Candidate::distanceFromMiddleMeters),
            )
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

    private data class Candidate(
        val index: Int,
        val errorMeters: Double,
        val unsupportedDistanceMeters: Double,
        val distanceFromMiddleMeters: Double,
    )

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
        const val GOOD_DEVIATION_RATIO = 0.01
        const val LIMITED_DEVIATION_RATIO = 0.03
        const val MINIMUM_GOOD_DEVIATION_METERS = 250.0
        const val MAXIMUM_GOOD_DEVIATION_METERS = 2_000.0
        const val MINIMUM_LIMITED_DEVIATION_METERS = 1_000.0
        const val MAXIMUM_LIMITED_DEVIATION_METERS = 6_000.0
    }
}
