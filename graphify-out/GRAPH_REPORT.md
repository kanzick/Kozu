# Graph Report - UngDung  (2026-11-03)

## Corpus Check
- 183 files · ~102,707 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 31 file(s) not represented in the graph (top: .xml 18, (none) 4, .properties 3)

## Summary
- 2896 nodes · 8013 edges · 138 communities (88 shown, 50 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 288 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `801a66e6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DownloadsStore
- HomeScreen.kt
- LibraryScreen
- PlaylistStore
- WavySlider
- SettingsComponents.kt
- Oklch.kt
- env
- env
- OsuAuth
- BrowseViewModel
- PlaybackController.kt
- steps
- Setting
- SearchViewModel.kt
- HttpRangeSource
- GameStageHost.kt
- KozuApp.kt
- AppContainer
- SettingsViewModel.kt
- SettingsStore
- DiscoverFeed.kt
- GameCore
- StreamResolver
- SettingsViewModel
- Beatmap
- PlaybackController
- GameStageHost.kt
- StatsPage
- DownloadManager
- SearchViewModel.kt
- DownloadService.kt
- Locals.kt
- ZipAudioDataSource
- JudgementEngine
- PlaybackService.kt
- download/OszExtractor.kt
- ioexception
- SongImporter
- ManiaChartParser
- LibraryViewModel
- Landscape3DPerspective.kt
- body
- body
- SettingsStore.kt
- HistoryStore
- GameViewModel.kt
- KozuDatabase
- KozuTab
- .remember
- PreviewStageModule
- StreamResolver.kt
- SettingsViewModel.kt
- KozuApplication.kt
- GameCore
- GameSession.kt
- ThemeMode
- Contributor Covenant Code of Conduct
- GameStageCoreTest.kt
- FavoritesStore
- ExploreScreen.kt
- HomeScreen
- KozuTheme.kt
- DownloadManager.kt
- PreviewPlayer
- DevicePage.kt
- ModeFilter
- Resolver
- StatusFilter
- GameStageModule
- TouchInputRouter
- BrowseFilters
- ProxyController.kt
- BeatmapInfoSheet
- BeatmapRepository
- PreviewStageScreen.kt
- PlaylistDao
- AppSettings
- SettingsPage
- MainActivity.kt
- Type.kt
- BackupManager.kt
- UpdateChecker.kt
- GameClock
- BrowseLanguage
- SettingCard
- drawPerspectivePlayfield
- BrowseGenre
- ArtworkPalette.kt
- PreviewStageCoreTest.kt
- StoragePage
- context
- .encodeList
- BeatmapRepository.kt
- CrossfadeController
- Landscape3DPerspective/README.md
- AppIcon
- ProxySettings
- BrowseStatus
- build.yml
- DeveloperPage.kt
- KozuDatabase
- BrowseRank
- GameResult
- Network.kt
- LibraryTab
- SettingsRoot
- Shapes.kt
- StartTab
- .applyEqualizer
- alignment
- GameAudio
- AudioEffectsController
- BandsCard
- SettingsPageScaffold
- BackupSection
- BeatmapDifficulty
- SleepTimerSheet
- SettingsSearch
- startFirst
- StorageConfirm
- SleepTimer.kt
- BackupPicker
- recordToolUse.sh script

## God Nodes (most connected - your core abstractions)
1. `Beatmap` - 134 edges
2. `SettingsStore` - 119 edges
3. `Setting` - 99 edges
4. `SettingsViewModel` - 69 edges
5. `BeatmapRepository` - 57 edges
6. `PlaybackController` - 46 edges
7. `AppContainer` - 41 edges
8. `AppSettings` - 40 edges
9. `StreamResolver` - 37 edges
10. `PlaybackService` - 37 edges

## Surprising Connections (you probably didn't know these)
- `toBeatmap()` --references--> `Beatmap`  [EXTRACTED]
  app/src/main/kotlin/vn/kozu/player/PlaybackMediaItem.kt → app/src/main/kotlin/vn/kozu/domain/model/Beatmap.kt
- `GameCore` --calls--> `GameClock`  [INFERRED]
  app/src/main/kotlin/vn/kozu/game/mania/gamestage/engine/GameCore.kt → app/src/main/kotlin/vn/kozu/game/mania/gamestage/engine/GameClock.kt
- `GameCore` --calls--> `JudgementEngine`  [INFERRED]
  app/src/main/kotlin/vn/kozu/game/mania/gamestage/engine/GameCore.kt → app/src/main/kotlin/vn/kozu/game/mania/gamestage/engine/JudgementEngine.kt
- `GameStageHost()` --calls--> `PlayScreen()`  [INFERRED]
  app/src/main/kotlin/vn/kozu/game/mania/gamestage/ui/GameStageHost.kt → app/src/main/kotlin/vn/kozu/game/mania/gamestage/ui/PlayScreen.kt
- `GameStageHost()` --calls--> `SongInfoDialog()`  [INFERRED]
  app/src/main/kotlin/vn/kozu/game/mania/gamestage/ui/GameStageHost.kt → app/src/main/kotlin/vn/kozu/game/mania/gamestage/ui/SongInfoDialog.kt

## Import Cycles
- None detected.

## Communities (138 total, 50 thin omitted)

### Community 1 - "HomeScreen.kt"
Cohesion: 0.05
Nodes (7): BeatmapArt(), bounceClick(), EqualizerBars(), rememberKozuHaptics(), MiniPlayer(), QueueSheet(), KozuMotion

### Community 2 - "LibraryScreen"
Cohesion: 0.26
Nodes (13): DownloadsPane(), formatBytes(), LibraryScreen(), libraryTabIcon(), LibraryTabsSheet(), PillButton(), PlaylistDetail(), PlaylistGrid() (+5 more)

### Community 3 - "PlaylistStore"
Cohesion: 0.14
Nodes (5): PlaylistEntity, SongEntity, SongInPlaylistEntity, PlaylistStore, Playlist

### Community 4 - "WavySlider"
Cohesion: 0.11
Nodes (13): PlaybackState, RepeatState, ALL, OFF, ONE, WavySlider(), PlayerProgress(), PlayerScreen() (+5 more)

### Community 6 - "Oklch.kt"
Cohesion: 0.18
Nodes (3): Lch, Oklch, TonalPalette

### Community 7 - "env"
Cohesion: 0.04
Nodes (44): concurrency, cancel-in-progress, group, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD, ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEYSTORE_PATH (+36 more)

### Community 8 - "env"
Cohesion: 0.05
Nodes (43): default, description, required, type, env, name, runs-on, steps (+35 more)

### Community 9 - "OsuAuth"
Cohesion: 0.06
Nodes (17): AccountStore, OsuSession, OsuApi, OsuAuth, BeatmapsetDto, BeatmapsetSearchResponse, CountryDto, CoverDto (+9 more)

### Community 10 - "BrowseViewModel"
Cohesion: 0.06
Nodes (24): KozuLoading(), RefreshIndicator(), AllBeatmapsScreen(), BrowseFooter(), BrowseRow(), starRange(), ActiveFilterChips(), BrowseSearchField() (+16 more)

### Community 12 - "steps"
Cohesion: 0.17
Nodes (12): name, runs-on, steps, timeout-minutes, jobs, build, name, run (+4 more)

### Community 13 - "Setting"
Cohesion: 0.02
Nodes (87): Setting, ACCENT_COLOR, APP_FONT, APP_ICON, APP_STYLE, APP_THEME, ARTWORK_COLORS, ARTWORK_MOBILE (+79 more)

### Community 14 - "SearchViewModel.kt"
Cohesion: 0.12
Nodes (16): SearchCategory, ANIME, CLASSICAL, ELECTRONIC, FOLK, HIP_HOP, JAZZ, METAL (+8 more)

### Community 15 - "HttpRangeSource"
Cohesion: 0.13
Nodes (8): HttpRangeSource, FilterInputStream, CountingInputStream, RangeSource, RangeTail, RemoteZip, TailRangeSource, ZipEntryInfo

### Community 16 - "GameStageHost.kt"
Cohesion: 0.13
Nodes (10): ManiaChart, GameSession, LeaderboardDialog(), KozuCatalog, DifficultyRecord, SongEntry, ChartSetCard(), DifficultyRow() (+2 more)

### Community 17 - "KozuApp.kt"
Cohesion: 0.06
Nodes (4): compact(), timingLine(), formatDuration(), formatTime()

### Community 18 - "AppContainer"
Cohesion: 0.17
Nodes (4): MirrorTester, OsuApiFactory, CacheCleaner, AppContainer

### Community 19 - "SettingsViewModel.kt"
Cohesion: 0.22
Nodes (4): ListeningStats, HomeUiState, HomeViewModel, dayKey()

### Community 20 - "SettingsStore"
Cohesion: 0.04
Nodes (10): Keys, SettingsStore, CrossfadeDuration, EXTRA_LONG, LONG, MEDIUM, OFF, SHORT (+2 more)

### Community 21 - "DiscoverFeed.kt"
Cohesion: 0.20
Nodes (3): DiscoverFeed, DiscoverState, isRankedOrLoved()

### Community 22 - "GameCore"
Cohesion: 0.17
Nodes (4): GameCore, NoteScroll, noteTimingEdgeAndJudgmentLineCoincideAtZeroErrorInAllScrollModes(), scrollSpeedMapping()

### Community 25 - "SettingsViewModel"
Cohesion: 0.06
Nodes (11): Checking, Failure, Idle, SettingsViewModel, StatsRange, MONTH, QUARTER, WEEK (+3 more)

### Community 26 - "Beatmap"
Cohesion: 0.10
Nodes (11): Beatmap, AutoplayQueue, QueueDedupe, TrackActions, NamePlaylistDialog(), PlaylistPickerSheet(), SheetAction(), SheetTrackHeader() (+3 more)

### Community 28 - "GameStageHost.kt"
Cohesion: 0.13
Nodes (8): SubmitOutcome, findActivity(), GameStageHost(), Selection, Line(), ResultData, ResultScreen(), GameScreen()

### Community 29 - "StatsPage"
Cohesion: 0.10
Nodes (25): AccountPage(), GradeTile(), OAuthCredentialsDialog(), ProfileChip(), ProfileHeader(), ProfileStats(), OfflinePage(), ActionRow() (+17 more)

### Community 30 - "DownloadManager"
Cohesion: 0.14
Nodes (7): DownloadManager, DownloadStatus, Failed, Queued, Running, Source, NetworkInfo

### Community 31 - "SearchViewModel.kt"
Cohesion: 0.15
Nodes (6): SearchPhase, DONE, IDLE, LOADING, SearchUiState, SearchViewModel

### Community 33 - "Locals.kt"
Cohesion: 0.23
Nodes (3): DownloadInfo, KozuHaptics, NowPlayingInfo

### Community 35 - "JudgementEngine"
Cohesion: 0.09
Nodes (15): ChartNote, NoteSample, JudgementEngine, Listener, HitWindows, Judgement, GOOD, GREAT (+7 more)

### Community 38 - "download/OszExtractor.kt"
Cohesion: 0.19
Nodes (4): ExtractResult, OszExtractor, OsuBeatmapParser, OsuDifficulty

### Community 40 - "SongImporter"
Cohesion: 0.12
Nodes (9): ImportFailure, FileResolver, OsuBeatmap, OsuParser, Imported, ImportSummary, Outcome, Skipped (+1 more)

### Community 41 - "ManiaChartParser"
Cohesion: 0.29
Nodes (4): ChartParseException, ManiaChartParser, RawNote, Timing

### Community 42 - "LibraryViewModel"
Cohesion: 0.09
Nodes (6): LibrarySort, ARTIST, MAPPER, RECENT, TITLE, LibraryViewModel

### Community 43 - "Landscape3DPerspective.kt"
Cohesion: 0.12
Nodes (11): drawLandscapeVerticalScrolling(), drawPortraitPlayfield(), drawJudgmentXLine(), PlayColors, PlayfieldLayout, ErrorHitMeter(), findActivity(), Hud() (+3 more)

### Community 44 - "body"
Cohesion: 0.12
Nodes (15): description, label, options, placeholder, body, attributes, id, type (+7 more)

### Community 45 - "body"
Cohesion: 0.12
Nodes (15): description, label, options, placeholder, body, attributes, id, type (+7 more)

### Community 46 - "SettingsStore.kt"
Cohesion: 0.04
Nodes (32): AccentColor, BLUE, GREEN, ORANGE, PINK, PURPLE, RED, TEAL (+24 more)

### Community 47 - "HistoryStore"
Cohesion: 0.25
Nodes (3): History, HistoryStore, PlayEvent

### Community 48 - "GameViewModel.kt"
Cohesion: 0.16
Nodes (3): PreviewStageConfig, GameMapDownloadState, GameViewModel

### Community 50 - "KozuTab"
Cohesion: 0.22
Nodes (13): toKozuTab(), ClassicItem(), ClassicNavBar(), FloatingNavBar(), PillItem(), PillNavBar(), SearchButton(), KozuTab (+5 more)

### Community 51 - ".remember"
Cohesion: 0.10
Nodes (24): AboutPage(), shareApp(), BackupOutcome, BackupPage(), HowItWorksCard(), OutcomeCard(), SectionSheet(), DeveloperPage() (+16 more)

### Community 54 - "StreamResolver.kt"
Cohesion: 0.11
Nodes (5): MirrorTestResult, ArchiveAudioSession, ZipCandidate, Mirror, Mirrors

### Community 55 - "SettingsViewModel.kt"
Cohesion: 0.11
Nodes (11): CatalogSource, MIXED, NONE, OFFICIAL_OSU, PUBLIC_MIRROR, ExploreUiState, ExploreViewModel, isRankedOrLoved() (+3 more)

### Community 57 - "GameCore"
Cohesion: 0.12
Nodes (16): Landscape3DPerspective, LandscapePlayTypeOption(), LandscapePlayTypeOptions(), LandscapeVerticalScrolling, PlayType, Portrait, GameOrientation, LANDSCAPE (+8 more)

### Community 58 - "GameSession.kt"
Cohesion: 0.07
Nodes (10): HitSoundPlayer, SampleLookup, BackgroundLoader, Phase, FAILED, FINISHED, LOADING, PAUSED (+2 more)

### Community 59 - "ThemeMode"
Cohesion: 0.25
Nodes (4): ThemeMode, DARK, LIGHT, SYSTEM

### Community 60 - "Contributor Covenant Code of Conduct"
Cohesion: 0.15
Nodes (12): 1. Correction, 2. Warning, 3. Temporary Ban, 4. Permanent Ban, Attribution, Contributor Covenant Code of Conduct, Enforcement, Enforcement Guidelines (+4 more)

### Community 61 - "GameStageCoreTest.kt"
Cohesion: 0.16
Nodes (34): accuracyWeights(), allGreatScoresBaseOnly(), allPerfectIsExactlyOneMillionAndSS(), chartText(), eachChartDifficultyUsesItsOwnOverallDifficultyForHitWindows(), earlyAndLateEdgeCases(), engineFor(), heldPastTailDeadlineIsJudgedByDeadline() (+26 more)

### Community 63 - "ExploreScreen.kt"
Cohesion: 0.09
Nodes (9): displayArtist(), displayTitle(), MosaicCover(), CoverCard(), TrackPill(), DownloadBadge(), TrackRow(), CollectionCard() (+1 more)

### Community 64 - "HomeScreen"
Cohesion: 0.19
Nodes (14): CatalogErrorBanner(), compactOsuCount(), formatOsuPlayTime(), Greeting(), HomeScreen(), ListeningStatsCard(), OsuPulseCard(), OsuRankHistoryChart() (+6 more)

### Community 65 - "KozuTheme.kt"
Cohesion: 0.08
Nodes (16): AppFont, KOZU, SYSTEM, AppStyle, CLASSIC, EXPRESSIVE, PaletteStyle, EXPRESSIVE (+8 more)

### Community 66 - "DownloadManager.kt"
Cohesion: 0.10
Nodes (3): LibrarySync, SyncReport, PlaybackEvents

### Community 68 - "DevicePage.kt"
Cohesion: 0.10
Nodes (15): appEntries(), decoderEntries(), DeviceEntry, deviceName(), devicePriority(), DeviceReport, directorySize(), isHardware() (+7 more)

### Community 69 - "ModeFilter"
Cohesion: 0.22
Nodes (6): ModeFilter, ANY, CATCH, MANIA, OSU, TAIKO

### Community 71 - "StatusFilter"
Cohesion: 0.22
Nodes (5): StatusFilter, ANY, DEFAULT, LOVED, RANKED

### Community 72 - "GameStageModule"
Cohesion: 0.19
Nodes (4): DefaultHitsound, GameStageModule, ScoreRecord, ScoreDatabase

### Community 73 - "TouchInputRouter"
Cohesion: 0.17
Nodes (3): Changes, TouchInputRouter, TouchInputRouterTest

### Community 74 - "BrowseFilters"
Cohesion: 0.08
Nodes (26): ActiveBrowseFilter, BrowseExtra, STORYBOARD, VIDEO, BrowseFilters, BrowseGeneral, CONVERTS, FEATURED_ARTISTS (+18 more)

### Community 75 - "ProxyController.kt"
Cohesion: 0.11
Nodes (6): Failure, ProxyController, ProxySelector, ProxyTestResult, SocksAuthenticator, Success

### Community 76 - "BeatmapInfoSheet"
Cohesion: 0.10
Nodes (15): CachedSetInfo, BeatmapDifficultyInfo, BeatmapSetInfo, GameModes, BeatmapInfoSheet(), countLine(), DifficultyRow(), formatCount() (+7 more)

### Community 77 - "BeatmapRepository"
Cohesion: 0.18
Nodes (3): BeatmapRepository, CatalogResult, StatusPageCursor

### Community 79 - "PreviewStageScreen.kt"
Cohesion: 0.17
Nodes (7): AddMapCard(), AddMapScreen(), DifficultyInfoRow(), MapPagingSnapshot, EmptyState(), SectionHeader(), segmentedShape()

### Community 82 - "SettingsPage"
Cohesion: 0.10
Nodes (19): SettingsPage, ABOUT, ACCOUNT, APPEARANCE, BACKUP, BEHAVIOR, CONTENT, DEVELOPER (+11 more)

### Community 83 - "MainActivity.kt"
Cohesion: 0.16
Nodes (3): MainActivity, KozuApp(), TabHost()

### Community 84 - "Type.kt"
Cohesion: 0.23
Nodes (3): buildTypography(), kozu(), quicksand()

### Community 85 - "BackupManager.kt"
Cohesion: 0.25
Nodes (4): BackupException, BackupInfo, BackupManager, readLimited()

### Community 86 - "UpdateChecker.kt"
Cohesion: 0.21
Nodes (7): Available, checkIfDue(), Failed, ParsedVersion, UpdateChecker, UpdateResult, UpToDate

### Community 92 - "GameClock"
Cohesion: 0.21
Nodes (5): AudioAnchor, GameClock, clockGivesUpOnAudio(), clockRunsFreeThenHoldsAtZeroUntilAudio(), FakeNano

### Community 93 - "BrowseLanguage"
Cohesion: 0.12
Nodes (16): BrowseLanguage, ANY, CHINESE, ENGLISH, FRENCH, GERMAN, INSTRUMENTAL, ITALIAN (+8 more)

### Community 94 - "SettingCard"
Cohesion: 0.23
Nodes (8): FeatureChip(), HeroCard(), LinkItem(), DropdownItem(), IconBadge(), ItemText(), SettingCard(), SwitchItem()

### Community 95 - "drawPerspectivePlayfield"
Cohesion: 0.30
Nodes (3): drawPerspectivePlayfield(), LandscapeProjection, progressAt()

### Community 96 - "BrowseGenre"
Cohesion: 0.13
Nodes (15): BrowseGenre, ANIME, ANY, CLASSICAL, ELECTRONIC, FOLK, HIP_HOP, JAZZ (+7 more)

### Community 99 - "StoragePage"
Cohesion: 0.21
Nodes (7): ConfirmDialog(), ForgottenRow(), ForgottenSheet(), Legend(), ReclaimCard(), sizeLabel(), StoragePage()

### Community 105 - "AppIcon"
Cohesion: 0.24
Nodes (5): AppIcon, DEFAULT, MIDNIGHT, MINT, VIOLET

### Community 106 - "ProxySettings"
Cohesion: 0.18
Nodes (7): ProxySettings, ProxyType, DISABLED, HTTP, SOCKS5, ProxySettingsDialog(), proxySummary()

### Community 108 - "BrowseStatus"
Cohesion: 0.18
Nodes (10): BrowseStatus, ANY, FAVOURITES, GRAVEYARD, HAS_LEADERBOARD, LOVED, MINE, PENDING (+2 more)

### Community 109 - "build.yml"
Cohesion: 0.18
Nodes (10): concurrency, cancel-in-progress, group, name, on, push, workflow_dispatch, permissions (+2 more)

### Community 111 - "DeveloperPage.kt"
Cohesion: 0.24
Nodes (5): buildNumber(), debugReport(), DeveloperConfirm, CACHES, RESET

### Community 113 - "BrowseRank"
Cohesion: 0.22
Nodes (9): BrowseRank, A, B, C, D, S, SH, X (+1 more)

### Community 116 - "LibraryTab"
Cohesion: 0.25
Nodes (7): libraryTabCount(), LibraryTab, DOWNLOADS, LIKED, PLAYLISTS, SONGS, LibraryUiState

### Community 117 - "SettingsRoot"
Cohesion: 0.33
Nodes (7): SettingsSectionTitle(), AccountSummaryCard(), CategoryRow(), SearchEntryBar(), SearchHint(), SettingsRoot(), SettingsSearchOverlay()

### Community 119 - "StartTab"
Cohesion: 0.25
Nodes (5): StartTab, EXPLORE, GAME, HOME, LIBRARY

### Community 124 - "BandsCard"
Cohesion: 0.38
Nodes (4): Band(), BandsCard(), frequencyLabel(), PresetCard()

### Community 125 - "SettingsPageScaffold"
Cohesion: 0.29
Nodes (3): CollapsingHeader(), SettingsPageScaffold(), NestedScrollConnection

### Community 126 - "BackupSection"
Cohesion: 0.40
Nodes (5): BackupSection, FAVORITES, HISTORY, PLAYLISTS, SETTINGS

### Community 129 - "SleepTimerSheet"
Cohesion: 0.50
Nodes (3): formatRemaining(), SleepTimerSheet(), TimerChip()

### Community 131 - "startFirst"
Cohesion: 0.60
Nodes (3): openBatterySettings(), openDefaultLinkSettings(), startFirst()

### Community 132 - "StorageConfirm"
Cohesion: 0.50
Nodes (4): StorageConfirm, CLEAN_ALL, FORGOTTEN, HISTORY

### Community 134 - "BackupPicker"
Cohesion: 0.67
Nodes (3): BackupPicker, Create, Restore

## Knowledge Gaps
- **294 isolated node(s):** `SETTINGS`, `FAVORITES`, `PLAYLISTS`, `HISTORY`, `Queued` (+289 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 846 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **50 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Beatmap` connect `Beatmap` to `DownloadsStore`, `HomeScreen.kt`, `LibraryScreen`, `PlaylistStore`, `WavySlider`, `BrowseViewModel`, `PlaybackController.kt`, `GameStageHost.kt`, `KozuApp.kt`, `SettingsViewModel.kt`, `DiscoverFeed.kt`, `SettingsViewModel`, `PlaybackController`, `DownloadManager`, `SearchViewModel.kt`, `Locals.kt`, `LibraryScreen.kt`, `LibraryViewModel`, `HistoryStore`, `GameViewModel.kt`, `SettingsViewModel.kt`, `FavoritesStore`, `ExploreScreen.kt`, `HomeScreen`, `DownloadManager.kt`, `BeatmapInfoSheet`, `BeatmapRepository`, `PreviewStageScreen.kt`, `MainActivity.kt`, `BackupManager.kt`, `context`, `.encodeList`, `BeatmapRepository.kt`, `.parseDetail`, `LibraryTab`, `BeatmapDifficulty`?**
  _High betweenness centrality (0.105) - this node is a cross-community bridge._
- **What connects `SETTINGS`, `FAVORITES`, `PLAYLISTS` to the rest of the system?**
  _294 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `HomeScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.04835164835164835 - nodes in this community are weakly interconnected._
- **Why does `SettingsStore` connect `SettingsStore` to `AppContainer`, `DiscoverFeed.kt`, `ioexception`, `SettingsStore.kt`, `.remember`, `StreamResolver.kt`, `ThemeMode`, `KozuTheme.kt`, `DownloadManager.kt`, `ModeFilter`, `StatusFilter`, `AppSettings`, `MainActivity.kt`, `BackupManager.kt`, `UpdateChecker.kt`, `StoragePage`, `BeatmapRepository.kt`, `ProxySettings`, `StartTab`?**
  _High betweenness centrality (0.058) - this node is a cross-community bridge._
- **Should `PlaylistStore` be split into smaller, more focused modules?**
  _Cohesion score 0.14245014245014245 - nodes in this community are weakly interconnected._
- **Why does `AppContainer` connect `AppContainer` to `DownloadsStore`, `PlaylistStore`, `OsuAuth`, `BrowseViewModel`, `KozuApp.kt`, `SettingsViewModel.kt`, `SettingsStore`, `DiscoverFeed.kt`, `StreamResolver`, `SettingsViewModel`, `Beatmap`, `PlaybackController`, `DownloadManager`, `SearchViewModel.kt`, `LibraryViewModel`, `HistoryStore`, `GameViewModel.kt`, `SettingsViewModel.kt`, `KozuApplication.kt`, `FavoritesStore`, `DownloadManager.kt`, `BeatmapRepository`, `BackupManager.kt`, `UpdateChecker.kt`?**
  _High betweenness centrality (0.042) - this node is a cross-community bridge._
- **Should `WavySlider` be split into smaller, more focused modules?**
  _Cohesion score 0.1111111111111111 - nodes in this community are weakly interconnected._