package com.riccardopinato.inviaalpc.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.riccardopinato.inviaalpc.AppLanguageStore
import com.riccardopinato.inviaalpc.AppText
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

        private const val WAKE_LOCK_WINDOW_MS =
            2L * 60L * 60L * 1000L

        private const val HEALTH_CHECK_MS = 5_000L
    }

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val connectivityManager by lazy {
        getSystemService(ConnectivityManager::class.java)
    }

    private var sessionJob: Job? = null
    private var networkGraceJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var networkCallbackRegistered = false
    private var expectedIp: String? = null
    private var mismatchSinceMillis: Long? = null

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                evaluateNetwork()
            }

            override fun onLost(network: Network) {
                evaluateNetwork()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: android.net.NetworkCapabilities
            ) {
                evaluateNetwork()
            }

            override fun onLinkPropertiesChanged(
                network: Network,
                linkProperties: android.net.LinkProperties
            ) {
                evaluateNetwork()
            }
        }

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
                terminateSession(
                    SessionEndReason.USER_STOP
                )
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

                expectedIp = session.localIp
                mismatchSinceMillis = null
                ensureWakeLock()
                registerNetworkCallback()
                startSessionHealthLoop()
            }
        }

        return START_NOT_STICKY
    }

    private fun registerNetworkCallback() {
        if (networkCallbackRegistered) {
            return
        }

        runCatching {
            connectivityManager.registerDefaultNetworkCallback(
                networkCallback
            )
            networkCallbackRegistered = true
        }
    }

    private fun unregisterNetworkCallback() {
        if (!networkCallbackRegistered) {
            return
        }

        runCatching {
            connectivityManager.unregisterNetworkCallback(
                networkCallback
            )
        }
        networkCallbackRegistered = false
    }

    private fun startSessionHealthLoop() {
        sessionJob?.cancel()

        sessionJob =
            serviceScope.launch {
                while (true) {
                    ensureWakeLock()

                    val session =
                        TransferRuntime.sessionManager.session.value
                            ?: break

                    if (session.isExpired()) {
                        terminateSession(
                            SessionEndReason.EXPIRED
                        )
                        break
                    }

                    evaluateNetwork()
                    delay(HEALTH_CHECK_MS)
                }
            }
    }

    private fun evaluateNetwork() {
        val expected = expectedIp ?: return
        val observed =
            NetworkUtils.findLanIpv4(
                applicationContext
            )

        if (observed == expected) {
            mismatchSinceMillis = null
            networkGraceJob?.cancel()
            networkGraceJob = null
            return
        }

        if (mismatchSinceMillis == null) {
            mismatchSinceMillis =
                System.currentTimeMillis()
        }

        if (networkGraceJob?.isActive == true) {
            return
        }

        networkGraceJob =
            serviceScope.launch {
                delay(
                    NetworkRecoveryPolicy.DEFAULT_GRACE_MS
                )

                val current =
                    NetworkUtils.findLanIpv4(
                        applicationContext
                    )

                val since = mismatchSinceMillis
                val now = System.currentTimeMillis()

                if (
                    NetworkRecoveryPolicy.graceExpired(
                        expectedIp = expected,
                        observedIp = current,
                        mismatchSinceMillis = since,
                        nowMillis = now
                    )
                ) {
                    val reason =
                        if (current == null) {
                            SessionEndReason.NETWORK_LOST
                        } else {
                            SessionEndReason.IP_CHANGED
                        }

                    terminateSession(reason)
                } else {
                    mismatchSinceMillis = null
                }
            }
    }

    private fun terminateSession(
        reason: SessionEndReason
    ) {
        sessionJob?.cancel()
        networkGraceJob?.cancel()
        unregisterNetworkCallback()
        releaseWakeLock()

        TransferRuntime.stopEverything(reason)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureWakeLock() {
        val current = wakeLock

        if (current?.isHeld == true) {
            return
        }

        val powerManager =
            getSystemService(
                PowerManager::class.java
            )

        wakeLock =
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                packageName + ":transfer"
            ).apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_WINDOW_MS)
            }
    }

    private fun releaseWakeLock() {
        val current = wakeLock

        if (current?.isHeld == true) {
            runCatching {
                current.release()
            }
        }

        wakeLock = null
    }

    override fun onTimeout(
        startId: Int,
        fgsType: Int
    ) {
        terminateSession(
            SessionEndReason.ANDROID_TIMEOUT
        )
        super.onTimeout(startId, fgsType)
    }

    override fun onDestroy() {
        sessionJob?.cancel()
        networkGraceJob?.cancel()
        unregisterNetworkCallback()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized("app_name"))
            .setContentText(localized("notification_active"))
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
                localized("notification_end"),
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

    private fun localized(
        key: String
    ): String =
        AppText.get(
            AppLanguageStore.current(
                applicationContext
            ),
            key
        )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    localized("notification_channel"),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description =
                        localized(
                            "notification_channel_desc"
                        )
                }
            )
    }
}
