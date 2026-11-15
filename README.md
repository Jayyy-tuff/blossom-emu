<!--
# SPDX-FileCopyrightText: Copyright 2025 Eden Emulator Project
# SPDX-License-Identifier: GPL-3.0-or-later

# SPDX-FileCopyrightText: 2018 yuzu Emulator Project
# SPDX-License-Identifier: GPL-2.0-or-later
-->
<!-- lang: en-GB -->

# 🌸 Blossom Emu

**Performance First. Compatibility Always. Customization Without Limits.**

Blossom Emu is an open-source Nintendo Switch emulator for Android, with Nintendo 3DS support planned.
It is a fork of [Eden](https://git.eden-emu.dev/eden-emu/eden); the 3DS core will come from
[Azahar](https://github.com/azahar-emu/azahar). All credit for the emulation itself belongs to those
projects and to yuzu and Citra before them.

- **Status and roadmap:** [docs/blossom/PLAN.md](docs/blossom/PLAN.md)
- **Changes made to Eden:** [docs/blossom/MODIFICATIONS.md](docs/blossom/MODIFICATIONS.md)
- **Licence:** GPL-3.0-or-later, see [LICENSE.txt](LICENSE.txt)

Blossom Emu includes no games, firmware, keys or Nintendo system software. Use your own legally
dumped games and system files.

## Download

Development builds are attached to the `latest` pre-release on this repository's Releases page.
Android 13 or newer, arm64 only.

## Build

Push to `main` (or run the **Blossom Android** workflow by hand) and GitHub Actions builds the APK.
To build locally, follow [docs/build/Android.md](docs/build/Android.md), then:

```sh
cd src/android
./gradlew assembleMainlineRelease
```

---

The original Eden README follows.

<h1 align="center">
  <br>
  <a href="https://git.eden-emu.dev/eden-emu/eden"><img src="./dist/qt_themes/default/icons/256x256/eden.png" alt="Eden" width="200"></a>
  <br>
  <b>Eden</b>
  <br>
</h1>

<h4 align="center"><b>Eden</b> is a free and open-source (FOSS) Switch 1 emulator started by developer Camille LaVey.
<br>
Written in C++, with builds for Windows, Linux, macOS, Android, FreeBSD and more.
</h4>

<p align="center">
    </a>
    <a href="https://discord.gg/HstXbPch7X">
        <img src="https://img.shields.io/discord/1367654015269339267?color=5865F2&label=Eden&logo=discord&logoColor=white"
            alt="Discord">
    </a>
    <a href="https://stt.gg/qKgFEAbH">
        <img src="https://img.shields.io/revolt/invite/qKgFEAbH?color=d61f3a&label=Stoat"
            alt="Stoat">
    </a>
</p>

<p align="center">
  <a href="#compatibility">Compatibility</a> |
  <a href="#contribute">Contribute</a> |
  <a href="#building">Building</a> |
  <a href="#download">Download</a> |
  <a href="#license">License</a>
</p>

## Compatibility

The emulator is capable of running most commercial games at full speed, provided you meet the necessary hardware requirements.

A list of supported games will be available in future. Please be patient.

Check out our [website](https://eden-emu.dev) for the latest news on exciting features, monthly progress reports, and more!

[![Packaging status](https://repology.org/badge/vertical-allrepos/eden-emulator.svg)](https://repology.org/project/eden-emulator/versions)

## Contribute

To contribute to Eden; be it financially, code, bug reports, or otherwise, see our [Contributing guidelines](./CONTRIBUTING.md).

## Documentation

We have a user manual! See our [User Handbook](./docs/user/README.md).

## Building

See the [General Build Guide](docs/Build.md)

For information on provided development tooling, see the [Tools directory](./tools)

## Download

You can download the latest releases from [our release page](https://git.eden-emu.dev/eden-emu/eden/releases).

Save us some bandwidth! We have [mirrors available](./docs/user/ThirdParty.md#mirrors) as well.

## License

Eden is licensed under the GPLv3 (or any later version). Refer to the [LICENSE.txt](https://git.eden-emu.dev/eden-emu/eden/src/branch/master/LICENSE.txt) file.

## Special thanks

Super special thanks to Cloudflare for preventing the git server from blowing up.

- Yuzu
- Ryujinx
- Sudachi
- Citron
- Torzu
- Suyu
- Ryubing

And everyone who continues or had contributed to the project! <3
