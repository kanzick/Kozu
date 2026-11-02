# Graph Report - UngDung  (2026-11-02)

## Corpus Check
- 145 files · ~70,081 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 24 file(s) not represented in the graph (top: .xml 12, (none) 3, .properties 3)

## Summary
- 1902 nodes · 5045 edges · 93 communities (59 shown, 34 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 104 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5c769b7e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DownloadsStore
- PlayScreen.kt
- LibraryScreen.kt
- PlaylistStore
- WavySlider
- AccountPage.kt
- KozuTheme.kt
- env
- env
- BeatmapRepository
- SearchScreen.kt
- PlaybackController.kt
- MainActivity.kt
- TrackRow.kt
- SearchViewModel.kt
- HttpRangeSource
- GameStageHost.kt
- KozuApp.kt
- .create
- SettingsViewModel.kt
- SettingsStore
- DiscoverFeed.kt
- GameCore
- AccountRepository
- StreamResolver
- SettingsViewModel
- KozuShell
- PlaybackController
- GameStageHost.kt
- StatsPage
- DownloadManager
- SearchViewModel
- DownloadService.kt
- Locals.kt
- ZipAudioDataSource
- JudgementEngine
- PlaybackService.kt
- PlayScreen.kt
- download/OszExtractor.kt
- OsuAuth
- SongImporter
- ManiaChartParser
- LibraryViewModel
- Landscape3DPerspective.kt
- body
- body
- AccentColor
- HistoryStore
- GameViewModel.kt
- KozuDatabase
- KozuTab
- SettingsScreen
- ManiaChart
- PreviewStageModule
- StreamResolver.kt
- ExploreViewModel.kt
- AppContainer
- GameCore
- GameSession.kt
- SettingsStore.kt
- Contributor Covenant Code of Conduct
- AppContainer.kt
- HistoryStore.kt
- HomeScreen.kt
- HomeScreen
- AccountPage
- GameClock
- SettingsPage
- ModeFilter
- KozuRoutingDataSource
- StatusFilter
- GameStageModule
- TouchInputRouter
- StreamMode
- NoteScroll
- .remember
- PlaylistPickerSheet
- GameScreen
- VerifyState
- Judgement
- StatsPage
- GameSession
- drawPerspectivePlayfield
- osu/OszExtractor.kt
- Landscape3DPerspective/README.md

## God Nodes (most connected - your core abstractions)
1. `Beatmap` - 110 edges
2. `SettingsViewModel` - 54 edges
3. `SettingsStore` - 45 edges
4. `BeatmapRepository` - 38 edges
5. `AppContainer` - 36 edges
6. `StreamResolver` - 35 edges
7. `PlaybackController` - 32 edges
8. `SongEntry` - 31 edges
9. `DifficultyRecord` - 30 edges
10. `DownloadManager` - 29 edges

## Surprising Connections (you probably didn't know these)
- `GameCore` --calls--> `GameClock`  [INFERRED]
  app/src/main/kotlin/vn/kozu/game/mania/gamestage/engine/GameCore.kt → app/src/main/kotlin/vn/kozu/game/mania/gamestage/engine/GameClock.kt
- `GameCore` --calls--> `JudgementEngine`  [INFERRED]
  app/src/main/kotlin/vn/kozu/game/mania/gamestage/engine/GameCore.kt → app/src/main/kotlin/vn/kozu/game/mania/gamestage/engine/JudgementEngine.kt
- `GameStageHost()` --calls--> `PlayScreen()`  [INFERRED]
  app/src/main/kotlin/vn/kozu/game/mania/gamestage/ui/GameStageHost.kt → app/src/main/kotlin/vn/kozu/game/mania/gamestage/ui/PlayScreen.kt
- `GameStageHost()` --calls--> `SongInfoDialog()`  [INFERRED]
  app/src/main/kotlin/vn/kozu/game/mania/gamestage/ui/GameStageHost.kt → app/src/main/kotlin/vn/kozu/game/mania/gamestage/ui/SongInfoDialog.kt
- `SongInfoDialog()` --calls--> `LeaderboardDialog()`  [INFERRED]
  app/src/main/kotlin/vn/kozu/game/mania/gamestage/ui/SongInfoDialog.kt → app/src/main/kotlin/vn/kozu/game/mania/gamestage/ui/LeaderboardDialog.kt

## Import Cycles
- None detected.

## Communities (93 total, 34 thin omitted)

### Community 2 - "LibraryScreen.kt"
Cohesion: 0.13
Nodes (15): Playlist, EmptyState(), SectionHeader(), segmentedShape(), NamePlaylistDialog(), DownloadsPane(), formatBytes(), LibraryScreen() (+7 more)

### Community 3 - "PlaylistStore"
Cohesion: 0.07
Nodes (7): KozuDatabase, PlaylistDao, PlaylistRow, PlaylistEntity, SongEntity, SongInPlaylistEntity, PlaylistStore

### Community 4 - "WavySlider"
Cohesion: 0.06
Nodes (16): PlaybackState, RepeatState, ALL, OFF, ONE, EqualizerBars(), WavySlider(), PlayerProgress() (+8 more)

### Community 5 - "AccountPage.kt"
Cohesion: 0.09
Nodes (3): LandscapeVerticalScrolling, AddMapScreen(), MapPagingSnapshot

### Community 6 - "KozuTheme.kt"
Cohesion: 0.06
Nodes (9): kozuColorScheme(), lerpColorScheme(), KozuTheme(), rememberAnimatedColorScheme(), Lch, Oklch, TonalPalette, CookieShape (+1 more)

### Community 7 - "env"
Cohesion: 0.04
Nodes (44): concurrency, cancel-in-progress, group, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD, ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEYSTORE_PATH (+36 more)

### Community 8 - "env"
Cohesion: 0.05
Nodes (43): default, description, required, type, env, name, runs-on, steps (+35 more)

### Community 9 - "BeatmapRepository"
Cohesion: 0.06
Nodes (18): OsuApi, BeatmapsetDto, BeatmapsetSearchResponse, CountryDto, CoverDto, GradeCountsDto, LevelDto, OsuBeatmapDto (+10 more)

### Community 11 - "PlaybackController.kt"
Cohesion: 0.10
Nodes (3): dayKey(), formatDuration(), formatTime()

### Community 12 - "MainActivity.kt"
Cohesion: 0.07
Nodes (23): MainActivity, name, runs-on, steps, timeout-minutes, concurrency, cancel-in-progress, group (+15 more)

### Community 14 - "SearchViewModel.kt"
Cohesion: 0.10
Nodes (20): SearchCategory, ANIME, CLASSICAL, ELECTRONIC, FOLK, HIP_HOP, JAZZ, METAL (+12 more)

### Community 15 - "HttpRangeSource"
Cohesion: 0.13
Nodes (8): HttpRangeSource, FilterInputStream, CountingInputStream, RangeSource, RangeTail, RemoteZip, TailRangeSource, ZipEntryInfo

### Community 16 - "GameStageHost.kt"
Cohesion: 0.14
Nodes (8): LeaderboardDialog(), KozuCatalog, DifficultyRecord, SongEntry, ChartSetCard(), DifficultyRow(), fmt(), PreviewStageScreen()

### Community 17 - "KozuApp.kt"
Cohesion: 0.08
Nodes (3): KozuApp(), TabHost(), ArtworkPalette

### Community 19 - "SettingsViewModel.kt"
Cohesion: 0.18
Nodes (3): ListeningStats, HomeUiState, HomeViewModel

### Community 21 - "DiscoverFeed.kt"
Cohesion: 0.16
Nodes (3): DiscoverFeed, DiscoverState, isRankedOrLoved()

### Community 23 - "AccountRepository"
Cohesion: 0.14
Nodes (4): AccountStore, OsuSession, AccountRepository, OsuProfile

### Community 25 - "SettingsViewModel"
Cohesion: 0.07
Nodes (6): SettingsViewModel, StatsRange, MONTH, QUARTER, WEEK, StatsUi

### Community 26 - "KozuShell"
Cohesion: 0.14
Nodes (5): Beatmap, TrackActions, KozuShell(), TrackActions, MiniPlayer()

### Community 28 - "GameStageHost.kt"
Cohesion: 0.17
Nodes (7): GameResult, findActivity(), GameStageHost(), Selection, Line(), ResultData, ResultScreen()

### Community 29 - "StatsPage"
Cohesion: 0.18
Nodes (16): OfflinePage(), ActionRow(), ChoiceRow(), formatBytes(), LocalContentColorAlpha(), PrimaryPill(), SecondaryPill(), SettingsEntry() (+8 more)

### Community 30 - "DownloadManager"
Cohesion: 0.19
Nodes (6): DownloadManager, DownloadStatus, Failed, Queued, Running, Source

### Community 31 - "SearchViewModel"
Cohesion: 0.17
Nodes (6): BrowseAll(), FilterRow(), RecentSearches(), SearchField(), SearchScreen(), SearchViewModel

### Community 38 - "download/OszExtractor.kt"
Cohesion: 0.19
Nodes (4): ExtractResult, OszExtractor, OsuBeatmapParser, OsuDifficulty

### Community 40 - "SongImporter"
Cohesion: 0.12
Nodes (9): ImportFailure, FileResolver, OsuBeatmap, OsuParser, Imported, ImportSummary, Outcome, Skipped (+1 more)

### Community 41 - "ManiaChartParser"
Cohesion: 0.32
Nodes (4): ChartParseException, ManiaChartParser, RawNote, Timing

### Community 42 - "LibraryViewModel"
Cohesion: 0.10
Nodes (11): LibrarySort, ARTIST, RECENT, TITLE, LibraryTab, DOWNLOADS, LIKED, PLAYLISTS (+3 more)

### Community 43 - "Landscape3DPerspective.kt"
Cohesion: 0.14
Nodes (4): progressAt(), drawLandscapeVerticalScrolling(), drawPortraitPlayfield(), PlayfieldLayout

### Community 44 - "body"
Cohesion: 0.12
Nodes (15): description, label, options, placeholder, body, attributes, id, type (+7 more)

### Community 45 - "body"
Cohesion: 0.12
Nodes (15): description, label, options, placeholder, body, attributes, id, type (+7 more)

### Community 46 - "AccentColor"
Cohesion: 0.15
Nodes (10): AccentColor, BLUE, GREEN, ORANGE, PINK, PURPLE, RED, TEAL (+2 more)

### Community 47 - "HistoryStore"
Cohesion: 0.36
Nodes (3): History, HistoryStore, PlayEvent

### Community 50 - "KozuTab"
Cohesion: 0.17
Nodes (7): FloatingNavBar(), NavItem(), KozuTab, EXPLORE, GAME, HOME, LIBRARY

### Community 51 - "SettingsScreen"
Cohesion: 0.23
Nodes (9): Entry, EntryGroup(), SettingsRoot(), SettingsScreen(), SettingsSectionLabel(), Tone, PRIMARY, SECONDARY (+1 more)

### Community 52 - "ManiaChart"
Cohesion: 0.29
Nodes (3): ChartNote, ManiaChart, NoteSample

### Community 53 - "PreviewStageModule"
Cohesion: 0.14
Nodes (4): StartupReport, PreviewStageConfig, PreviewStageModule, ModulePaths

### Community 54 - "StreamResolver.kt"
Cohesion: 0.10
Nodes (5): ArchiveAudioSession, ZipCandidate, Mirror, Mirrors, NetworkInfo

### Community 55 - "ExploreViewModel.kt"
Cohesion: 0.24
Nodes (9): CatalogSource, MIXED, NONE, OFFICIAL_OSU, PUBLIC_MIRROR, ExploreUiState, ExploreViewModel, isRankedOrLoved() (+1 more)

### Community 56 - "AppContainer"
Cohesion: 0.31
Nodes (4): AppContainer, KozuApplication, container(), containerFactory()

### Community 57 - "GameCore"
Cohesion: 0.13
Nodes (12): Landscape3DPerspective, LandscapePlayTypeOption(), LandscapePlayTypeOptions(), PlayType, Portrait, GameSettings, SettingsStore, fmt1() (+4 more)

### Community 58 - "GameSession.kt"
Cohesion: 0.07
Nodes (9): SampleLookup, BackgroundLoader, Phase, FAILED, FINISHED, LOADING, PAUSED, PLAYING (+1 more)

### Community 59 - "SettingsStore.kt"
Cohesion: 0.12
Nodes (5): MirrorTestResult, ThemeMode, DARK, LIGHT, SYSTEM

### Community 60 - "Contributor Covenant Code of Conduct"
Cohesion: 0.15
Nodes (12): 1. Correction, 2. Warning, 3. Temporary Ban, 4. Permanent Ban, Attribution, Contributor Covenant Code of Conduct, Enforcement, Enforcement Guidelines (+4 more)

### Community 61 - "AppContainer.kt"
Cohesion: 0.25
Nodes (3): MirrorTester, LibrarySync, SyncReport

### Community 63 - "HomeScreen.kt"
Cohesion: 0.08
Nodes (6): BeatmapArt(), MosaicCover(), CoverCard(), TrackPill(), CollectionCard(), ExploreScreen()

### Community 64 - "HomeScreen"
Cohesion: 0.26
Nodes (8): CatalogErrorBanner(), Greeting(), HomeScreen(), ListeningStatsCard(), RecentlyPlayedGrid(), StatBlock(), WeekBars(), YourMixCard()

### Community 65 - "AccountPage"
Cohesion: 0.29
Nodes (9): AccountPage(), GradeTile(), OAuthCredentialsDialog(), ProfileChip(), ProfileHeader(), ProfileStats(), formatCompact(), formatDurationLong() (+1 more)

### Community 67 - "GameClock"
Cohesion: 0.07
Nodes (5): GameAudio, HitSoundPlayer, AudioAnchor, GameClock, PreviewPlayer

### Community 68 - "SettingsPage"
Cohesion: 0.20
Nodes (10): SettingsPage, ABOUT, ACCOUNT, APPEARANCE, CONTENT, OFFLINE, PLAYBACK, ROOT (+2 more)

### Community 69 - "ModeFilter"
Cohesion: 0.22
Nodes (6): ModeFilter, ANY, CATCH, MANIA, OSU, TAIKO

### Community 71 - "StatusFilter"
Cohesion: 0.25
Nodes (5): StatusFilter, ANY, DEFAULT, LOVED, RANKED

### Community 72 - "GameStageModule"
Cohesion: 0.14
Nodes (5): DefaultHitsound, GameStageModule, SubmitOutcome, ScoreRecord, ScoreDatabase

### Community 74 - "StreamMode"
Cohesion: 0.33
Nodes (3): StreamMode, FULL, PREVIEW

### Community 76 - ".remember"
Cohesion: 0.70
Nodes (3): AddMirrorDialog(), MirrorRow(), SourcesPage()

### Community 77 - "PlaylistPickerSheet"
Cohesion: 0.60
Nodes (4): PlaylistPickerSheet(), SheetAction(), SheetTrackHeader(), TrackMenuSheet()

### Community 81 - "VerifyState"
Cohesion: 0.33
Nodes (5): Checking, Failure, Idle, Success, VerifyState

### Community 83 - "Judgement"
Cohesion: 0.17
Nodes (9): HitWindows, Judgement, GOOD, GREAT, MEH, MISS, OK, PERFECT (+1 more)

### Community 92 - "StatsPage"
Cohesion: 0.83
Nodes (3): DayBars(), RankedList(), StatsPage()

### Community 94 - "GameSession"
Cohesion: 0.33
Nodes (5): GameSession, findActivity(), Hud(), PlayScreen(), PreparationPanel()

## Knowledge Gaps
- **84 isolated node(s):** `Queued`, `OFFICIAL_OSU`, `PUBLIC_MIRROR`, `MIXED`, `NONE` (+79 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 529 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **34 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Beatmap` connect `KozuShell` to `DownloadsStore`, `PlayScreen.kt`, `LibraryScreen.kt`, `PlaylistStore`, `WavySlider`, `AccountPage.kt`, `BeatmapRepository`, `SearchScreen.kt`, `PlaybackController.kt`, `TrackRow.kt`, `SearchViewModel.kt`, `GameStageHost.kt`, `KozuApp.kt`, `SettingsViewModel.kt`, `DiscoverFeed.kt`, `SettingsViewModel`, `PlaybackController`, `DownloadManager`, `Locals.kt`, `LibraryViewModel`, `HistoryStore`, `GameViewModel.kt`, `ExploreViewModel.kt`, `HistoryStore.kt`, `HomeScreen.kt`, `HomeScreen`, `DownloadManager.kt`, `PlaylistPickerSheet`?**
  _High betweenness centrality (0.131) - this node is a cross-community bridge._
- **What connects `Queued`, `OFFICIAL_OSU`, `PUBLIC_MIRROR` to the rest of the system?**
  _84 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `PlayScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.07568027210884354 - nodes in this community are weakly interconnected._
- **Why does `AppContainer` connect `AppContainer` to `DownloadsStore`, `PlaylistStore`, `BeatmapRepository`, `KozuApp.kt`, `.create`, `SettingsViewModel.kt`, `SettingsStore`, `DiscoverFeed.kt`, `AccountRepository`, `StreamResolver`, `SettingsViewModel`, `KozuShell`, `PlaybackController`, `DownloadManager`, `SearchViewModel`, `OsuAuth`, `LibraryViewModel`, `HistoryStore`, `GameViewModel.kt`, `ExploreViewModel.kt`, `AppContainer.kt`, `HistoryStore.kt`?**
  _High betweenness centrality (0.072) - this node is a cross-community bridge._
- **Should `LibraryScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.12660028449502134 - nodes in this community are weakly interconnected._
- **Why does `SettingsViewModel` connect `SettingsViewModel` to `DownloadsStore`, `AccountPage`, `ModeFilter`, `StatusFilter`, `StreamMode`, `PlaybackController.kt`, `.remember`, `AccentColor`, `android`, `VerifyState`, `SettingsViewModel.kt`, `SettingsScreen`, `StreamResolver.kt`, `AccountRepository`, `AppContainer`, `SettingsStore.kt`, `StatsPage`, `StatsPage`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._
- **Should `PlaylistStore` be split into smaller, more focused modules?**
  _Cohesion score 0.06988120195667366 - nodes in this community are weakly interconnected._