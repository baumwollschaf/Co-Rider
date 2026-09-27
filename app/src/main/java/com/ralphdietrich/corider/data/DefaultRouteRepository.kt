package com.ralphdietrich.corider.data

import com.ralphdietrich.corider.domain.GpxImportSource
import com.ralphdietrich.corider.domain.GpxParser
import com.ralphdietrich.corider.domain.MapsRouteUrlFactory
import com.ralphdietrich.corider.domain.RouteRepository
import com.ralphdietrich.corider.domain.RouteDetailsAnalyzer
import com.ralphdietrich.corider.domain.DefaultMapsRouteUrlFactory
import com.ralphdietrich.corider.domain.SaxGpxParser
import com.ralphdietrich.corider.domain.TrackGeometry
import com.ralphdietrich.corider.domain.model.GpxRoute
import com.ralphdietrich.corider.domain.model.RouteLibrary
import com.ralphdietrich.corider.domain.model.RouteDetails
import com.ralphdietrich.corider.domain.model.ThemeMode
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultRouteRepository(
    private val preferencesStore: AppPreferencesStore,
    private val fileStore: GpxFileStore,
    private val gpxParser: GpxParser = SaxGpxParser(),
    private val mapsRouteUrlFactory: MapsRouteUrlFactory = DefaultMapsRouteUrlFactory(),
    private val routeDetailsAnalyzer: RouteDetailsAnalyzer = RouteDetailsAnalyzer(),
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : RouteRepository {
    private val writeMutex = Mutex()

    override val library: Flow<RouteLibrary> = preferencesStore.state.map { state ->
        RouteLibrary(
            routes = state.routes,
            activeRouteId = state.activeRouteId,
            themeMode = state.themeMode,
        )
    }

    override suspend fun importRoute(source: GpxImportSource) = writeMutex.withLock {
        val routeId = newId()
        var newInternalFile: String? = null
        try {
            val internalFileName = fileStore.store(routeId, source.openStream())
            newInternalFile = internalFileName
            val importedDocument = fileStore.open(internalFileName).use { input ->
                gpxParser.parse(input)
            }
            TrackGeometry.primarySegment(importedDocument)
            val newRoute = GpxRoute(
                id = routeId,
                displayName = source.displayName.ifBlank { "Route.gpx" },
                internalFileName = internalFileName,
                importedAtEpochMillis = now(),
            )
            var removedRoutes = emptyList<GpxRoute>()
            preferencesStore.update { current ->
                val history = HistoryPolicy.limit(
                    listOf(newRoute) + current.routes.filterNot { it.id == routeId },
                )
                removedRoutes = history.removed
                current.copy(
                    routes = history.kept,
                    activeRouteId = routeId,
                )
            }
            removedRoutes.forEach { removed ->
                runCatching { fileStore.delete(removed.internalFileName) }
            }
        } catch (error: Throwable) {
            newInternalFile?.let { fileName ->
                runCatching { fileStore.delete(fileName) }
            }
            throw error
        }
    }

    override suspend fun activateRoute(routeId: String) = writeMutex.withLock {
        preferencesStore.update { current ->
            val selected = current.routes.firstOrNull { it.id == routeId }
            if (selected == null) {
                current
            } else {
                current.copy(
                    routes = listOf(selected) + current.routes.filterNot { it.id == routeId },
                    activeRouteId = routeId,
                )
            }
        }
        Unit
    }

    override suspend fun createMapsRouteUrl(routeId: String): String = writeMutex.withLock {
        mapsRouteUrlFactory.createUrl(parseRoute(routeId))
    }

    override suspend fun loadRouteDetails(routeId: String): RouteDetails = writeMutex.withLock {
        routeDetailsAnalyzer.analyze(parseRoute(routeId))
    }

    override suspend fun setThemeMode(mode: ThemeMode) = writeMutex.withLock {
        preferencesStore.update { current -> current.copy(themeMode = mode) }
        Unit
    }

    private suspend fun parseRoute(routeId: String) =
        preferencesStore.state.first().routes.firstOrNull { it.id == routeId }
            ?.let { route ->
                fileStore.open(route.internalFileName).use { input -> gpxParser.parse(input) }
            }
            ?: error("The selected route no longer exists")
}
