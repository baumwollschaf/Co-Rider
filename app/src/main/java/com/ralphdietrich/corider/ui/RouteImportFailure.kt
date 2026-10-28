package com.ralphdietrich.corider.ui

import com.ralphdietrich.corider.domain.GpxParsingException
import com.ralphdietrich.corider.domain.RouteConversionException
import org.xml.sax.SAXException

enum class RouteImportFailure {
    INVALID_GPX,
    NO_USABLE_TRACK,
    FILE_UNAVAILABLE,
}

internal fun Throwable.toRouteImportFailure(): RouteImportFailure = when (this) {
    is RouteConversionException -> RouteImportFailure.NO_USABLE_TRACK
    is GpxParsingException, is SAXException -> RouteImportFailure.INVALID_GPX
    else -> RouteImportFailure.FILE_UNAVAILABLE
}
