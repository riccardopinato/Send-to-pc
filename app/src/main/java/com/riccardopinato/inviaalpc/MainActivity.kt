package com.riccardopinato.inviaalpc

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.riccardopinato.inviaalpc.ui.InviaAlPcApp
import com.riccardopinato.inviaalpc.ui.theme.InviaAlPcTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TransferViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        processIntent(intent)

        setContent {
            InviaAlPcTheme {
                InviaAlPcApp(
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
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
