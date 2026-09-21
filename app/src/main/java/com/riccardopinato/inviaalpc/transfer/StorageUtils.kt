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

    fun canReceive(context: Context, incomingBytes: Long): Boolean {
        if (incomingBytes <= 0L) return false

        val required = incomingBytes + TransferLimits.STORAGE_HEADROOM_BYTES
        return availableBytes(context) >= required
    }
}
