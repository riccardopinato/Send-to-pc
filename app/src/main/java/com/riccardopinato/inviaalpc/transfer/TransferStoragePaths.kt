package com.riccardopinato.inviaalpc.transfer

import android.os.Environment

object TransferStoragePaths {

    val CURRENT_RELATIVE_PATH: String =
        Environment.DIRECTORY_DOWNLOADS +
            "/Send to PC/"

    val LEGACY_RELATIVE_PATH: String =
        Environment.DIRECTORY_DOWNLOADS +
            "/Invia al PC/"

    val MANAGED_RELATIVE_PATHS: List<String> =
        listOf(
            CURRENT_RELATIVE_PATH,
            LEGACY_RELATIVE_PATH
        )
}
