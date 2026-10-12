package com.riccardopinato.inviaalpc

import android.app.Application
import com.riccardopinato.inviaalpc.transfer.TransferRuntime

class InviaAlPcApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        StartupCrashGuard.install(this)
        StartupCrashGuard.markStage(
            this,
            "application_onCreate"
        )

        if (
            StartupCrashGuard
                .hasPendingCrash(this)
        ) {
            StartupCrashGuard.markStage(
                this,
                "safe_mode_skip_runtime"
            )
            return
        }

        StartupCrashGuard.markStage(
            this,
            "before_transfer_runtime"
        )

        TransferRuntime.initialize(this)

        StartupCrashGuard.markStage(
            this,
            "after_transfer_runtime"
        )
    }
}
