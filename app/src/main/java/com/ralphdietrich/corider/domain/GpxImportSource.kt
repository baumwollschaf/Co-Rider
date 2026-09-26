package com.ralphdietrich.corider.domain

import java.io.InputStream

interface GpxImportSource {
    val displayName: String

    fun openStream(): InputStream
}
