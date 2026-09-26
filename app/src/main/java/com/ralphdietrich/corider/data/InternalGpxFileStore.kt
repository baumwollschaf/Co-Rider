package com.ralphdietrich.corider.data

import android.content.Context
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InternalGpxFileStore(
    private val context: Context,
) : GpxFileStore {
    override suspend fun store(routeId: String, input: InputStream): String =
        withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, GPX_DIRECTORY).apply { mkdirs() }
            val fileName = "$routeId.gpx"
            val destination = File(directory, fileName)
            try {
                input.use { source ->
                    destination.outputStream().use(source::copyTo)
                }
            } catch (error: Throwable) {
                destination.delete()
                throw error
            }
            fileName
        }

    override suspend fun open(internalFileName: String): InputStream =
        withContext(Dispatchers.IO) {
            require('/' !in internalFileName && '\\' !in internalFileName) {
                "Invalid internal GPX file name"
            }
            File(File(context.filesDir, GPX_DIRECTORY), internalFileName).inputStream()
        }

    override suspend fun delete(internalFileName: String) {
        withContext(Dispatchers.IO) {
            File(File(context.filesDir, GPX_DIRECTORY), internalFileName).delete()
        }
    }

    private companion object {
        const val GPX_DIRECTORY = "gpx"
    }
}
