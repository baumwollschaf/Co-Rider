package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.RouteLibrary
import com.ralphdietrich.corider.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    val library: Flow<RouteLibrary>

    suspend fun importRoute(source: GpxImportSource)

    suspend fun activateRoute(routeId: String)

    suspend fun setThemeMode(mode: ThemeMode)
}
