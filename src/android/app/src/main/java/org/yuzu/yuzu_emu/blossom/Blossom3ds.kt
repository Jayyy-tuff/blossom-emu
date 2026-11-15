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
 */
object Blossom3ds {
    private const val TAG = "Blossom3ds"
    private const val CORE_APPLICATION = "org.citra.citra_emu.CitraApplication"
    private const val CORE_MAIN_ACTIVITY = "org.citra.citra_emu.ui.main.MainActivity"

    /** True once the 3DS core has been found and started. */
    var isReady: Boolean = false
        private set

    fun init(application: Application) {
        val coreClass = try {
            Class.forName(CORE_APPLICATION)
        } catch (e: Throwable) {
            Log.i(TAG, "3DS core is not part of this build")
            return
        }

        try {
            val core = coreClass.getDeclaredConstructor().newInstance()
            coreClass.getMethod("blossomAttach", Context::class.java).invoke(core, application)
            isReady = true
        } catch (e: Throwable) {
            Log.e(TAG, "3DS core failed to start", e)
        }
    }

    fun launch(context: Context) {
        context.startActivity(Intent().setClassName(context, CORE_MAIN_ACTIVITY))
    }
}
