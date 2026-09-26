package com.ralphdietrich.corider.domain.model

/** Domain models reserved for the later GPX parsing and route-conversion steps. */
data class GpxDocument(
    val name: String?,
    val tracks: List<GpxTrack>,
    val waypoints: List<GpxPoint> = emptyList(),
)

data class GpxTrack(
    val name: String?,
    val segments: List<List<GpxPoint>>,
)

data class GpxPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double? = null,
    val timestampEpochMillis: Long? = null,
)
