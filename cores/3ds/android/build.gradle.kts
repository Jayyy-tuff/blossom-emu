// SPDX-FileCopyrightText: Copyright 2026 Blossom Emu contributors
// SPDX-License-Identifier: GPL-3.0-or-later

// Versions mirror Azahar's own Android build (src/android/build.gradle.kts).
plugins {
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.0")
    }
}
