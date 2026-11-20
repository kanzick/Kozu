<div align="center">

  <p>
    <a href="README.md"><img src="https://img.shields.io/badge/🇬🇧%20English-ff66aa?style=for-the-badge&labelColor=21182e" alt="English" /></a>
    <a href="README_VI.md"><img src="https://img.shields.io/badge/🇻🇳%20Ti%E1%BA%BFng%20Vi%E1%BB%87t-3fa9f5?style=for-the-badge&labelColor=21182e" alt="Tiếng Việt" /></a>
  </p>

  <p><strong>Simple music app for osu! built with Kotlin.</strong></p>
  <p>Listen your favourite songs from osu! without launching the base game. It aims to be small, fast and pleasant to use.</p>

  <p>
    <a href="https://github.com/kanzick/Kozu/releases/latest"><strong>Download</strong></a> ·
    <a href="#features"><strong>Features</strong></a> ·
    <a href="#osu-lazer-bridge"><strong>osu!lazer</strong></a> ·
    <a href="#build-from-source"><strong>Build</strong></a> ·
    <a href="https://github.com/kanzick/Kozu/issues"><strong>Support</strong></a>
  </p>

  <p>
    <a href="https://github.com/kanzick/Kozu/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/kanzick/Kozu/build.yml?branch=main&style=for-the-badge&label=build&logo=githubactions&logoColor=white&labelColor=21182e" alt="Build status" /></a>
    <a href="https://github.com/kanzick/Kozu/releases"><img src="https://img.shields.io/github/v/release/kanzick/Kozu?include_prereleases&style=for-the-badge&color=ff66aa&labelColor=21182e&logo=github" alt="Latest release" /></a>
    <a href="https://github.com/kanzick/Kozu/releases"><img src="https://img.shields.io/github/downloads/kanzick/Kozu/total?style=for-the-badge&color=ff8a4c&labelColor=21182e&logo=android" alt="Downloads" /></a>
    <a href="https://github.com/kanzick/Kozu/stargazers"><img src="https://img.shields.io/github/stars/kanzick/Kozu?style=for-the-badge&color=3fa9f5&labelColor=21182e&logo=github" alt="GitHub stars" /></a>
    <a href="LICENSE"><img src="https://img.shields.io/github/license/kanzick/Kozu?style=for-the-badge&color=a58cff&labelColor=21182e" alt="GPL-3.0 license" /></a>
  </p>

  <p>
    <img src="https://img.shields.io/badge/Android-9%2B-3ddc84?style=flat-square&logo=android&logoColor=white" alt="Android 9 and newer" />
    <img src="https://img.shields.io/badge/Kotlin-2.4.21-7f52ff?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285f4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose and Material 3" />
  </p>
</div>

---

## Features

<table>
  <tr>
    <td width="50%" valign="top">
      <h3>🎼 Discover</h3>
      <ul>
        <li>Search the osu! beatmap catalogue by title, artist, or mapper.</li>
        <li>Explore Ranked, Loved, trending, new releases, and spotlight maps.</li>
        <li>Browse across game modes and narrow results with filters.</li>
        <li>Open beatmap sets to inspect their difficulties and details.</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>🎧 Listen</h3>
      <ul>
        <li>Choose full-song playback through enabled public mirrors, or preview-only playback.</li>
        <li>Keep music playing in the background with a persistent queue.</li>
        <li>Use shuffle, repeat, autoplay, queue history, and crossfade.</li>
        <li>Shape the sound with the equalizer, presets, and loudness controls.</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>🗂️ Your library</h3>
      <ul>
        <li>Keep liked songs, playlists, downloads, and listening history together.</li>
        <li>Sort and reorder tracks to make the library feel like yours.</li>
        <li>Download songs for offline listening and for use in Game mode.</li>
        <li>See listening trends, top tracks, and weekly activity.</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>🎮 Mania mode</h3>
      <ul>
        <li>Play local osu!mania charts from downloaded or imported beatmaps.</li>
        <li>Choose difficulties and adjust gameplay settings.</li>
        <li>Keep local scores, personal bests, and play history.</li>
        <li>Import Mania charts alongside music from osu!lazer.</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>🔄 osu!lazer</h3>
      <ul>
        <li>Import one or more <code>.osz</code> archives, scan folders, or share files to Kozu.</li>
        <li>Read an osu!lazer library from a selected data folder or through Shizuku on supported devices.</li>
        <li>Turn osu!lazer collections into Kozu playlists.</li>
        <li>Export selected beatmaps back to osu!lazer, or save and share them as <code>.osz</code>.</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>🎨 Make it yours</h3>
      <ul>
        <li>Use light, dark, system, or AMOLED-friendly themes.</li>
        <li>Let player colors adapt to the current artwork.</li>
        <li>Customize player layouts, typography, density, and appearance.</li>
        <li>Switch between English and Vietnamese.</li>
      </ul>
    </td>
  </tr>
