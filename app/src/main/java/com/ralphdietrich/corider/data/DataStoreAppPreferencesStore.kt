package com.ralphdietrich.corider.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ralphdietrich.corider.domain.model.GpxRoute
import com.ralphdietrich.corider.domain.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreAppPreferencesStore(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesStore {
    override val state: Flow<PersistedAppState> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::readState)

    override suspend fun update(
        transform: (PersistedAppState) -> PersistedAppState,
    ): PersistedAppState {
        var updatedState: PersistedAppState? = null
        dataStore.edit { preferences ->
            val updated = transform(readState(preferences))
            writeState(preferences, updated)
            updatedState = updated
        }
        return updatedState ?: state.first()
    }

    private fun readState(preferences: Preferences): PersistedAppState {
        val routes = buildList {
            repeat(MAX_HISTORY_SIZE) { index ->
                val id = preferences[routeIdKey(index)] ?: return@repeat
                val displayName = preferences[routeDisplayNameKey(index)] ?: return@repeat
                val internalFileName = preferences[routeFileNameKey(index)] ?: return@repeat
                val importedAt = preferences[routeImportedAtKey(index)] ?: return@repeat
                add(
                    GpxRoute(
                        id = id,
                        displayName = displayName,
                        internalFileName = internalFileName,
                        importedAtEpochMillis = importedAt,
                    ),
                )
            }
        }
        val themeMode = preferences[THEME_MODE]
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM
        return PersistedAppState(
            routes = routes,
            activeRouteId = preferences[ACTIVE_ROUTE_ID],
            themeMode = themeMode,
        )
    }

    private fun writeState(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        state: PersistedAppState,
    ) {
        repeat(MAX_HISTORY_SIZE) { index ->
            preferences.remove(routeIdKey(index))
            preferences.remove(routeDisplayNameKey(index))
            preferences.remove(routeFileNameKey(index))
            preferences.remove(routeImportedAtKey(index))
        }
        state.routes.take(MAX_HISTORY_SIZE).forEachIndexed { index, route ->
            preferences[routeIdKey(index)] = route.id
            preferences[routeDisplayNameKey(index)] = route.displayName
            preferences[routeFileNameKey(index)] = route.internalFileName
            preferences[routeImportedAtKey(index)] = route.importedAtEpochMillis
        }
        state.activeRouteId?.let { preferences[ACTIVE_ROUTE_ID] = it }
            ?: preferences.remove(ACTIVE_ROUTE_ID)
        preferences[THEME_MODE] = state.themeMode.name
    }

    private companion object {
        const val MAX_HISTORY_SIZE = 10
        val ACTIVE_ROUTE_ID = stringPreferencesKey("active_route_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")

        fun routeIdKey(index: Int) = stringPreferencesKey("route_${index}_id")
        fun routeDisplayNameKey(index: Int) = stringPreferencesKey("route_${index}_display_name")
        fun routeFileNameKey(index: Int) = stringPreferencesKey("route_${index}_file_name")
        fun routeImportedAtKey(index: Int) = longPreferencesKey("route_${index}_imported_at")
    }
}
