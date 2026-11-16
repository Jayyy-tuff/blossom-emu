// SPDX-FileCopyrightText: Copyright 2026 Blossom Emu contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package org.yuzu.yuzu_emu.blossom

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Bridge to the optional Nintendo 3DS core (Azahar), which is embedded as a prebuilt
 * library. Nothing here links against the core at compile time, so the app still
 * builds and runs when the core is left out.
 *
 * Starting the core loads a second native emulator into the process. A crash there
 * must never stop the Switch side from opening, so every start is bracketed by a
 * persisted "starting" flag: if the flag is still set on the next launch, the last
 * start killed the process and the core is left alone until the user retries.
 */
object Blossom3ds {
    private const val TAG = "Blossom3ds"
    private const val CORE_APPLICATION = "org.citra.citra_emu.CitraApplication"
    private const val CORE_MAIN_ACTIVITY = "org.citra.citra_emu.ui.main.MainActivity"
    private const val PREFS = "blossom"
    private const val KEY_STARTING = "c3ds_starting"
    private const val KEY_LAST_ERROR = "c3ds_last_error"
    private const val KEY_USED = "c3ds_used"

    private var application: Application? = null

    /** True when the 3DS core is part of this build. */
    var isAvailable: Boolean = false
        private set

    /** True once the 3DS core has been started in this process. */
    var isReady: Boolean = false
        private set

    /** True when the previous attempt to start the core ended the process. */
    var crashedLastStart: Boolean = false
        private set

    fun init(app: Application) {
        application = app
        isAvailable = try {
            Class.forName(CORE_APPLICATION, false, Blossom3ds::class.java.classLoader)
            true
        } catch (e: Throwable) {
            false
        }
        if (!isAvailable) {
            Log.i(TAG, "3DS core is not part of this build")
            return
        }

        if (prefs(app).getBoolean(KEY_STARTING, false)) {
            crashedLastStart = true
            Log.w(TAG, "3DS core crashed during its last start; not starting it automatically")
            return
        }

        // Only preload the core for people who use it. Preloading is what lets Android
        // restore a 3DS screen after it has reclaimed the app's process.
        if (prefs(app).getBoolean(KEY_USED, false)) {
            start()
        }
    }

    /** Starts the core if needed. Returns true when it is running. */
    fun start(): Boolean {
        if (isReady) {
            return true
        }
        val app = application ?: return false
        if (!isAvailable) {
            return false
        }

        val prefs = prefs(app)
        prefs.edit().putBoolean(KEY_STARTING, true).commit()
        try {
            val coreClass = Class.forName(CORE_APPLICATION)
            val core = coreClass.getDeclaredConstructor().newInstance()
            coreClass.getMethod("blossomAttach", Context::class.java).invoke(core, app)
            isReady = true
            crashedLastStart = false
            prefs.edit().remove(KEY_LAST_ERROR).apply()
        } catch (e: Throwable) {
            val cause = e.cause ?: e
            Log.e(TAG, "3DS core failed to start", cause)
            prefs.edit().putString(KEY_LAST_ERROR, Log.getStackTraceString(cause)).apply()
        }
        prefs.edit().putBoolean(KEY_STARTING, false).commit()
        return isReady
    }

    /** Opens the 3DS library. Returns false when the core could not be started. */
    fun launch(context: Context): Boolean {
        if (!start()) {
            return false
        }
        prefs(context).edit().putBoolean(KEY_USED, true).apply()
        context.startActivity(Intent().setClassName(context, CORE_MAIN_ACTIVITY))
        return true
    }

    fun lastError(context: Context): String? = prefs(context).getString(KEY_LAST_ERROR, null)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
