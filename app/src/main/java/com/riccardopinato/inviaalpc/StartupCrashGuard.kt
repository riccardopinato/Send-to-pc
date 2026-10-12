package com.riccardopinato.inviaalpc

import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter

object StartupCrashGuard {
    private const val PREFS = "startup_crash_guard"
    private const val KEY_REPORT = "pending_report"
    private const val KEY_STAGE = "startup_stage"
    private const val KEY_TIME = "crash_time"
    private const val KEY_STARTUP_COMPLETE = "startup_complete"

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true

        val app = context.applicationContext

        runCatching {
            app.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .edit()
                .putBoolean(
                    KEY_STARTUP_COMPLETE,
                    false
                )
                .commit()
        }

        val previous =
            Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler {
                thread,
                throwable ->
            record(
                context = app,
                throwable = throwable
            )

            previous?.uncaughtException(
                thread,
                throwable
            )
        }
    }

    fun markStage(
        context: Context,
        stage: String
    ) {
        runCatching {
            prefs.edit()
                .putString(
                    KEY_STAGE,
                    stage
                )
                .commit()
        }
    }

    fun hasPendingCrash(
        context: Context
    ): Boolean =
        runCatching {
            !context.applicationContext
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .getString(
                    KEY_REPORT,
                    null
                )
                .isNullOrBlank()
        }.getOrDefault(false)

    fun report(
        context: Context
    ): String {
        val prefs =
            context.applicationContext
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )

        val report =
            prefs.getString(
                KEY_REPORT,
                null
            ) ?: "No captured startup exception."

        val stage =
            prefs.getString(
                KEY_STAGE,
                "unknown"
            ) ?: "unknown"

        val time =
            prefs.getLong(
                KEY_TIME,
                0L
            )

        return buildString {
            appendLine(
                "Send to PC startup diagnostics"
            )
            appendLine(
                "Version: " +
                    BuildConfig.VERSION_NAME +
                    " (" +
                    BuildConfig.VERSION_CODE +
                    ")"
            )
            appendLine(
                "Stage: " +
                    stage
            )
            appendLine(
                "Crash time: " +
                    time
            )
            appendLine()
            append(report)
        }
    }

    fun markStartupCompleted(
        context: Context
    ) {
        runCatching {
            context.applicationContext
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .edit()
                .putBoolean(
                    KEY_STARTUP_COMPLETE,
                    true
                )
                .putString(
                    KEY_STAGE,
                    "startup_completed"
                )
                .commit()
        }
    }

    fun clear(
        context: Context
    ) {
        runCatching {
            context.applicationContext
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .edit()
                .remove(
                    KEY_REPORT
                )
                .remove(
                    KEY_TIME
                )
                .putBoolean(
                    KEY_STARTUP_COMPLETE,
                    false
                )
                .putString(
                    KEY_STAGE,
                    "manual_retry"
                )
                .commit()
        }
    }

    private fun record(
        context: Context,
        throwable: Throwable
    ) {
        runCatching {
            val prefs =
                context.applicationContext
                    .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                    )

            if (
                prefs.getBoolean(
                    KEY_STARTUP_COMPLETE,
                    false
                )
            ) {
                return@runCatching
            }

            val writer =
                StringWriter()

            throwable.printStackTrace(
                PrintWriter(
                    writer
                )
            )

            context.applicationContext
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .edit()
                .putString(
                    KEY_REPORT,
                    writer.toString()
                        .take(32_000)
                )
                .putLong(
                    KEY_TIME,
                    System.currentTimeMillis()
                )
                .commit()
        }
    }
}
