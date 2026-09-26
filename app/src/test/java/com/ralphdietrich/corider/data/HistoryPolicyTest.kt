package com.ralphdietrich.corider.data

import com.ralphdietrich.corider.domain.model.GpxRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryPolicyTest {
    @Test
    fun `limit keeps ten newest routes and returns all older routes`() {
        val routes = (0 until 12).map(::route)

        val result = HistoryPolicy.limit(routes)

        assertEquals((0 until 10).map { "route-$it" }, result.kept.map(GpxRoute::id))
        assertEquals(listOf("route-10", "route-11"), result.removed.map(GpxRoute::id))
    }

    private fun route(index: Int) = GpxRoute(
        id = "route-$index",
        displayName = "Route $index.gpx",
        internalFileName = "route-$index.gpx",
        importedAtEpochMillis = index.toLong(),
    )
}
