package com.ralphdietrich.corider.data

import com.ralphdietrich.corider.domain.model.GpxRoute

data class HistoryLimitResult(
    val kept: List<GpxRoute>,
    val removed: List<GpxRoute>,
)

object HistoryPolicy {
    const val MAX_ROUTES = 10

    fun limit(
        routes: List<GpxRoute>,
        maximum: Int = MAX_ROUTES,
    ): HistoryLimitResult {
        require(maximum >= 0) { "maximum must not be negative" }
        return HistoryLimitResult(
            kept = routes.take(maximum),
            removed = routes.drop(maximum),
        )
    }
}
