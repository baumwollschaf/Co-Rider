package com.ralphdietrich.corider.data

import com.ralphdietrich.corider.domain.GpxImportSource
import com.ralphdietrich.corider.domain.model.GpxRoute
import com.ralphdietrich.corider.domain.model.ThemeMode
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRouteRepositoryTest {
    @Test
    fun `eleventh import removes oldest metadata and internal file`() = runBlocking {
        val preferences = FakePreferencesStore()
        val files = FakeGpxFileStore()
        var nextId = 0
        val repository = DefaultRouteRepository(
            preferencesStore = preferences,
            fileStore = files,
            now = { nextId.toLong() },
            newId = { "route-${nextId++}" },
        )

        repeat(11) { index ->
            repository.importRoute(source("Tour $index.gpx"))
        }

        val library = repository.library.first()
        assertEquals(10, library.routes.size)
        assertEquals("route-10", library.activeRouteId)
        assertEquals((10 downTo 1).map { "route-$it" }, library.routes.map(GpxRoute::id))
        assertFalse(files.storedFiles.containsKey("route-0.gpx"))
        assertTrue(files.deletedFiles.contains("route-0.gpx"))
    }

    @Test
    fun `active route and theme survive repository recreation`() = runBlocking {
        val routes = listOf(route("first"), route("second"))
        val preferences = FakePreferencesStore(PersistedAppState(routes = routes))
        val files = FakeGpxFileStore()
        val firstRepository = DefaultRouteRepository(preferences, files)

        firstRepository.activateRoute("second")
        firstRepository.setThemeMode(ThemeMode.DARK)
        val recreatedRepository = DefaultRouteRepository(preferences, files)

        val restored = recreatedRepository.library.first()
        assertEquals("second", restored.activeRouteId)
        assertEquals("second", restored.routes.first().id)
        assertEquals(ThemeMode.DARK, restored.themeMode)
    }

    private fun source(name: String) = object : GpxImportSource {
        override val displayName = name
        override fun openStream(): InputStream = ByteArrayInputStream("<gpx/>".toByteArray())
    }

    private fun route(id: String) = GpxRoute(
        id = id,
        displayName = "$id.gpx",
        internalFileName = "$id.gpx",
        importedAtEpochMillis = 1L,
    )
}

private class FakePreferencesStore(
    initialState: PersistedAppState = PersistedAppState(),
) : AppPreferencesStore {
    private val mutableState = MutableStateFlow(initialState)
    override val state: Flow<PersistedAppState> = mutableState

    override suspend fun update(
        transform: (PersistedAppState) -> PersistedAppState,
    ): PersistedAppState {
        val updated = transform(mutableState.value)
        mutableState.value = updated
        return updated
    }
}

private class FakeGpxFileStore : GpxFileStore {
    val storedFiles = mutableMapOf<String, ByteArray>()
    val deletedFiles = mutableListOf<String>()

    override suspend fun store(routeId: String, input: InputStream): String {
        val fileName = "$routeId.gpx"
        storedFiles[fileName] = input.use(InputStream::readBytes)
        return fileName
    }

    override suspend fun delete(internalFileName: String) {
        deletedFiles += internalFileName
        storedFiles.remove(internalFileName)
    }
}
