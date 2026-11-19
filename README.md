<h1 align="center">Kozu</h1>

<p align="center">
  <a href="https://github.com/kanzick/Kozu/releases"><img src="https://img.shields.io/github/v/release/kanzick/Kozu?include_prereleases" alt="Release version" /></a>
  <img src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform: Android" />
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <a href="LICENSE"><img src="https://img.shields.io/github/license/kanzick/Kozu" alt="License" /></a>
</p>

<p align="center">
  A music app for osu! built with Kotlin.
</p>

---

## Overview

Listen your favourite songs from osu! without launching the base game. It aims to be small, fast and pleasant to use. 

**Quick links**

- [Download](https://github.com/kanzick/Kozu/releases)
- [Report a bug / request a feature](https://github.com/kanzick/Kozu/issues)
- [Build from source](#build-from-source)

<!-- TODO: add a screenshot (or 2-3 side by side), e.g.
<p align="center">
  <img src="docs/images/screenshot-library.png" width="240" />
  <img src="docs/images/screenshot-player.png" width="240" />
</p>
-->

## Features

<!-- TODO: edit this list so it matches what the app actually does today. -->

- 🎧 Music player focused on osu! songs
- 🎮 Intergrated osu!mania for mobile  

## Getting started

### Install

Download the latest APK from the [Releases](https://github.com/kanzick/Kozu/releases) page and install it on your device. You may need to allow installs from unknown sources.

### Build from source

**Requirements**

- [Android Studio](https://developer.android.com/studio)
- JDK 17 or newer.
- An Android device or emulator.

**Steps**

```bash
# 1. Clone repo
git clone https://github.com/kanzick/Kozu.git
cd Kozu

# 2. Build a debug APK
./gradlew assembleDebug 

# 3. Or install straight onto a connected device / running emulator
./gradlew installDebug
```

## osu!lazer import & export

Settings → **osu!lazer** moves beatmaps between osu!lazer and Kozu using standard `.osz` archives.

- **Import:** pick `.osz` files, scan a whole folder or share beatmaps straight from osu!lazer to Kozu. Several files are queued and imported in the background.
- **Mania charts:** beatmaps with osu!mania difficulties can also be added to the Game library automatically.
- **Duplicates:** sets already in your library are skipped, or replaced if you turn on *Replace existing beatmaps*. Beatmaps that have no online ID get a stable local ID so re-importing never creates copies.
- **Export:** choose beatmaps from your library (and Game charts) and hand them to osu!lazer in batches, share them with any app or save the `.osz` files into a folder.

osu!lazer keeps its database in a realm file that other apps cannot read, so `.osz` is the supported exchange format.

## Contributing

Contributions, bug reports and ideas are welcome.

1. Check the [issues](https://github.com/kanzick/Kozu/issues) to open issues for what you'd like to change or report on.
2. Fork the repo and create a branch: `git checkout -b feature/my-feature`
3. Commit your changes and push the branch.
4. Open a pull request.

Please read the [Code of Conduct](CODE_OF_CONDUCT.md) before participating.

## License

Kozu is licensed under the **GNU General Public License v3.0**. See [LICENSE](https://github.com/kanzick/Kozu?tab=GPL-3.0-1-ov-file).

## Acknowledgements

- [osu!](https://osu.ppy.sh/) for the game and the music community around it
