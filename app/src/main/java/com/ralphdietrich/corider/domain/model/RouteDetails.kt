package com.ralphdietrich.corider.domain.model

data class RouteDetails(
    val totalDistanceMeters: Double,
    val trackPointCount: Int,
    val minimumElevationMeters: Double?,
    val maximumElevationMeters: Double?,
    val elevationProfile: List<ElevationSample>,
    val positions: List<RoutePosition>,
)

data class ElevationSample(
    val distanceMeters: Double,
    val elevationMeters: Double,
)

data class RoutePosition(
    val kind: RoutePositionKind,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
)

enum class RoutePositionKind {
    START,
    MANEUVER,
    STOP,
    DESTINATION,
}
