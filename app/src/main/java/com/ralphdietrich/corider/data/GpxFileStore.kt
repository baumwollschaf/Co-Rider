package com.ralphdietrich.corider.data

import java.io.InputStream

interface GpxFileStore {
    suspend fun store(routeId: String, input: InputStream): String

    suspend fun open(internalFileName: String): InputStream

    suspend fun delete(internalFileName: String)
}
