package com.riccardopinato.inviaalpc.transfer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

object SharedItemResolver {

    fun resolve(context: Context, uri: Uri): SharedItem {
        tryPersistReadPermission(context, uri)

        val resolver = context.contentResolver
        var displayName = uri.lastPathSegment ?: "file"
        var sizeBytes: Long? = null

        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    displayName = cursor.getString(nameIndex)
                        ?.takeIf { it.isNotBlank() }
                        ?: displayName
                }

                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        return SharedItem(
            uri = uri,
            displayName = FileNameUtils.sanitize(displayName),
            mimeType = resolver.getType(uri),
            sizeBytes = sizeBytes
        )
    }

    fun resolveAll(context: Context, uris: List<Uri>): List<SharedItem> =
        uris
            .distinctBy { it.toString() }
            .mapNotNull { uri ->
                runCatching { resolve(context, uri) }.getOrNull()
            }

    private fun tryPersistReadPermission(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
}
