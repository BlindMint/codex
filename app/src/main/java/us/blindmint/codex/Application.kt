/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import us.blindmint.codex.core.crash.CrashHandler
import us.blindmint.codex.data.security.CredentialEncryptor

@HiltAndroidApp
class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        CredentialEncryptor.initialize(this)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this, defaultHandler))
    }
}