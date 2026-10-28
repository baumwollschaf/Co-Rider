package com.ralphdietrich.corider.ui

import com.ralphdietrich.corider.domain.GpxParsingException
import com.ralphdietrich.corider.domain.RouteConversionException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteImportFailureTest {
    @Test
    fun `invalid GPX is classified separately`() {
        assertEquals(
            RouteImportFailure.INVALID_GPX,
            GpxParsingException("Invalid GPX").toRouteImportFailure(),
        )
    }

    @Test
    fun `missing track is classified separately`() {
        assertEquals(
            RouteImportFailure.NO_USABLE_TRACK,
            RouteConversionException("No track").toRouteImportFailure(),
        )
    }

    @Test
    fun `file access failure has a safe fallback`() {
        assertEquals(
            RouteImportFailure.FILE_UNAVAILABLE,
            IOException("Unavailable").toRouteImportFailure(),
        )
    }
}
