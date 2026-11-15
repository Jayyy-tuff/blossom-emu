# Modifications to Eden

Blossom Emu is a modified version of Eden (GPL-3.0-or-later). This file lists every change
made to the upstream source, as the licence requires.

Base: Eden commit `b4e39d9ae412eebcc896759eedd58226d43a43c5` (15 Nov 2026).

## 15 Nov 2026 — branding and CI

| File | Change |
|---|---|
| `src/android/app/build.gradle.kts` | Application ID `org.blossomemu.blossom`; app name "Blossom Emu" (mainline flavour). The Kotlin namespace `org.yuzu.yuzu_emu` is unchanged because the JNI layer depends on it. |
| `src/android/app/src/main/res/values/strings.xml` | 14 user-facing English strings say "Blossom Emu". Credits, links and translations still name Eden. |
| `src/android/app/src/main/res/values/yuzu_colors.xml`, `values-night/yuzu_colors.xml` | The `eden_*` theme colours use the Blossom palette. |
| `src/android/app/src/main/res/values/colors.xml` | Launcher icon background `#15151D`. |
| `res/drawable/ic_launcher_foreground.png`, `ic_yuzu.png`, `ic_yuzu_splash.png`, `res/mipmap-*/ic_launcher.png` | New original Blossom icon. |
| `.github/workflows/blossom-android.yml` | New: builds the arm64 APK and publishes it to a `latest` pre-release. |
| `README.md`, `docs/blossom/` | New Blossom documentation. |

No emulation code has been changed.
