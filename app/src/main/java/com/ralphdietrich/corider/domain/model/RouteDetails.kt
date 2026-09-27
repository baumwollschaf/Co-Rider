package com.ralphdietrich.corider.domain.model

data class RouteDetails(
    val totalDistanceMeters: Double,
    val trackPointCount: Int,
    val minimumElevationMeters: Double?,
    val maximumElevationMeters: Double?,
    val elevationProfile: List<ElevationSample>,
    val representation: RouteRepresentation,
)

data class ElevationSample(
    val distanceMeters: Double,
    val elevationMeters: Double,
)

data class RouteRepresentation(
    val quality: RouteRepresentationQuality,
    val shapingPointCount: Int,
    val maximumDeviationMeters: Double,
    val maximumUnsupportedDistanceMeters: Double,
    val ignoredSegmentCount: Int,
)

enum class RouteRepresentationQuality {
    GOOD,
    LIMITED,
    COMPLEX,
}
