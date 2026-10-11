package com.riccardopinato.inviaalpc.transfer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferReliabilityPolicyTest {

    @Test
    fun acceptsExactSingleFileLimit() {
        assertTrue(
            TransferReliabilityPolicy
                .isValidFileSize(
                    TransferLimits
                        .MAX_SINGLE_UPLOAD_BYTES
                )
        )
    }

    @Test
    fun rejectsFileOverLimit() {
        assertFalse(
            TransferReliabilityPolicy
                .isValidFileSize(
                    TransferLimits
                        .MAX_SINGLE_UPLOAD_BYTES +
                        1L
                )
        )
    }

    @Test
    fun chunkBoundaryIsOverflowSafe() {
        val total =
            TransferLimits
                .MAX_SINGLE_UPLOAD_BYTES

        assertTrue(
            TransferReliabilityPolicy
                .isValidChunk(
                    totalBytes = total,
                    offset = total - 1L,
                    contentLength = 1L
                )
        )

        assertFalse(
            TransferReliabilityPolicy
                .isValidChunk(
                    totalBytes = total,
                    offset = total - 1L,
                    contentLength = Long.MAX_VALUE
                )
        )
    }

    @Test
    fun rejectsChunkPastEnd() {
        assertFalse(
            TransferReliabilityPolicy
                .isValidChunk(
                    totalBytes = 1_024L,
                    offset = 1_000L,
                    contentLength = 25L
                )
        )
    }

    @Test
    fun batchAllowsHundredFilesAtLimit() {
        assertTrue(
            TransferReliabilityPolicy
                .isValidBatch(
                    batchId =
                        "batch_12345678",
                    fileCount =
                        TransferLimits
                            .MAX_SELECTED_FILES,
                    totalBytes =
                        TransferLimits
                            .MAX_SINGLE_UPLOAD_BYTES *
                            TransferLimits
                                .MAX_SELECTED_FILES
                )
        )
    }

    @Test
    fun batchRejectsHundredAndOneFiles() {
        assertFalse(
            TransferReliabilityPolicy
                .isValidBatch(
                    batchId =
                        "batch_12345678",
                    fileCount =
                        TransferLimits
                            .MAX_SELECTED_FILES +
                            1,
                    totalBytes = 1L
                )
        )
    }

    @Test
    fun uploadIdHasBoundedSafeShape() {
        assertTrue(
            TransferReliabilityPolicy
                .isValidUploadId(
                    "upload_12345678"
                )
        )

        assertFalse(
            TransferReliabilityPolicy
                .isValidUploadId(
                    "../bad"
                )
        )
    }

    @Test
    fun storageCapacityHonorsHeadroom() {
        val incoming = 1_000L
        val headroom = 100L

        assertTrue(
            TransferReliabilityPolicy
                .hasStorageCapacity(
                    availableBytes = 1_100L,
                    incomingBytes = incoming,
                    headroomBytes = headroom
                )
        )

        assertFalse(
            TransferReliabilityPolicy
                .hasStorageCapacity(
                    availableBytes = 1_099L,
                    incomingBytes = incoming,
                    headroomBytes = headroom
                )
        )
    }

    @Test
    fun storageCapacityRejectsLongOverflow() {
        assertFalse(
            TransferReliabilityPolicy
                .hasStorageCapacity(
                    availableBytes =
                        Long.MAX_VALUE,
                    incomingBytes =
                        Long.MAX_VALUE,
                    headroomBytes = 1L
                )
        )
    }
}
