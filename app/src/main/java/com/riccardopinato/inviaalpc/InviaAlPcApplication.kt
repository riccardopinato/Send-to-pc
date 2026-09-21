package com.riccardopinato.inviaalpc

import android.app.Application
import com.riccardopinato.inviaalpc.transfer.TransferRuntime

class InviaAlPcApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        TransferRuntime.initialize(this)
    }
}
