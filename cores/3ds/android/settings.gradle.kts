// SPDX-FileCopyrightText: Copyright 2026 Blossom Emu contributors
// SPDX-License-Identifier: GPL-3.0-or-later

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "blossom-3ds"
include(":core")
