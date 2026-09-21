package com.riccardopinato.inviaalpc.transfer

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

class ReceivedFileStore(
    private val context: Context
) {

    data class Destination(
        val uri: Uri,
        val displayName: String,
        val output: OutputStream
    )

    fun create(requestedName: String, mimeType: String?): Destination {
        val resolver = context.contentResolver
        val safeName = FileNameUtils.sanitize(requestedName)

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, safeName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType ?: "application/octet-stream")
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/Invia al PC/"
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        ) ?: error("Impossibile creare il file ricevuto.")

        val output = resolver.openOutputStream(uri, "w") ?: run {
            resolver.delete(uri, null, null)
            error("Impossibile aprire il file ricevuto.")
        }

        return Destination(
            uri = uri,
            displayName = safeName,
            output = output
        )
    }

    fun complete(destination: Destination) {
        context.contentResolver.update(
            destination.uri,
            ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            },
            null,
            null
        )
    }

    fun abort(destination: Destination) {
        runCatching { destination.output.close() }
        runCatching {
            context.contentResolver.delete(destination.uri, null, null)
        }
    }
}
