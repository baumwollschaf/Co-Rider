package com.ralphdietrich.corider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
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
                CoRiderApp(
                    uiState = uiState,
                    onDocumentSelected = viewModel::importRoute,
                    onRouteSelected = viewModel::activateRoute,
                    onThemeModeSelected = viewModel::setThemeMode,
                    onImportErrorShown = viewModel::dismissImportError,
                )
            }
        }
    }
}
