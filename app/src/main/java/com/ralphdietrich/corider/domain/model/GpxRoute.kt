package com.ralphdietrich.corider.domain.model

data class GpxRoute(
    val id: String,
    val displayName: String,
    val internalFileName: String,
    val importedAtEpochMillis: Long,
)

data class RouteLibrary(
    val routes: List<GpxRoute> = emptyList(),
    val activeRouteId: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    val activeRoute: GpxRoute?
        get() = routes.firstOrNull { it.id == activeRouteId }
}
