package com.riccardopinato.inviaalpc.transfer

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.min

data class ResumableUploadStatus(
    val uploadId: String,
    val displayName: String,
    val mimeType: String?,
    val totalBytes: Long,
    val receivedBytes: Long,
    val contentUri: String?,
    val completed: Boolean,
    val sha256: String?
)

class ResumableUploadStore(
    context: Context
) {
    private val app = context.applicationContext
    private val resolver = app.contentResolver
    private val prefs =
        app.getSharedPreferences(
            "resumable_uploads",
            Context.MODE_PRIVATE
        )

    private data class Record(
        val uploadId: String,
        val displayName: String,
        val mimeType: String?,
        val totalBytes: Long,
        val receivedBytes: Long,
        val uri: String,
        val expectedSha256: String?,
        val finalSha256: String?,
        val completed: Boolean,
        val updatedAtMillis: Long
    )

    private val records = LinkedHashMap<String, Record>()

    init {
        load()
        cleanupOld()
    }

    @Synchronized
    fun status(uploadId: String): ResumableUploadStatus? =
        records[uploadId]?.toStatus()

    @Synchronized
    fun writeChunk(
        uploadId: String,
        requestedName: String,
        mimeType: String?,
        totalBytes: Long,
        offset: Long,
        input: InputStream,
        contentLength: Long,
        expectedSha256: String?
    ): ResumableUploadStatus {
        require(validUploadId(uploadId)) {
            "Upload id non valido"
        }
        require(totalBytes in 1..TransferLimits.MAX_SINGLE_UPLOAD_BYTES) {
            "Dimensione file non valida"
        }
        require(offset >= 0L && contentLength > 0L) {
            "Chunk non valido"
        }
        require(offset + contentLength <= totalBytes) {
            "Chunk oltre la dimensione del file"
        }

        val current =
            records[uploadId]
                ?: createRecord(
                    uploadId = uploadId,
                    requestedName = requestedName,
                    mimeType = mimeType,
                    totalBytes = totalBytes,
                    expectedSha256 = expectedSha256
                )

        if (current.completed) {
            return current.toStatus()
        }

        if (current.totalBytes != totalBytes) {
            error("Metadati upload non coerenti")
        }

        if (offset != current.receivedBytes) {
            error("OFFSET_MISMATCH:" + current.receivedBytes)
        }

        val uri = Uri.parse(current.uri)

        val written =
            resolver.openFileDescriptor(uri, "rw")
                ?.use { descriptor ->
                    FileOutputStream(descriptor.fileDescriptor)
                        .channel
                        .use { channel ->
                            channel.position(offset)

                            val buffer = ByteArray(CHUNK_IO_BUFFER)
                            var remaining = contentLength
                            var copied = 0L

                            while (remaining > 0L) {
                                val wanted =
                                    min(
                                        remaining,
                                        buffer.size.toLong()
                                    ).toInt()

                                val read =
                                    input.read(
                                        buffer,
                                        0,
                                        wanted
                                    )

                                if (read <= 0) {
                                    error("Chunk interrotto")
                                }

                                val byteBuffer =
                                    java.nio.ByteBuffer.wrap(
                                        buffer,
                                        0,
                                        read
                                    )

                                while (
                                    byteBuffer.hasRemaining()
                                ) {
                                    channel.write(
                                        byteBuffer
                                    )
                                }

                                copied += read
                                remaining -= read
                            }

                            channel.force(false)
                            copied
                        }
                }
                ?: error("Impossibile aprire il file parziale")

        if (written != contentLength) {
            error("Chunk incompleto")
        }

        val received = offset + written
        var updated =
            current.copy(
                receivedBytes = received,
                updatedAtMillis = System.currentTimeMillis()
            )

        if (received == totalBytes) {
            val shouldHash =
                updated.expectedSha256 != null ||
                    totalBytes <= HASH_LIMIT_BYTES

            val finalHash =
                if (shouldHash) {
                    sha256(uri)
                } else {
                    null
                }

            val expected =
                updated.expectedSha256
                    ?.lowercase(Locale.ROOT)
                    ?.takeIf { it.length == 64 }

            if (
                expected != null &&
                finalHash != null &&
                !MessageDigest.isEqual(
                    expected.toByteArray(),
                    finalHash.toByteArray()
                )
            ) {
                runCatching {
                    resolver.delete(
                        uri,
                        null,
                        null
                    )
                }
                records.remove(uploadId)
                persist()
                error("HASH_MISMATCH")
            }

            resolver.update(
                uri,
                ContentValues().apply {
                    put(
                        MediaStore.Downloads.IS_PENDING,
                        0
                    )
                },
                null,
                null
            )

            updated =
                updated.copy(
                    completed = true,
                    finalSha256 = finalHash,
                    updatedAtMillis =
                        System.currentTimeMillis()
                )
        }

        records[uploadId] = updated
        persist()

        return updated.toStatus()
    }

    @Synchronized
    fun cancel(uploadId: String): Boolean {
        val record =
            records[uploadId]
                ?: return false

        if (!record.completed) {
            runCatching {
                resolver.delete(
                    Uri.parse(record.uri),
                    null,
                    null
                )
            }
        }

        records.remove(uploadId)
        persist()
        return true
    }

    @Synchronized
    fun cleanupOld(
        now: Long = System.currentTimeMillis()
    ) {
        val expired =
            records.values.filter {
                now - it.updatedAtMillis >
                    KEEP_METADATA_MS
            }

        expired.forEach { record ->
            if (!record.completed) {
                runCatching {
                    resolver.delete(
                        Uri.parse(record.uri),
                        null,
                        null
                    )
                }
            }

            records.remove(record.uploadId)
        }

        if (expired.isNotEmpty()) {
            persist()
        }
    }

    private fun createRecord(
        uploadId: String,
        requestedName: String,
        mimeType: String?,
        totalBytes: Long,
        expectedSha256: String?
    ): Record {
        val displayName =
            uniqueDisplayName(
                FileNameUtils.sanitize(
                    requestedName
                )
            )

        val values =
            ContentValues().apply {
                put(
                    MediaStore.Downloads.DISPLAY_NAME,
                    displayName
                )
                put(
                    MediaStore.Downloads.MIME_TYPE,
                    mimeType
                        ?: "application/octet-stream"
                )
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS +
                        "/Invia al PC/"
                )
                put(
                    MediaStore.Downloads.IS_PENDING,
                    1
                )
            }

        val uri =
            resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: error(
                "Impossibile creare il file parziale"
            )

        val record =
            Record(
                uploadId = uploadId,
                displayName = displayName,
                mimeType = mimeType,
                totalBytes = totalBytes,
                receivedBytes = 0L,
                uri = uri.toString(),
                expectedSha256 =
                    expectedSha256
                        ?.lowercase(Locale.ROOT)
                        ?.takeIf {
                            it.matches(
                                Regex("[0-9a-f]{64}")
                            )
                        },
                finalSha256 = null,
                completed = false,
                updatedAtMillis =
                    System.currentTimeMillis()
            )

        records[uploadId] = record
        persist()

        return record
    }

    private fun uniqueDisplayName(
        safeName: String
    ): String {
        val dot = safeName.lastIndexOf('.')

        val base =
            if (dot > 0) {
                safeName.substring(0, dot)
            } else {
                safeName
            }

        val ext =
            if (
                dot > 0 &&
                dot < safeName.lastIndex
            ) {
                safeName.substring(dot)
            } else {
                ""
            }

        var candidate = safeName
        var suffix = 1

        while (nameExists(candidate)) {
            candidate =
                base + " (" + suffix + ")" + ext
            suffix++
        }

        return candidate
    }

    private fun nameExists(
        displayName: String
    ): Boolean {
        val projection =
            arrayOf(
                MediaStore.Downloads._ID
            )

        val selection =
            MediaStore.Downloads.DISPLAY_NAME +
                "=?"

        return resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(displayName),
            null
        )?.use {
            it.moveToFirst()
        } ?: false
    }

    private fun sha256(uri: Uri): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        resolver.openInputStream(uri)
            ?.buffered(CHUNK_IO_BUFFER)
            ?.use { input ->
                val buffer =
                    ByteArray(
                        CHUNK_IO_BUFFER
                    )

                while (true) {
                    val read =
                        input.read(buffer)

                    if (read <= 0) {
                        break
                    }

                    digest.update(
                        buffer,
                        0,
                        read
                    )
                }
            }
            ?: error(
                "Impossibile verificare il file"
            )

        return digest.digest()
            .joinToString("") {
                "%02x".format(it)
            }
    }

    private fun validUploadId(
        value: String
    ): Boolean =
        value.matches(
            Regex(
                "[A-Za-z0-9_-]{8,128}"
            )
        )

    private fun Record.toStatus() =
        ResumableUploadStatus(
            uploadId = uploadId,
            displayName = displayName,
            mimeType = mimeType,
            totalBytes = totalBytes,
            receivedBytes = receivedBytes,
            contentUri =
                if (completed) uri else null,
            completed = completed,
            sha256 = finalSha256
        )

    private fun load() {
        val raw =
            prefs.getString(
                KEY,
                null
            ) ?: return

        runCatching {
            val array = JSONArray(raw)

            for (
                index in
                0 until array.length()
            ) {
                val item =
                    array.getJSONObject(
                        index
                    )

                val record =
                    Record(
                        uploadId =
                            item.getString(
                                "id"
                            ),
                        displayName =
                            item.getString(
                                "name"
                            ),
                        mimeType =
                            item.optString(
                                "mime",
                                ""
                            ).takeIf {
                                it.isNotBlank()
                            },
                        totalBytes =
                            item.getLong(
                                "total"
                            ),
                        receivedBytes =
                            item.getLong(
                                "received"
                            ),
                        uri =
                            item.getString(
                                "uri"
                            ),
                        expectedSha256 =
                            item.optString(
                                "expected",
                                ""
                            ).takeIf {
                                it.isNotBlank()
                            },
                        finalSha256 =
                            item.optString(
                                "final",
                                ""
                            ).takeIf {
                                it.isNotBlank()
                            },
                        completed =
                            item.getBoolean(
                                "completed"
                            ),
                        updatedAtMillis =
                            item.getLong(
                                "updated"
                            )
                    )

                records[
                    record.uploadId
                ] = record
            }
        }
    }

    private fun persist() {
        val array = JSONArray()

        records.values.forEach {
            record ->
            array.put(
                JSONObject().apply {
                    put(
                        "id",
                        record.uploadId
                    )
                    put(
                        "name",
                        record.displayName
                    )
                    put(
                        "mime",
                        record.mimeType
                            ?: JSONObject.NULL
                    )
                    put(
                        "total",
                        record.totalBytes
                    )
                    put(
                        "received",
                        record.receivedBytes
                    )
                    put(
                        "uri",
                        record.uri
                    )
                    put(
                        "expected",
                        record.expectedSha256
                            ?: JSONObject.NULL
                    )
                    put(
                        "final",
                        record.finalSha256
                            ?: JSONObject.NULL
                    )
                    put(
                        "completed",
                        record.completed
                    )
                    put(
                        "updated",
                        record.updatedAtMillis
                    )
                }
            )
        }

        prefs.edit()
            .putString(
                KEY,
                array.toString()
            )
            .apply()
    }

    companion object {
        private const val KEY =
            "uploads"

        private const val CHUNK_IO_BUFFER =
            256 * 1024

        private const val KEEP_METADATA_MS =
            24L * 60L * 60L * 1000L

        private const val HASH_LIMIT_BYTES =
            256L * 1024L * 1024L
    }
}
