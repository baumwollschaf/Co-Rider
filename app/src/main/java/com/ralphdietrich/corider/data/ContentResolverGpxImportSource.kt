package com.ralphdietrich.corider.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.ralphdietrich.corider.domain.GpxImportSource
import java.io.InputStream

class ContentResolverGpxImportSource(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
) : GpxImportSource {
    override val displayName: String by lazy {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumn >= 0 && cursor.moveToFirst()) cursor.getString(nameColumn) else null
        } ?: uri.lastPathSegment ?: "Route.gpx"
    }

    override fun openStream(): InputStream =
        checkNotNull(contentResolver.openInputStream(uri)) {
            "No stream available for the selected document"
        }
}
