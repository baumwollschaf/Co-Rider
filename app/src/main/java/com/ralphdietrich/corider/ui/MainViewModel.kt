package com.ralphdietrich.corider.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ralphdietrich.corider.data.ContentResolverGpxImportSource
import com.ralphdietrich.corider.domain.RouteRepository
import com.ralphdietrich.corider.domain.model.GpxRoute
import com.ralphdietrich.corider.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val routes: List<GpxRoute> = emptyList(),
    val activeRoute: GpxRoute? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isImporting: Boolean = false,
    val importFailed: Boolean = false,
)

class MainViewModel(
    private val repository: RouteRepository,
    private val contentResolver: ContentResolver,
) : ViewModel() {
    private val isImporting = MutableStateFlow(false)
    private val importFailed = MutableStateFlow(false)

    val uiState = combine(
        repository.library,
        isImporting,
        importFailed,
    ) { library, importing, failed ->
        MainUiState(
            routes = library.routes,
            activeRoute = library.activeRoute,
            themeMode = library.themeMode,
            isImporting = importing,
            importFailed = failed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    fun importRoute(uri: Uri) {
        viewModelScope.launch {
            isImporting.value = true
            importFailed.value = false
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.importRoute(ContentResolverGpxImportSource(contentResolver, uri))
                }
            }.onFailure {
                importFailed.value = true
            }
            isImporting.value = false
        }
    }

    fun activateRoute(routeId: String) {
        viewModelScope.launch { repository.activateRoute(routeId) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun dismissImportError() {
        importFailed.value = false
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
