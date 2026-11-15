# SPDX-FileCopyrightText: Copyright 2026 Blossom Emu contributors
# SPDX-License-Identifier: GPL-3.0-or-later

# Rules from Azahar's proguard-rules.pro, applied when the Blossom app is shrunk.
-keep class org.ini4j.spi.IniParser
-keep class org.ini4j.spi.IniBuilder
-keep class org.ini4j.spi.IniFormatter
-dontwarn java.beans.Introspector
-dontwarn java.beans.VetoableChangeListener
-dontwarn java.beans.VetoableChangeSupport

# The host app starts the 3DS core by reflection.
-keep class org.citra.citra_emu.CitraApplication { *; }
