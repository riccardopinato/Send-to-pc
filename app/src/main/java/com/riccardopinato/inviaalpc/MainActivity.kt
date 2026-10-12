package com.riccardopinato.inviaalpc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.riccardopinato.inviaalpc.monetization.MonetizationController
import com.riccardopinato.inviaalpc.ui.InviaAlPcApp
import com.riccardopinato.inviaalpc.ui.theme.InviaAlPcTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TransferViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StartupCrashGuard.markStage(
            this,
            "main_after_super"
        )

        if (
            StartupCrashGuard
                .hasPendingCrash(this)
        ) {
            showStartupCrashRecovery()
            return
        }

        StartupCrashGuard.markStage(
            this,
            "before_enable_edge_to_edge"
        )
        enableEdgeToEdge()

        StartupCrashGuard.markStage(
            this,
            "before_process_intent"
        )
        processIntent(intent)

        StartupCrashGuard.markStage(
            this,
            "before_set_content"
        )
        setContent {
            val uiState by
                viewModel.uiState
                    .collectAsState()

            InviaAlPcTheme(
                themeMode =
                    uiState.themeMode
            ) {
                InviaAlPcApp(
                    viewModel = viewModel
                )
            }
        }

        StartupCrashGuard.markStage(
            this,
            "after_set_content"
        )

        runCatching {
            StartupCrashGuard.markStage(
                this,
                "before_monetization"
            )

            MonetizationController
                .initialize(this)

            StartupCrashGuard.markStage(
                this,
                "after_monetization"
            )
        }.onFailure {
            MonetizationController
                .disableForSession(
                    stage = "activity_startup",
                    throwable = it
                )
        }

        window.decorView.postDelayed(
            {
                StartupCrashGuard
                    .markStartupCompleted(
                        this
                    )
            },
            15_000L
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (
            StartupCrashGuard
                .hasPendingCrash(this)
        ) {
            return
        }

        setIntent(intent)
        processIntent(intent)
    }

    private fun showStartupCrashRecovery() {
        val density =
            resources.displayMetrics.density

        fun dp(value: Int): Int =
            (value * density).toInt()

        val report =
            StartupCrashGuard.report(this)

        val title =
            TextView(this).apply {
                text =
                    "Send to PC - Startup diagnostics"
                textSize = 20f
                setPadding(
                    dp(16),
                    dp(20),
                    dp(16),
                    dp(8)
                )
            }

        val info =
            TextView(this).apply {
                text =
                    "È stato rilevato un crash durante l'avvio precedente. " +
                        "Copia o fotografa il report qui sotto."
                textSize = 15f
                setPadding(
                    dp(16),
                    dp(4),
                    dp(16),
                    dp(12)
                )
            }

        val reportView =
            TextView(this).apply {
                text = report
                textSize = 12f
                setTextIsSelectable(true)
                setPadding(
                    dp(16),
                    dp(8),
                    dp(16),
                    dp(16)
                )
            }

        val copyButton =
            Button(this).apply {
                text = "Copia report"
                setOnClickListener {
                    val clipboard =
                        getSystemService(
                            Context.CLIPBOARD_SERVICE
                        ) as ClipboardManager

                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            "Send to PC startup report",
                            report
                        )
                    )

                    text = "Report copiato"
                }
            }

        val resetButton =
            Button(this).apply {
                text =
                    "Azzera report e chiudi"
                setOnClickListener {
                    StartupCrashGuard.clear(
                        this@MainActivity
                    )
                    finishAndRemoveTask()
                    android.os.Process.killProcess(
                        android.os.Process.myPid()
                    )
                }
            }

        val column =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                addView(title)
                addView(info)
                addView(
                    copyButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(
                            dp(16),
                            dp(4),
                            dp(16),
                            dp(4)
                        )
                    }
                )
                addView(
                    resetButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(
                            dp(16),
                            dp(4),
                            dp(16),
                            dp(8)
                        )
                    }
                )
                addView(reportView)
            }

        val scroll =
            ScrollView(this).apply {
                addView(column)
            }

        setContentView(scroll)
    }

    private fun processIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            Intent.EXTRA_STREAM,
                            Uri::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }

                if (uri != null) {
                    viewModel.addUris(listOf(uri))
                    viewModel.setSection(HomeSection.SEND)
                    return
                }

                val text =
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                        ?.trim()

                if (text.isNullOrBlank()) return

                if (
                    text.startsWith("http://", ignoreCase = true) ||
                    text.startsWith("https://", ignoreCase = true)
                ) {
                    viewModel.setSharedLink(text)
                } else {
                    viewModel.setSharedText(text)
                }

                viewModel.setSection(HomeSection.SEND)
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(
                            Intent.EXTRA_STREAM,
                            Uri::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra<Uri>(
                            Intent.EXTRA_STREAM
                        )
                    }

                if (!uris.isNullOrEmpty()) {
                    viewModel.addUris(uris)
                    viewModel.setSection(HomeSection.SEND)
                }
            }
        }
    }
}
