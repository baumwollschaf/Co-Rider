package com.ralphdietrich.corider.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ralphdietrich.corider.domain.model.GpxRoute
import com.ralphdietrich.corider.domain.model.ThemeMode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreAppPreferencesStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `state is written to and read from preferences datastore`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataFile = File(temporaryFolder.root, "preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { dataFile },
        )
        val store = DataStoreAppPreferencesStore(dataStore)
        val expectedRoute = GpxRoute(
            id = "route-id",
            displayName = "Alpenrunde.gpx",
            internalFileName = "route-id.gpx",
            importedAtEpochMillis = 1234L,
        )

        store.update {
            PersistedAppState(
                routes = listOf(expectedRoute),
                activeRouteId = expectedRoute.id,
                themeMode = ThemeMode.DARK,
            )
        }

        assertEquals(
            PersistedAppState(
                routes = listOf(expectedRoute),
                activeRouteId = expectedRoute.id,
                themeMode = ThemeMode.DARK,
            ),
            store.state.first(),
        )
        scope.cancel()
    }
}
