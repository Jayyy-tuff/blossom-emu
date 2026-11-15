# Blossom Emu — technical plan and status

Blossom Emu is a performance-first Nintendo Switch and Nintendo 3DS emulator for Android.
It is a fork of real, working emulators, not a rewrite.

No games, firmware, keys or Nintendo system applications are included, and none will be.

## 1. Upstream audit (15 Nov 2026)

| | Switch core | 3DS core |
|---|---|---|
| Project | Eden (yuzu lineage) | Azahar (Citra lineage) |
| Source used | `github.com/eden-emulator/mirror` (mirror of `git.eden-emu.dev/eden-emu/eden`) | `github.com/azahar-emu/azahar` |
| Commit audited | `b4e39d9ae412` | `11eceda5e0e6` |
| Licence | GPL-3.0-or-later (some files GPL-2.0-or-later) | File headers: GPLv2-or-later and GPLv3-or-later |
| Android app | `src/android`, Kotlin + JNI, namespace `org.yuzu.yuzu_emu` | `src/android`, Kotlin + JNI, namespace `org.citra.citra_emu` |
| Android toolchain | AGP 8.13, compileSdk 36, NDK 30, CMake 3.31.6, arm64-v8a | compileSdk 37, NDK 27.3 |

Licence conclusion: both cores can ship in one application under **GPL-3.0-or-later**.
Blossom keeps every upstream copyright header and licence file.

### What Eden already provides (found in the source, not yet re-tested by Blossom)

| Blossom spec item | Where it lives in Eden |
|---|---|
| Vulkan renderer, shader and pipeline caches | `src/video_core` |
| ARM64 JIT | `src/dynarmic` |
| Custom GPU drivers (Turnip etc.) | libadrenotools, `src/android/app/src/main/jni` |
| Android performance hints (ADPF) | `src/common/adpf.cpp` |
| Mods: LayeredFS/RomFS, ExeFS, IPS, IPSwitch, cheats | `src/core/file_sys/patch_manager.cpp`, `ips_layer.cpp` |
| QLaunch / system applet launching from user-supplied firmware | `AppletLauncherFragment.kt`, `model/Applet.kt` |
| Per-game settings, on-screen controls, controller mapping | `src/android/app` |

So most of the Switch side of the Blossom spec is inherited. Blossom's own work is the
unified app, the 3DS core, the mod and performance UX, and measured tuning.

## 2. Status

Legend: **Done** = in this repository. **Inherited** = upstream Eden feature, untested by Blossom.
**Planned** = not started.

| Area | Status |
|---|---|
| Eden fork with full upstream history (mergeable) | Done |
| Blossom name, application ID `org.blossomemu.blossom`, icon, pink/lavender/charcoal palette | Done |
| GitHub Actions build producing an arm64 APK | Done (workflow written; first run not yet executed) |
| Switch emulation | Inherited |
| Mods, custom drivers, QLaunch applet | Inherited |
| 3DS core (Azahar) in the same app | Planned — step 2 |
| Blossom Home (unified Switch + 3DS library) | Planned |
| Blossom Mods dashboard (profiles, conflicts, load order) | Planned |
| Blossom Performance Engine presets and overlay | Planned |
| Any performance gain over Eden | None claimed. Nothing has been benchmarked. |

## 3. Roadmap

### Phase 1a — Switch (this commit)
Branded Eden fork that builds in CI and runs Switch games.
Exit test: APK installs on a Galaxy S25 FE and boots one legally dumped game.

### Phase 1b — 3DS core
1. Add Azahar as a git submodule at `cores/3ds/azahar` (kept independent, so upstream merges stay clean).
2. Build its Android app as a Gradle **library** module (`:azahar`) through a small patch set in
   `cores/3ds/patches`: application plugin → library plugin, resource prefix to avoid name
   clashes with the Switch app, its own NDK version.
3. Both native libraries ship side by side in one APK. Each core keeps its own emulation activity.
4. Add Blossom Home: one launcher activity with Switch and 3DS libraries, which starts the
   right core's activity for the selected game.

Known risks: duplicate resource and class names between two yuzu/Citra-lineage apps, two NDK
versions in one build, APK size, CI build time roughly doubling.

### Phase 2 — Performance
Record baseline frame-time captures of unmodified Eden on the test phone first.
Only then add presets (Eco, Balanced, Performance, Turbo, Custom) as bundles of existing
settings, and keep changes that measure better without breaking games.

### Phase 3 — Blossom Mods
Per-game dashboard on top of the existing patch manager: ZIP/folder import, enable/disable,
priority, conflict detection (two mods replacing the same RomFS path), profiles.

### Phase 4 — QLaunch
Start from Eden's existing applet launcher. Document which firmware versions boot the HOME
Menu, what breaks, and fill in missing services incrementally.

### Phase 5 and 6 — UI polish, stability, public beta
As in the project brief.

## 4. Keeping up with upstream

```sh
git remote add upstream https://github.com/eden-emulator/mirror
git fetch upstream
git merge upstream/master
```

Blossom changes are kept small and listed in `MODIFICATIONS.md` so merges stay easy.
