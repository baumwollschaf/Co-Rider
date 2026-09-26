package com.ralphdietrich.corider.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ralphdietrich.corider.R
import com.ralphdietrich.corider.domain.model.GpxRoute
import com.ralphdietrich.corider.domain.model.ThemeMode
import kotlinx.coroutines.launch

private val GpxMimeTypes = arrayOf(
    "application/gpx+xml",
    "application/xml",
    "text/xml",
    "application/octet-stream",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoRiderApp(
    uiState: MainUiState,
    onDocumentSelected: (Uri) -> Unit,
    onRouteSelected: (String) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onImportErrorShown: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val importErrorText = stringResource(R.string.import_failed)
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onDocumentSelected) }
    val openDocument = {
        coroutineScope.launch { drawerState.close() }
        documentLauncher.launch(GpxMimeTypes)
    }

    LaunchedEffect(uiState.importFailed) {
        if (uiState.importFailed) {
            snackbarHostState.showSnackbar(importErrorText)
            onImportErrorShown()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                routes = uiState.routes,
                activeRouteId = uiState.activeRoute?.id,
                themeMode = uiState.themeMode,
                onOpenDocument = openDocument,
                onRouteSelected = { routeId ->
                    onRouteSelected(routeId)
                    coroutineScope.launch { drawerState.close() }
                },
                onThemeModeSelected = onThemeModeSelected,
            )
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.menu_open),
                            )
                        }
                    },
                )
            },
        ) { contentPadding ->
            MainContent(
                activeRoute = uiState.activeRoute,
                isImporting = uiState.isImporting,
                onOpenDocument = openDocument,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@Composable
private fun AppDrawer(
    routes: List<GpxRoute>,
    activeRouteId: String?,
    themeMode: ThemeMode,
    onOpenDocument: () -> Unit,
    onRouteSelected: (String) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
) {
    ModalDrawerSheet(modifier = Modifier.widthIn(max = 360.dp)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.open_gpx)) },
                    selected = false,
                    onClick = onOpenDocument,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                DrawerSectionTitle(stringResource(R.string.recently_used))
            }
            if (routes.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_recent_routes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                    )
                }
            } else {
                items(routes, key = GpxRoute::id) { route ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = route.displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        selected = route.id == activeRouteId,
                        onClick = { onRouteSelected(route.id) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                DrawerSectionTitle(stringResource(R.string.appearance))
                ThemeMode.entries.forEach { mode ->
                    ThemeModeItem(
                        mode = mode,
                        selected = mode == themeMode,
                        onClick = { onThemeModeSelected(mode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
    )
}

@Composable
private fun ThemeModeItem(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = when (mode) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }
    NavigationDrawerItem(
        label = { Text(label) },
        icon = {
            RadioButton(
                selected = selected,
                onClick = null,
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun MainContent(
    activeRoute: GpxRoute?,
    isImporting: Boolean,
    onOpenDocument: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (activeRoute == null) {
            EmptyState(isImporting, onOpenDocument)
        } else {
            ActiveRouteState(activeRoute, isImporting, onOpenDocument)
        }
    }
}

@Composable
private fun EmptyState(
    isImporting: Boolean,
    onOpenDocument: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.widthIn(max = 520.dp),
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "GPX",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.empty_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        ImportButton(isImporting, onOpenDocument, R.string.choose_gpx)
    }
}

@Composable
private fun ActiveRouteState(
    route: GpxRoute,
    isImporting: Boolean,
    onOpenDocument: () -> Unit,
) {
    Column(
        modifier = Modifier.widthIn(max = 560.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.active_route),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = route.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.stored_locally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        ImportButton(isImporting, onOpenDocument, R.string.choose_another_gpx)
    }
}

@Composable
private fun ImportButton(
    isImporting: Boolean,
    onClick: () -> Unit,
    labelResource: Int,
) {
    Button(
        onClick = onClick,
        enabled = !isImporting,
    ) {
        if (isImporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.size(10.dp))
        }
        Text(stringResource(labelResource))
    }
}
