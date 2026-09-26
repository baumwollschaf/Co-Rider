package com.ralphdietrich.corider.data

import com.ralphdietrich.corider.domain.model.GpxRoute
import com.ralphdietrich.corider.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

data class PersistedAppState(
    val routes: List<GpxRoute> = emptyList(),
    val activeRouteId: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

interface AppPreferencesStore {
    val state: Flow<PersistedAppState>

    suspend fun update(
        transform: (PersistedAppState) -> PersistedAppState,
    ): PersistedAppState
}
