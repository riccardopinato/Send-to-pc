package com.riccardopinato.inviaalpc.transfer

object TransferReliabilityPolicy {

    private val uploadIdPattern =
        Regex("[A-Za-z0-9_-]{8,128}")

    fun isValidUploadId(
        value: String
    ): Boolean =
        uploadIdPattern.matches(value)

    fun isValidFileSize(
        totalBytes: Long
    ): Boolean =
        totalBytes in
            1L..TransferLimits.MAX_SINGLE_UPLOAD_BYTES

    fun isValidChunk(
        totalBytes: Long,
        offset: Long,
        contentLength: Long
    ): Boolean {
        if (!isValidFileSize(totalBytes)) {
            return false
        }

        if (
            offset < 0L ||
            contentLength <= 0L ||
            offset > totalBytes
        ) {
            return false
        }

        return contentLength <=
            totalBytes - offset
    }

    fun isValidBatch(
        batchId: String,
        fileCount: Int,
        totalBytes: Long
    ): Boolean {
        if (
            batchId.isBlank() ||
            batchId.length > 100
        ) {
            return false
        }

        if (
            fileCount !in
                1..TransferLimits.MAX_SELECTED_FILES
        ) {
            return false
        }

        if (totalBytes < 0L) {
            return false
        }

        val maxBatchBytes =
            TransferLimits
                .MAX_SINGLE_UPLOAD_BYTES *
                TransferLimits
                    .MAX_SELECTED_FILES

        return totalBytes <=
            maxBatchBytes
    }

    fun bytesPerSecond(
        transferredBytes: Long,
        elapsedMillis: Long
    ): Long {
        if (
            transferredBytes <= 0L ||
            elapsedMillis <= 0L
        ) {
            return 0L
        }

        val scaled =
            if (
                transferredBytes >
                Long.MAX_VALUE / 1_000L
            ) {
                Long.MAX_VALUE
            } else {
                transferredBytes * 1_000L
            }

        return scaled / elapsedMillis
    }

    fun hasStorageCapacity(
        availableBytes: Long,
        incomingBytes: Long,
        headroomBytes: Long =
            TransferLimits.STORAGE_HEADROOM_BYTES
    ): Boolean {
        if (
            availableBytes < 0L ||
            incomingBytes <= 0L ||
            headroomBytes < 0L
        ) {
            return false
        }

        if (
            incomingBytes >
            Long.MAX_VALUE -
                headroomBytes
        ) {
            return false
        }

        val required =
            incomingBytes +
                headroomBytes

        return availableBytes >=
            required
    }
}
