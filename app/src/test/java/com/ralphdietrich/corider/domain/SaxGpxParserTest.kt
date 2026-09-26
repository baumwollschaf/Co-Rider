package com.ralphdietrich.corider.domain

import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SaxGpxParserTest {
    private val parser = SaxGpxParser()

    @Test
    fun `parses namespaced GPX track without mixing in standalone waypoints`() = runBlocking {
        val document = parser.parse(
            input(
                """
                <gpx xmlns="http://www.topografix.com/GPX/1/1" version="1.1">
                  <metadata><name>Weekend ride</name></metadata>
                  <wpt lat="50.0" lon="8.0"><name>Fuel</name></wpt>
                  <trk>
                    <name>Main track</name>
                    <trkseg>
                      <trkpt lat="52.1" lon="13.2">
                        <ele>34.5</ele>
                        <time>2026-09-26T10:15:30Z</time>
                      </trkpt>
                      <trkpt lat="53.1" lon="10.2" />
                    </trkseg>
                  </trk>
                </gpx>
                """.trimIndent(),
            ),
        )

        assertEquals("Weekend ride", document.name)
        assertEquals("Main track", document.tracks.single().name)
        assertEquals(2, document.tracks.single().segments.single().size)
        assertEquals(34.5, document.tracks.single().segments.single().first().elevationMeters)
        assertEquals(1_790_417_730_000L, document.tracks.single().segments.single().first().timestampEpochMillis)
        assertEquals(1, document.waypoints.size)
    }

    @Test
    fun `parses GPX route points as a usable ordered segment`() = runBlocking {
        val document = parser.parse(
            input(
                """
                <gpx version="1.1">
                  <rte><name>Planned route</name>
                    <rtept lat="1" lon="2" />
                    <rtept lat="3" lon="4" />
                  </rte>
                </gpx>
                """.trimIndent(),
            ),
        )

        assertEquals("Planned route", document.tracks.single().name)
        assertEquals(2, document.tracks.single().segments.single().size)
    }

    @Test
    fun `rejects a document that is not GPX`() {
        assertThrows(Exception::class.java) {
            runBlocking { parser.parse(input("<route />")) }
        }
    }

    private fun input(xml: String) = ByteArrayInputStream(xml.toByteArray())
}
