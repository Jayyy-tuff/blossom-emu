# SPDX-FileCopyrightText: Copyright 2026 Blossom Emu contributors
# SPDX-License-Identifier: GPL-3.0-or-later

# Rules from Azahar's proguard-rules.pro, applied when the Blossom app is shrunk.
-keep class org.ini4j.spi.IniParser
-keep class org.ini4j.spi.IniBuilder
-keep class org.ini4j.spi.IniFormatter
-dontwarn java.beans.Introspector
-dontwarn java.beans.VetoableChangeListener
-dontwarn java.beans.VetoableChangeSupport

# Azahar's native library looks up Kotlin classes, methods and fields by name when
# it loads, and Azahar's own build runs without R8 optimisation. The Blossom app
# optimises, which may rename, inline or drop those members and abort the process on
# load. Keep the whole 3DS core exactly as compiled. (The host app also starts
# CitraApplication by reflection.)
-keep class org.citra.citra_emu.** { *; }
