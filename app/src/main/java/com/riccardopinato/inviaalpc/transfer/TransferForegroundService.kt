package com.riccardopinato.inviaalpc.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.riccardopinato.inviaalpc.MainActivity
import com.riccardopinato.inviaalpc.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TransferForegroundService : Service() {

    companion object {
        const val ACTION_START =
            "com.riccardopinato.inviaalpc.action.START"

        const val ACTION_STOP =
            "com.riccardopinato.inviaalpc.action.STOP"

        private const val CHANNEL_ID =
            "transfer_session"

        private const val NOTIFICATION_ID = 101
    }

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var watcherJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        TransferRuntime.initialize(applicationContext)
        createChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                terminateSession()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                val session =
                    TransferRuntime.sessionManager.session.value
                        ?: run {
                            stopSelf()
                            return START_NOT_STICKY
                        }

                startForeground(
                    NOTIFICATION_ID,
                    buildNotification()
                )

                startWatcher(session.localIp)
            }
        }

        return START_NOT_STICKY
    }

    private fun startWatcher(expectedIp: String) {
        watcherJob?.cancel()

        watcherJob =
            serviceScope.launch {
                while (true) {
                    val session =
                        TransferRuntime.sessionManager.session.value
                            ?: break

                    if (session.isExpired()) {
                        TransferRuntime.sessionManager.expire()
                        terminateSession()
                        break
                    }

                    val currentIp =
                        NetworkUtils.findLanIpv4(applicationContext)

                    if (currentIp == null || currentIp != expectedIp) {
                        TransferRuntime.sessionManager.updateStatus(
                            TransferStatus.ERROR
                        )
                        terminateSession()
                        break
                    }

                    delay(1_000L)
                }
            }
    }

    private fun terminateSession() {
        watcherJob?.cancel()
        TransferRuntime.stopEverything()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        terminateSession()
        super.onTimeout(startId, fgsType)
    }

    override fun onDestroy() {
        watcherJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Invia al PC")
            .setContentText("Sessione locale attiva")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    10,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                0,
                "Termina",
                PendingIntent.getService(
                    this,
                    11,
                    Intent(
                        this,
                        TransferForegroundService::class.java
                    ).apply {
                        action = ACTION_STOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Trasferimento locale",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description =
                        "Sessioni di trasferimento file tramite rete locale"
                }
            )
    }
}
