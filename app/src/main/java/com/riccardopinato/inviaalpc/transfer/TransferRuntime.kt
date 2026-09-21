package com.riccardopinato.inviaalpc.transfer

import android.content.Context

object TransferRuntime {

    @Volatile
    private var initialized = false

    lateinit var sessionManager: TransferSessionManager
        private set

    lateinit var trustedDeviceStore: TrustedDeviceStore
        private set

    @Volatile
    var server: LocalTransferServer? = null
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return

        sessionManager = TransferSessionManager(context.applicationContext)
        trustedDeviceStore = TrustedDeviceStore(context.applicationContext)
        initialized = true
    }

    @Synchronized
    fun attachServer(value: LocalTransferServer) {
        server?.stop()
        server = value
    }

    @Synchronized
    fun stopServer() {
        runCatching { server?.stop() }
        server = null
    }

    @Synchronized
    fun stopEverything() {
        stopServer()

        if (::sessionManager.isInitialized) {
            sessionManager.stop()
            sessionManager.destroy()
        }
    }
}
