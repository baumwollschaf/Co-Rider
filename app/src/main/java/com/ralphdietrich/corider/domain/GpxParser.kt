package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.GpxDocument
import java.io.InputStream

interface GpxParser {
    suspend fun parse(input: InputStream): GpxDocument
}
