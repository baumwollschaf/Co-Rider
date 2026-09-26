package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.GpxDocument
import com.ralphdietrich.corider.domain.model.GpxPoint
import com.ralphdietrich.corider.domain.model.GpxTrack
import java.io.InputStream
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

class SaxGpxParser : GpxParser {
    override suspend fun parse(input: InputStream): GpxDocument {
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            isValidating = false
        }
        val reader = factory.newSAXParser().xmlReader
        SECURITY_FEATURES.forEach { (feature, enabled) ->
            runCatching { reader.setFeature(feature, enabled) }
        }
        reader.entityResolver = EntityResolver { _, _ -> InputSource(StringReader("")) }

        val handler = GpxHandler()
        reader.contentHandler = handler
        input.use { reader.parse(InputSource(it)) }
        return handler.result()
    }

    private class GpxHandler : DefaultHandler() {
        private val elementStack = ArrayDeque<String>()
        private val text = StringBuilder()
        private val tracks = mutableListOf<GpxTrack>()
        private val waypoints = mutableListOf<GpxPoint>()
        private var documentName: String? = null
        private var seenRoot = false
        private var pointCount = 0

        private var trackName: String? = null
        private var trackSegments: MutableList<List<GpxPoint>>? = null
        private var trackSegment: MutableList<GpxPoint>? = null
        private var routePoints: MutableList<GpxPoint>? = null
        private var point: MutablePoint? = null
        private var pointElement: String? = null
        private val timestampFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
        ).map { pattern ->
            SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
        }

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String?,
            attributes: Attributes,
        ) {
            val name = localName.orEmpty().ifEmpty { qName.orEmpty().substringAfter(':') }
            if (elementStack.isEmpty()) {
                if (name != "gpx") throw GpxParsingException("The document is not a GPX file")
                seenRoot = true
            }
            elementStack.addLast(name)
            text.setLength(0)

            when (name) {
                "trk" -> {
                    trackName = null
                    trackSegments = mutableListOf()
                }
                "trkseg" -> trackSegment = mutableListOf()
                "rte" -> {
                    trackName = null
                    routePoints = mutableListOf()
                }
                "trkpt", "rtept", "wpt" -> {
                    val latitude = attributes.getValue("lat")?.toDoubleOrNull()
                    val longitude = attributes.getValue("lon")?.toDoubleOrNull()
                    if (latitude == null || longitude == null ||
                        !latitude.isFinite() || !longitude.isFinite() ||
                        latitude !in -90.0..90.0 || longitude !in -180.0..180.0
                    ) {
                        throw GpxParsingException("A GPX point has invalid coordinates")
                    }
                    pointCount += 1
                    if (pointCount > MAX_POINTS) {
                        throw GpxParsingException("The GPX file contains too many points")
                    }
                    point = MutablePoint(latitude, longitude)
                    pointElement = name
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            text.append(ch, start, length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            val name = localName.orEmpty().ifEmpty { qName.orEmpty().substringAfter(':') }
            val parent = elementStack.elementAtOrNull(elementStack.size - 2)
            val value = text.toString().trim()

            when (name) {
                "name" -> when (parent) {
                    "metadata" -> documentName = value.ifEmpty { null }
                    "trk", "rte" -> trackName = value.ifEmpty { null }
                }
                "ele" -> point?.elevationMeters = value.toDoubleOrNull()
                "time" -> point?.timestampEpochMillis = parseTimestamp(value)
                "trkpt" -> finishPoint("trkpt") { trackSegment?.add(it) }
                "rtept" -> finishPoint("rtept") { routePoints?.add(it) }
                "wpt" -> finishPoint("wpt", waypoints::add)
                "trkseg" -> {
                    trackSegment?.takeIf(List<GpxPoint>::isNotEmpty)?.let { segment ->
                        trackSegments?.add(segment.toList())
                    }
                    trackSegment = null
                }
                "trk" -> {
                    tracks += GpxTrack(trackName, trackSegments.orEmpty().toList())
                    trackName = null
                    trackSegments = null
                }
                "rte" -> {
                    routePoints?.takeIf(List<GpxPoint>::isNotEmpty)?.let { points ->
                        tracks += GpxTrack(trackName, listOf(points.toList()))
                    }
                    trackName = null
                    routePoints = null
                }
            }

            if (elementStack.isNotEmpty()) elementStack.removeLast()
            text.setLength(0)
        }

        fun result(): GpxDocument {
            if (!seenRoot) throw GpxParsingException("The GPX file is empty")
            return GpxDocument(
                name = documentName,
                tracks = tracks.toList(),
                waypoints = waypoints.toList(),
            )
        }

        private fun finishPoint(expectedElement: String, add: (GpxPoint) -> Unit) {
            if (pointElement == expectedElement) point?.toPoint()?.let(add)
            point = null
            pointElement = null
        }

        private fun parseTimestamp(value: String): Long? {
            val normalized = normalizeFraction(value)
            return timestampFormats.firstNotNullOfOrNull { format ->
                runCatching { format.parse(normalized)?.time }.getOrNull()
            }
        }

        private fun normalizeFraction(value: String): String {
            val fractionStart = value.indexOf('.')
            if (fractionStart < 0) return value
            val zoneStart = value.indexOfFirstFrom(fractionStart + 1) { character ->
                character == 'Z' || character == '+' || character == '-'
            }
            if (zoneStart < 0) return value
            val milliseconds = value.substring(fractionStart + 1, zoneStart)
                .take(3)
                .padEnd(3, '0')
            return value.substring(0, fractionStart + 1) + milliseconds + value.substring(zoneStart)
        }

        private inline fun String.indexOfFirstFrom(
            startIndex: Int,
            predicate: (Char) -> Boolean,
        ): Int {
            for (index in startIndex until length) if (predicate(this[index])) return index
            return -1
        }
    }

    private data class MutablePoint(
        val latitude: Double,
        val longitude: Double,
        var elevationMeters: Double? = null,
        var timestampEpochMillis: Long? = null,
    ) {
        fun toPoint() = GpxPoint(
            latitude = latitude,
            longitude = longitude,
            elevationMeters = elevationMeters,
            timestampEpochMillis = timestampEpochMillis,
        )
    }

    private companion object {
        const val MAX_POINTS = 200_000
        val SECURITY_FEATURES = mapOf(
            "http://apache.org/xml/features/disallow-doctype-decl" to true,
            "http://xml.org/sax/features/external-general-entities" to false,
            "http://xml.org/sax/features/external-parameter-entities" to false,
            "http://apache.org/xml/features/nonvalidating/load-external-dtd" to false,
        )
    }
}

class GpxParsingException(message: String) : IllegalArgumentException(message)
