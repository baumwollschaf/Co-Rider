package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.RouteLibrary
import com.ralphdietrich.corider.domain.model.RouteDetails
import com.ralphdietrich.corider.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    val library: Flow<RouteLibrary>

    suspend fun importRoute(source: GpxImportSource)

    suspend fun activateRoute(routeId: String)

    suspend fun createMapsRouteUrl(routeId: String): String

    suspend fun loadRouteDetails(routeId: String): RouteDetails

    suspend fun setThemeMode(mode: ThemeMode)
}
