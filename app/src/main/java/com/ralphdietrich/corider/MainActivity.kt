package com.ralphdietrich.corider

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ralphdietrich.corider.domain.model.ThemeMode
import com.ralphdietrich.corider.ui.CoRiderApp
import com.ralphdietrich.corider.ui.MainViewModel
import com.ralphdietrich.corider.ui.theme.CoRiderTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        val app = application as CoRiderApplication
        MainViewModel.factory(app.routeRepository, contentResolver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            val useDarkTheme = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            CoRiderTheme(darkTheme = useDarkTheme) {
                LaunchedEffect(Unit) {
                    viewModel.mapsUrlEvents.collect { url ->
                        if (!openMapsRoute(url)) viewModel.reportMapsLaunchFailure()
                    }
                }
                CoRiderApp(
                    uiState = uiState,
                    onDocumentSelected = viewModel::importRoute,
                    onRouteSelected = viewModel::activateRoute,
                    onThemeModeSelected = viewModel::setThemeMode,
                    onOpenInMaps = viewModel::openActiveRouteInMaps,
                    onImportErrorShown = viewModel::dismissImportError,
                    onMapsRouteErrorShown = viewModel::dismissMapsRouteError,
                )
            }
        }
    }

    private fun openMapsRoute(url: String): Boolean {
        val uri = url.toUri()
        val mapsIntent = Intent(Intent.ACTION_VIEW, uri).setPackage(GOOGLE_MAPS_PACKAGE)
        return try {
            startActivity(mapsIntent)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }

    private companion object {
        const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    }
}
