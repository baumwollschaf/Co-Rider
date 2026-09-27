package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.ElevationSample
import com.ralphdietrich.corider.domain.model.GpxDocument
import com.ralphdietrich.corider.domain.model.GpxPoint
import com.ralphdietrich.corider.domain.model.RouteDetails
import kotlin.math.ceil

class RouteDetailsAnalyzer(
    private val maximumProfileSamples: Int = 320,
    private val smoothingRadius: Int = 2,
    private val pointSelector: RoutePointSelector = ShapePreservingRoutePointSelector(),
    private val maximumWaypoints: Int = GoogleMapsRouteConstraints.MAX_INTERMEDIATE_POINTS,
) {
    init {
        require(maximumProfileSamples >= 2)
        require(smoothingRadius >= 0)
    }

    fun analyze(document: GpxDocument): RouteDetails {
        val points = TrackGeometry.primarySegment(document)
        val distances = cumulativeDistances(points)
        val rawElevationSamples = points.mapIndexedNotNull { index, point ->
            point.elevationMeters?.takeIf(Double::isFinite)?.let { elevation ->
                ElevationSample(distances[index], elevation)
            }
        }
        val smoothedProfile = smooth(rawElevationSamples)
        val representation = pointSelector.select(document, maximumWaypoints).representation

        return RouteDetails(
            totalDistanceMeters = distances.last(),
            trackPointCount = points.size,
            minimumElevationMeters = rawElevationSamples.minOfOrNull(ElevationSample::elevationMeters),
            maximumElevationMeters = rawElevationSamples.maxOfOrNull(ElevationSample::elevationMeters),
            elevationProfile = reduce(smoothedProfile),
            representation = representation,
        )
    }

    private fun cumulativeDistances(points: List<GpxPoint>): List<Double> {
        var distance = 0.0
        return points.mapIndexed { index, point ->
            if (index > 0) distance += TrackGeometry.distanceMeters(points[index - 1], point)
            distance
        }
    }

    private fun smooth(samples: List<ElevationSample>): List<ElevationSample> {
        if (smoothingRadius == 0 || samples.size < 3) return samples
        return samples.mapIndexed { index, sample ->
            val from = (index - smoothingRadius).coerceAtLeast(0)
            val to = (index + smoothingRadius).coerceAtMost(samples.lastIndex)
            val average = (from..to).map { samples[it].elevationMeters }.average()
            sample.copy(elevationMeters = average)
        }
    }

    private fun reduce(samples: List<ElevationSample>): List<ElevationSample> {
        if (samples.size <= maximumProfileSamples) return samples
        val bucketSize = ceil(samples.size.toDouble() / maximumProfileSamples).toInt()
        val reduced = samples.chunked(bucketSize).map { bucket ->
            ElevationSample(
                distanceMeters = bucket.map(ElevationSample::distanceMeters).average(),
                elevationMeters = bucket.map(ElevationSample::elevationMeters).average(),
            )
        }.toMutableList()
        reduced[0] = samples.first()
        reduced[reduced.lastIndex] = samples.last()
        return reduced
    }
}
