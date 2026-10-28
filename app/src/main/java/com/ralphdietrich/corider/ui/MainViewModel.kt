package com.ralphdietrich.corider.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ralphdietrich.corider.data.ContentResolverGpxImportSource
import com.ralphdietrich.corider.domain.RouteRepository
import com.ralphdietrich.corider.domain.model.GpxRoute
import com.ralphdietrich.corider.domain.model.RouteDetails
import com.ralphdietrich.corider.domain.model.ThemeMode
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val routes: List<GpxRoute> = emptyList(),
    val activeRoute: GpxRoute? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isImporting: Boolean = false,
    val importFailure: RouteImportFailure? = null,
    val isPreparingRoute: Boolean = false,
    val mapsRouteFailed: Boolean = false,
    val routeDetails: RouteDetails? = null,
    val isLoadingRouteDetails: Boolean = false,
    val routeDetailsFailed: Boolean = false,
)

private data class MainOperationState(
    val isImporting: Boolean = false,
    val importFailure: RouteImportFailure? = null,
    val isPreparingRoute: Boolean = false,
    val mapsRouteFailed: Boolean = false,
    val detailsRouteId: String? = null,
    val routeDetails: RouteDetails? = null,
    val isLoadingRouteDetails: Boolean = false,
    val routeDetailsFailed: Boolean = false,
)

class MainViewModel(
    private val repository: RouteRepository,
    private val contentResolver: ContentResolver,
) : ViewModel() {
    private val operationState = MutableStateFlow(MainOperationState())
    private val mapsUrlChannel = Channel<String>(Channel.BUFFERED)

    val mapsUrlEvents = mapsUrlChannel.receiveAsFlow()

    val uiState = combine(
        repository.library,
        operationState,
    ) { library, operation ->
        val detailsMatchActiveRoute = operation.detailsRouteId == library.activeRouteId
        MainUiState(
            routes = library.routes,
            activeRoute = library.activeRoute,
            themeMode = library.themeMode,
            isImporting = operation.isImporting,
            importFailure = operation.importFailure,
            isPreparingRoute = operation.isPreparingRoute,
            mapsRouteFailed = operation.mapsRouteFailed,
            routeDetails = operation.routeDetails.takeIf { detailsMatchActiveRoute },
            isLoadingRouteDetails = operation.isLoadingRouteDetails && detailsMatchActiveRoute,
            routeDetailsFailed = operation.routeDetailsFailed && detailsMatchActiveRoute,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    init {
        viewModelScope.launch {
            repository.library
                .map { it.activeRouteId }
                .distinctUntilChanged()
                .collectLatest(::loadRouteDetails)
        }
    }

    fun importRoute(uri: Uri) {
        viewModelScope.launch {
            operationState.update { it.copy(isImporting = true, importFailure = null) }
            try {
                withContext(Dispatchers.IO) {
                    repository.importRoute(ContentResolverGpxImportSource(contentResolver, uri))
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                operationState.update {
                    it.copy(importFailure = error.toRouteImportFailure())
                }
            } finally {
                operationState.update { it.copy(isImporting = false) }
            }
        }
    }

    fun activateRoute(routeId: String) {
        viewModelScope.launch { repository.activateRoute(routeId) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun openActiveRouteInMaps() {
        val routeId = uiState.value.activeRoute?.id ?: return
        if (operationState.value.isPreparingRoute) return
        viewModelScope.launch {
            operationState.update { it.copy(isPreparingRoute = true, mapsRouteFailed = false) }
            try {
                val url = withContext(Dispatchers.IO) { repository.createMapsRouteUrl(routeId) }
                mapsUrlChannel.send(url)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                operationState.update { it.copy(mapsRouteFailed = true) }
            } finally {
                operationState.update { it.copy(isPreparingRoute = false) }
            }
        }
    }

    fun reportMapsLaunchFailure() {
        operationState.update { it.copy(mapsRouteFailed = true) }
    }

    fun dismissImportError() {
        operationState.update { it.copy(importFailure = null) }
    }

    fun dismissMapsRouteError() {
        operationState.update { it.copy(mapsRouteFailed = false) }
    }

    private suspend fun loadRouteDetails(routeId: String?) {
        if (routeId == null) {
            operationState.update {
                it.copy(
                    detailsRouteId = null,
                    routeDetails = null,
                    isLoadingRouteDetails = false,
                    routeDetailsFailed = false,
                )
            }
            return
        }

        operationState.update {
            it.copy(
                detailsRouteId = routeId,
                routeDetails = null,
                isLoadingRouteDetails = true,
                routeDetailsFailed = false,
            )
        }
        try {
            val details = withContext(Dispatchers.IO) { repository.loadRouteDetails(routeId) }
            operationState.update { current ->
                if (current.detailsRouteId == routeId) {
                    current.copy(routeDetails = details, isLoadingRouteDetails = false)
                } else {
                    current
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            operationState.update { current ->
                if (current.detailsRouteId == routeId) {
                    current.copy(isLoadingRouteDetails = false, routeDetailsFailed = true)
                } else {
                    current
                }
            }
        }
    }

    companion object {
        fun factory(
            repository: RouteRepository,
            contentResolver: ContentResolver,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(MainViewModel::class.java))
                return MainViewModel(repository, contentResolver) as T
            }
        }
    }
}
