package com.riccardopinato.inviaalpc.transfer

import android.content.Context
import android.os.StatFs

object StorageUtils {

    fun availableBytes(context: Context): Long {
        val base = context.getExternalFilesDir(null) ?: context.filesDir

        return runCatching {
            StatFs(base.absolutePath).availableBytes
        }.getOrDefault(0L)
    }

    fun canReceive(
        context: Context,
        incomingBytes: Long
    ): Boolean =
        TransferReliabilityPolicy
            .hasStorageCapacity(
                availableBytes =
                    availableBytes(
                        context
                    ),
                incomingBytes =
                    incomingBytes
            )
}
