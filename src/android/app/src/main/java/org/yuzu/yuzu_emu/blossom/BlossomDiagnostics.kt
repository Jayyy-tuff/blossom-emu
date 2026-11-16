// SPDX-FileCopyrightText: Copyright 2026 Blossom Emu contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package org.yuzu.yuzu_emu.blossom

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Collects what is needed to debug a crash from a phone, without a computer:
 * the last uncaught Kotlin/Java exception, and the system's record of why the
 * app's recent processes ended (including the abort message of native crashes).
 */
object BlossomDiagnostics {
    private const val PREFS = "blossom"
    private const val KEY_LAST_EXCEPTION = "last_uncaught_exception"
    private const val MAX_TRACE_BYTES = 512 * 1024
    private val TRACE_KEYWORDS = listOf(
        "abort", "jni", "exception", "error", "signal", "sigsegv", "sigabrt", "fault",
        "citra", "yuzu", "blossom", ".so", "backtrace"
    )

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val text = timestamp(System.currentTimeMillis()) + " on thread " + thread.name +
                    "\n" + Log.getStackTraceString(throwable)
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_LAST_EXCEPTION, text.take(16000)).commit()
            } catch (ignored: Throwable) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun buildReport(context: Context): String {
        val sb = StringBuilder()
        sb.append("Blossom Emu diagnostics\n")
        sb.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            .append(", Android ").append(Build.VERSION.RELEASE)
            .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            sb.append("Build: ").append(info.versionName).append('\n')
        } catch (ignored: Throwable) {
        }
        sb.append("3DS core: available=").append(Blossom3ds.isAvailable)
            .append(", ready=").append(Blossom3ds.isReady)
            .append(", crashedLastStart=").append(Blossom3ds.crashedLastStart).append('\n')

        Blossom3ds.lastError(context)?.let {
            sb.append("\n== 3DS core start error ==\n").append(it).append('\n')
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_EXCEPTION, null)?.let {
                sb.append("\n== Last uncaught exception ==\n").append(it).append('\n')
            }

        sb.append("\n== Recent process exits ==\n")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            sb.append("(needs Android 11 or newer)\n")
            return sb.toString()
        }
        try {
            val manager = context.getSystemService(ActivityManager::class.java)
            val exits = manager.getHistoricalProcessExitReasons(null, 0, 5)
            if (exits.isEmpty()) {
                sb.append("(none recorded)\n")
            }
            for (exit in exits) {
                sb.append(timestamp(exit.timestamp)).append("  ")
                    .append(exit.processName).append("  reason=").append(reasonName(exit.reason))
                    .append(" status=").append(exit.status)
                exit.description?.let { sb.append("  ").append(it) }
                sb.append('\n')
                if (exit.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                    exit.reason == ApplicationExitInfo.REASON_ANR
                ) {
                    sb.append(traceHighlights(exit))
                }
            }
        } catch (e: Throwable) {
            sb.append("Could not read exit reasons: ").append(e).append('\n')
        }
        return sb.toString()
    }

    fun share(context: Context) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "Blossom Emu diagnostics")
            .putExtra(Intent.EXTRA_TEXT, buildReport(context).take(60000))
        context.startActivity(Intent.createChooser(intent, "Blossom Emu diagnostics"))
    }

    /** Pulls the readable, relevant lines out of a native crash record. */
    private fun traceHighlights(exit: ApplicationExitInfo): String {
        val sb = StringBuilder()
        try {
            val bytes = exit.traceInputStream?.use { stream ->
                val buffer = ByteArray(MAX_TRACE_BYTES)
                var total = 0
                while (total < buffer.size) {
                    val read = stream.read(buffer, total, buffer.size - total)
                    if (read <= 0) break
                    total += read
                }
                buffer.copyOf(total)
            } ?: return ""

            val seen = HashSet<String>()
            var lines = 0
            val run = StringBuilder()
            fun flush() {
                val text = run.toString().trim()
                run.setLength(0)
                if (text.length < 12 || lines >= 60) return
                val lower = text.lowercase(Locale.ROOT)
                if (TRACE_KEYWORDS.any { lower.contains(it) } && seen.add(text)) {
                    sb.append("    ").append(text.take(400)).append('\n')
                    lines++
                }
            }
            for (b in bytes) {
                val c = b.toInt() and 0xFF
                if (c in 32..126) run.append(c.toChar()) else flush()
            }
            flush()
        } catch (e: Throwable) {
            sb.append("    (could not read crash record: ").append(e).append(")\n")
        }
        return sb.toString()
    }

    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "CRASH"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
        else -> reason.toString()
    }

    private fun timestamp(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(millis))
}
