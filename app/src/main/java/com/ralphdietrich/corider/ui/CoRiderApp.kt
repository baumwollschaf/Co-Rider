package com.ralphdietrich.corider.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ralphdietrich.corider.R
import com.ralphdietrich.corider.domain.model.ElevationSample
import com.ralphdietrich.corider.domain.model.GpxRoute
import com.ralphdietrich.corider.domain.model.RouteDetails
import com.ralphdietrich.corider.domain.model.RouteRepresentationQuality
import com.ralphdietrich.corider.domain.model.ThemeMode
import kotlinx.coroutines.launch

private val GpxMimeTypes = arrayOf(
    "application/gpx+xml",
    "application/xml",
    "text/xml",
    "application/octet-stream",
)
private val FileNameSeparators = Regex("[-_]+")
private val RepeatedWhitespace = Regex("\\s+")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoRiderApp(
    uiState: MainUiState,
    onDocumentSelected: (Uri) -> Unit,
    onRouteSelected: (String) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onOpenInMaps: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenCommunityGroup: () -> Unit,
    onImportErrorShown: () -> Unit,
    onMapsRouteErrorShown: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val importErrorText = uiState.importFailure?.let { failure ->
        stringResource(
            when (failure) {
                RouteImportFailure.INVALID_GPX -> R.string.import_invalid_gpx
                RouteImportFailure.NO_USABLE_TRACK -> R.string.import_no_usable_track
                RouteImportFailure.FILE_UNAVAILABLE -> R.string.import_file_unavailable
            },
        )
    }
    val mapsRouteErrorText = stringResource(R.string.maps_route_failed)
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onDocumentSelected) }
    val openDocument = {
        coroutineScope.launch { drawerState.close() }
        documentLauncher.launch(GpxMimeTypes)
    }

    LaunchedEffect(uiState.importFailure) {
        if (importErrorText != null) {
            snackbarHostState.showSnackbar(importErrorText)
            onImportErrorShown()
        }
    }

    LaunchedEffect(uiState.mapsRouteFailed) {
        if (uiState.mapsRouteFailed) {
            snackbarHostState.showSnackbar(mapsRouteErrorText)
            onMapsRouteErrorShown()
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
                onOpenPrivacyPolicy = {
                    coroutineScope.launch { drawerState.close() }
                    onOpenPrivacyPolicy()
                },
                onOpenCommunityGroup = {
                    coroutineScope.launch { drawerState.close() }
                    onOpenCommunityGroup()
                },
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
            bottomBar = {
                if (uiState.activeRoute != null) {
                    RouteActionBar(
                        isImporting = uiState.isImporting,
                        isPreparingRoute = uiState.isPreparingRoute,
                        canOpenInMaps = uiState.routeDetails != null,
                        onOpenDocument = openDocument,
                        onOpenInMaps = onOpenInMaps,
                    )
                }
            },
        ) { contentPadding ->
            MainContent(
                uiState = uiState,
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
    onOpenPrivacyPolicy: () -> Unit,
    onOpenCommunityGroup: () -> Unit,
) {
    ModalDrawerSheet(modifier = Modifier.widthIn(max = 360.dp)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.privacy_policy)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                        )
                    },
                    selected = false,
                    onClick = onOpenPrivacyPolicy,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                )
                NavigationDrawerItem(
                    label = {
                        Column {
                            Text(
                                text = stringResource(R.string.community_group_name),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.community_group_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    selected = false,
                    onClick = onOpenCommunityGroup,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
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
        icon = { RadioButton(selected = selected, onClick = null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun MainContent(
    uiState: MainUiState,
    onOpenDocument: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.activeRoute == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(uiState.isImporting, onOpenDocument)
        }
    } else {
        RouteDetailsContent(
            route = uiState.activeRoute,
            details = uiState.routeDetails,
            isLoading = uiState.isLoadingRouteDetails,
            loadingFailed = uiState.routeDetailsFailed,
            modifier = modifier,
        )
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
        Image(
            painter = painterResource(R.drawable.co_rider_logo),
            contentDescription = null,
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(28.dp)),
            contentScale = ContentScale.Crop,
        )
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
        Button(onClick = onOpenDocument, enabled = !isImporting) {
            if (isImporting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(10.dp))
            }
            Text(stringResource(R.string.choose_gpx))
        }
    }
}

@Composable
private fun RouteDetailsContent(
    route: GpxRoute,
    details: RouteDetails?,
    isLoading: Boolean,
    loadingFailed: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 760.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(if (wideLayout) 24.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { RouteHeader(route, Modifier.widthIn(max = 960.dp).fillMaxWidth()) }
            if (isLoading) {
                item { LoadingDetailsCard(Modifier.widthIn(max = 960.dp).fillMaxWidth()) }
            }
            if (loadingFailed) {
                item { DetailsErrorCard(Modifier.widthIn(max = 960.dp).fillMaxWidth()) }
            }
            details?.let { routeDetails ->
                item {
                    RouteRepresentationCard(
                        details = routeDetails,
                        modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth(),
                    )
                }
                if (wideLayout) {
                    item {
                        Row(
                            modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            RouteStatistics(routeDetails, Modifier.weight(1f))
                            ElevationProfileCard(routeDetails, Modifier.weight(1f))
                        }
                    }
                } else {
                    item { RouteStatistics(routeDetails, Modifier.widthIn(max = 720.dp).fillMaxWidth()) }
                    item { ElevationProfileCard(routeDetails, Modifier.widthIn(max = 720.dp).fillMaxWidth()) }
                }
            }
        }
    }
}

@Composable
private fun RouteHeader(route: GpxRoute, modifier: Modifier = Modifier) {
    val title = remember(route.displayName) { route.displayName.toFriendlyRouteTitle() }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.active_route),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = route.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.stored_locally),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteStatistics(details: RouteDetails, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.route_summary),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            Statistic(
                label = stringResource(R.string.distance),
                value = stringResource(R.string.distance_km, details.totalDistanceMeters / 1_000.0),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Statistic(
                    label = stringResource(R.string.lowest_point),
                    value = details.minimumElevationMeters.elevationText(),
                    modifier = Modifier.weight(1f),
                )
                Statistic(
                    label = stringResource(R.string.highest_point),
                    value = details.maximumElevationMeters.elevationText(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RouteRepresentationCard(details: RouteDetails, modifier: Modifier = Modifier) {
    val representation = details.representation
    val (title, description) = when (representation.quality) {
        RouteRepresentationQuality.GOOD ->
            stringResource(R.string.representation_good) to
                stringResource(R.string.representation_good_description)
        RouteRepresentationQuality.LIMITED ->
            stringResource(R.string.representation_limited) to
                stringResource(R.string.representation_limited_description)
        RouteRepresentationQuality.COMPLEX ->
            stringResource(R.string.representation_complex) to
                stringResource(R.string.representation_complex_description)
    }
    val containerColor = when (representation.quality) {
        RouteRepresentationQuality.GOOD -> MaterialTheme.colorScheme.primaryContainer
        RouteRepresentationQuality.LIMITED -> MaterialTheme.colorScheme.secondaryContainer
        RouteRepresentationQuality.COMPLEX -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (representation.quality) {
        RouteRepresentationQuality.GOOD -> MaterialTheme.colorScheme.onPrimaryContainer
        RouteRepresentationQuality.LIMITED -> MaterialTheme.colorScheme.onSecondaryContainer
        RouteRepresentationQuality.COMPLEX -> MaterialTheme.colorScheme.onErrorContainer
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.google_maps_final_route),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Statistic(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ElevationProfileCard(details: RouteDetails, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.original_gpx_elevation),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.original_gpx_elevation_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (details.elevationProfile.size < 2) {
                Text(
                    text = stringResource(R.string.no_elevation_data),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ElevationChart(
                    samples = details.elevationProfile,
                    totalDistanceMeters = details.totalDistanceMeters,
                )
            }
        }
    }
}

@Composable
private fun ElevationChart(
    samples: List<ElevationSample>,
    totalDistanceMeters: Double,
) {
    val minimum = samples.minOf(ElevationSample::elevationMeters)
    val maximum = samples.maxOf(ElevationSample::elevationMeters)
    val elevationRange = (maximum - minimum).coerceAtLeast(1.0)
    val distanceRange = totalDistanceMeters.coerceAtLeast(1.0)
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val chartDescription = stringResource(
        R.string.elevation_chart_description,
        minimum,
        maximum,
        totalDistanceMeters / 1_000.0,
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { contentDescription = chartDescription },
    ) {
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
        val path = Path()
        samples.forEachIndexed { index, sample ->
            val x = (sample.distanceMeters / distanceRange * size.width).toFloat()
            val y = (size.height - (sample.elevationMeters - minimum) / elevationRange * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = stringResource(R.string.distance_km, 0.0),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.distance_km, totalDistanceMeters / 1_000.0),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingDetailsCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.loading_route_details))
        }
    }
}

@Composable
private fun DetailsErrorCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            text = stringResource(R.string.route_details_failed),
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun RouteActionBar(
    isImporting: Boolean,
    isPreparingRoute: Boolean,
    canOpenInMaps: Boolean,
    onOpenDocument: () -> Unit,
    onOpenInMaps: () -> Unit,
) {
    Surface(shadowElevation = 8.dp, tonalElevation = 2.dp) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
        ) {
            if (maxWidth >= 620.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ChooseFileButton(
                        isImporting = isImporting,
                        enabled = !isImporting && !isPreparingRoute,
                        onClick = onOpenDocument,
                        modifier = Modifier.weight(1f),
                    )
                    OpenMapsButton(
                        isPreparingRoute = isPreparingRoute,
                        enabled = canOpenInMaps && !isImporting && !isPreparingRoute,
                        onClick = onOpenInMaps,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChooseFileButton(
                        isImporting = isImporting,
                        enabled = !isImporting && !isPreparingRoute,
                        onClick = onOpenDocument,
                    )
                    OpenMapsButton(
                        isPreparingRoute = isPreparingRoute,
                        enabled = canOpenInMaps && !isImporting && !isPreparingRoute,
                        onClick = onOpenInMaps,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChooseFileButton(
    isImporting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (isImporting) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
        }
        Spacer(Modifier.size(10.dp))
        Text(stringResource(R.string.choose_another_gpx))
    }
}

@Composable
private fun OpenMapsButton(
    isPreparingRoute: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (isPreparingRoute) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(imageVector = Icons.Default.Place, contentDescription = null)
        }
        Spacer(Modifier.size(10.dp))
        Text(stringResource(R.string.open_in_google_maps))
    }
}

@Composable
private fun Double?.elevationText(): String =
    this?.let { stringResource(R.string.elevation_meters, it) }
        ?: stringResource(R.string.not_available)

private fun String.toFriendlyRouteTitle(): String {
    val withoutExtension = if (endsWith(".gpx", ignoreCase = true)) dropLast(4) else this
    return withoutExtension
        .replace(FileNameSeparators, " ")
        .replace(RepeatedWhitespace, " ")
        .trim()
        .ifEmpty { this }
}