</table>

Kozu also includes osu! news, optional profile integration, configurable audio sources, storage tools, and selective backup and restore.

## Audio sources, clearly explained

Kozu offers two playback modes:

- **Full song** tries the public mirror sources you have enabled and plays a full audio file when one is available. It does not silently fall back to a short preview.
- **Preview only** uses the beatmap preview source.

Beatmap listings, metadata, covers, and previews are obtained from osu!. Optional third-party services may provide full-track streaming and downloads. **Kozu does not host beatmaps, audio, or artwork**; availability and terms are controlled by their respective providers, and the content remains owned by its creators.

> [!TIP]
> You do not need an osu! account to browse the catalogue, stream, or download. Sign in when you want to connect your osu! profile and sync personal library information.

<a id="osu!lazer"></a>
## osu!lazer

Move beatmaps between Kozu and osu!lazer without rebuilding your collection by hand.

| Into Kozu | Out of Kozu |
|---|---|
| Import `.osz` files, multiple files, or an entire folder. | Send selected beatmaps to osu!lazer. |
| Share exported beatmaps directly from osu!lazer. | Save or share standard `.osz` archives. |
| Read the lazer library from its data folder, or use Shizuku where available. | Include downloaded Game charts in an export. |
| Optionally create Kozu playlists from lazer collections. | |

For direct library reading, close osu!lazer first so its database is not in use. Kozu reads a copy and does not modify lazer's data.

## Download

<a href="https://github.com/kanzick/Kozu/releases/latest"><img src="https://img.shields.io/badge/Get%20Kozu-GitHub%20Releases-ff66aa?style=for-the-badge&logo=github&logoColor=white&labelColor=21182e" alt="Download Kozu from GitHub Releases" /></a>

Download the APK that matches your device from [GitHub Releases](https://github.com/kanzick/Kozu/releases). Release builds are published in these variants:

| APK | For |
|---|---|
| `kozu-universal.apk` | Most straightforward choice if you are unsure. |
| `kozu-arm64-v8a.apk` | Most modern Android phones and tablets. |
| `kozu-armeabi-v7a.apk` | 32-bit ARM devices. |
| `kozu-x86.apk` / `kozu-x86_64.apk` | Compatible x86 devices and emulators. |

**Minimum Android version:** Android 9 (API 28).

## Build from source

**Requirements**

- Android Studio with an Android SDK installed.
- JDK 17 or newer.
- An Android 9+ device or emulator to run the app.

```bash
git clone https://github.com/kanzick/Kozu.git
cd Kozu

# Build a debug APK
./gradlew assembleDebug

# Or install it on a connected device / running emulator
./gradlew installDebug
```

## Account, privacy, and backups

- **Guest access:** browsing, streaming, and downloads are available without signing in.
- **Optional osu! sign-in:** connect a profile to show account information and sync personal data.
- **Selective backups:** choose settings, favorites, playlists, listening history, downloaded music, and game data.
- **Credentials stay out of backups:** sign-in tokens, API credentials, and proxy passwords are not included in Kozu backup files.

## Support and contributing

Found a bug or have an idea? Open an [issue](https://github.com/kanzick/Kozu/issues). Contributions and pull requests are welcome. Please read the [Code of Conduct](CODE_OF_CONDUCT.md) before participating.

## Credits and legal

Kozu is an independent fan project. It is not affiliated with, endorsed by, or sponsored by osu! or ppy.

osu! is the game and catalogue that make this project possible. Beatmap audio and artwork remain the property of their respective creators. Please respect the terms of the services and content sources you use.

## License

Kozu is open source under the **GNU General Public License v3.0 (GPL-3.0)**. See [`LICENSE`](LICENSE) for the full license text.

---

<div align="center">
  <strong>Found your next favourite track?</strong><br />
  <a href="https://github.com/kanzick/Kozu/stargazers">Leave Kozu a ⭐ on GitHub</a> ·
  <a href="https://github.com/kanzick/Kozu/releases">Get the latest build</a>
</div>
