package vn.kozu.ui.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.text.Normalizer
import vn.kozu.R
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.ViewCompact
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

enum class SettingsPage(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
) {
    ROOT(R.string.settings, 0, Icons.Rounded.Tune),
    ACCOUNT(R.string.osu_account, R.string.sign_in_and_api_access, Icons.Rounded.AccountCircle),
    MUSIC(R.string.music, R.string.catalog_and_library_sync, Icons.Rounded.LibraryMusic),
    CONTENT(R.string.content, R.string.titles_and_catalog_preferences, Icons.Rounded.Language),
    APPEARANCE(R.string.appearance, R.string.theme_and_layout, Icons.Rounded.Palette),
    LANGUAGE(R.string.language, R.string.app_language, Icons.Rounded.Language),
    TYPOGRAPHY(R.string.typography, R.string.font_and_text, Icons.Rounded.TextFields),
    PLAYER_STYLE(R.string.player, R.string.mini_player_and_full_player, Icons.Rounded.PlayCircle),
    PLAYBACK(R.string.playback, R.string.audio_settings, Icons.Rounded.MusicNote),
    BEHAVIOR(R.string.behavior, R.string.controls_and_gestures, Icons.Rounded.TouchApp),
    BACKUP(R.string.backup_and_restore, R.string.export_and_restore_data, Icons.Rounded.Upload),
    DEVELOPER(R.string.developer_options, R.string.advanced_maintenance, Icons.Rounded.DeveloperMode),
    STORAGE(R.string.storage, R.string.cache_and_downloads, Icons.Rounded.Storage),
    EQUALIZER(R.string.equalizer, R.string.sound_settings, Icons.Rounded.GraphicEq),
    DEVICE(R.string.device, R.string.audio_and_decoder_support, Icons.Rounded.DeveloperBoard),
    DOWNLOADS(R.string.downloads, R.string.offline_beatmaps, Icons.Rounded.CloudDownload),
    SOURCES(R.string.audio_sources, R.string.streaming_mirrors, Icons.Rounded.Dns),
    STATS(R.string.listening_history, R.string.plays_and_favorites, Icons.Rounded.BarChart),
    UPDATES(R.string.updates, R.string.version_and_channel, Icons.Rounded.SystemUpdate),
    ABOUT(R.string.about, R.string.app_and_project_info, Icons.Rounded.Info),
    LICENSES(R.string.licenses, R.string.open_source_notices, Icons.Rounded.Layers),
}

enum class Setting(
    val page: SettingsPage,
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val keywords: String = "",
) {
    LANGUAGE(
        SettingsPage.LANGUAGE,
        Icons.Rounded.Language,
        R.string.language,
        R.string.choose_the_app_language,
        "locale translation system",
    ),
    LOGIN(
        SettingsPage.ACCOUNT,
        Icons.Rounded.AccountCircle,
        R.string.osu_account,
        R.string.sync_your_osu_profile_and_favorites,
        "login sign in oauth profile",
    ),
    OSU_API(
        SettingsPage.ACCOUNT,
        Icons.Rounded.Code,
        R.string.osu_api_credentials,
        R.string.credentials_for_the_official_osu_catalog,
        "client id secret api key",
    ),

    DEFAULT_STATUS(
        SettingsPage.MUSIC,
        Icons.Rounded.FilterAlt,
        R.string.default_status_filter,
        R.string.choose_the_default_beatmap_status,
        "ranked loved filter category",
    ),
    DEFAULT_MODE(
        SettingsPage.MUSIC,
        Icons.Rounded.Explore,
        R.string.default_game_mode,
        R.string.choose_a_default_game_mode,
        "osu taiko catch mania ruleset",
    ),
    SYNC_NOW(
        SettingsPage.MUSIC,
        Icons.Rounded.Sync,
        R.string.sync_library,
        R.string.sync_favorites_playlists_and_downloads,
        "refresh update library",
    ),
    AUTO_SYNC(
        SettingsPage.MUSIC,
        Icons.Rounded.Cached,
        R.string.sync_on_launch,
        R.string.sync_the_library_on_launch,
        "automatic",
    ),
    ARTWORK_WIFI(
        SettingsPage.MUSIC,
        Icons.Rounded.Image,
        R.string.wi_fi_artwork_quality,
        R.string.resolution_for_covers_on_wi_fi,
        "cover image resolution",
    ),
    ARTWORK_MOBILE(
        SettingsPage.MUSIC,
        Icons.Rounded.Image,
        R.string.mobile_artwork_quality,
        R.string.resolution_for_covers_on_mobile_data,
        "cover image resolution data saver",
    ),
    IMAGE_CACHE(
        SettingsPage.MUSIC,
        Icons.Rounded.Storage,
        R.string.artwork_cache_limit,
        R.string.maximum_disk_space_used_by_cached_covers,
        "image cache size",
    ),

    PREFER_UNICODE(
        SettingsPage.CONTENT,
        Icons.Rounded.TextFields,
        R.string.prefer_original_titles,
        R.string.prefer_original_song_titles_when_available,
        "unicode romanised japanese",
    ),
    SHOW_MAPPER(SettingsPage.CONTENT, Icons.Rounded.Star, R.string.show_mapper_names, R.string.display_who_mapped_each_beatmap, "creator"),
    TOP_SIZE(SettingsPage.CONTENT, Icons.Rounded.BarChart, R.string.my_top_size, R.string.number_of_songs_in_top_stats_lists, "top stats"),
    SHOW_EXPLICIT(
        SettingsPage.CONTENT,
        Icons.Rounded.Security,
        R.string.show_explicit_content,
        R.string.include_explicit_maps_in_search_results,
        "nsfw adult mature",
    ),
    SHOW_MIX_CARD(SettingsPage.CONTENT, Icons.Rounded.Shuffle, R.string.show_your_mix, R.string.display_the_shuffle_mix_card_on_home, "collage home"),
    MIX_SIZE(SettingsPage.CONTENT, Icons.Rounded.Shuffle, R.string.your_mix_size, R.string.choose_how_many_songs_play_when_you_shuffle_your_mix, "mix shuffle queue songs count"),
    DISPLAY_NAME(SettingsPage.CONTENT, Icons.Rounded.AccountCircle, R.string.display_name, R.string.name_used_in_the_home_greeting, "greeting username"),
    PROXY(
        SettingsPage.CONTENT,
        Icons.Rounded.Cloud,
        R.string.proxy_server,
        R.string.use_an_http_or_socks5_proxy,
        "network vpn socks http",
    ),
    CLEAR_HISTORY(
        SettingsPage.CONTENT,
        Icons.Rounded.History,
        R.string.clear_listening_history,
        R.string.remove_every_play_recorded_on_this_device,
        "privacy stats delete",
    ),
    CLEAR_SEARCHES(
        SettingsPage.CONTENT,
        Icons.Rounded.DeleteSweep,
        R.string.clear_recent_searches,
        R.string.forget_your_recent_search_terms,
        "privacy search history delete",
    ),

    APP_THEME(
        SettingsPage.APPEARANCE,
        Icons.Rounded.WbSunny,
        R.string.app_theme,
        R.string.choose_light_dark_or_system_theme,
        "dark light mode",
    ),
    PURE_BLACK(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Palette,
        R.string.pure_black,
        R.string.use_true_black_surfaces_on_oled_screens,
        "amoled oled dark",
    ),
    APP_FONT(
        SettingsPage.TYPOGRAPHY,
        Icons.Rounded.FormatSize,
        R.string.app_font,
        R.string.choose_the_app_font,
        "typeface quicksand roboto google sans flex",
    ),
    TEXT_SIZE(
        SettingsPage.TYPOGRAPHY,
        Icons.Rounded.FormatSize,
        R.string.text_size,
        R.string.adjust_text_size_across_the_app,
        "font scale bigger smaller large",
    ),
    FONT_WEIGHT(
        SettingsPage.TYPOGRAPHY,
        Icons.Rounded.FormatBold,
        R.string.text_weight,
        R.string.make_text_lighter_or_bolder_across_the_app,
        "bold thin heavy light font",
    ),
    FONT_WIDTH(
        SettingsPage.TYPOGRAPHY,
        Icons.Rounded.SwapHoriz,
        R.string.letter_width,
        R.string.adjust_letter_width_in_google_sans_flex,
        "condensed expanded stretch font width",
    ),
    FONT_ROUNDNESS(
        SettingsPage.TYPOGRAPHY,
        Icons.Rounded.RoundedCorner,
        R.string.letter_roundness,
        R.string.adjust_font_roundness_in_google_sans_flex,
        "rounded soft font",
    ),
    RESET_TYPOGRAPHY(
        SettingsPage.TYPOGRAPHY,
        Icons.Rounded.RestartAlt,
        R.string.reset_typography,
        R.string.restore_default_typography,
        "default font reset",
    ),
    UI_DENSITY(
        SettingsPage.APPEARANCE,
        Icons.Rounded.ViewCompact,
        R.string.layout_density,
        R.string.set_spacing_across_the_app,
        "compact spacing size ui scale padding",
    ),
    COMPACT_MODE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Compress,
        R.string.compact_mode,
        R.string.use_tighter_spacing_and_smaller_controls,
        "dense small spacing",
    ),
    ARTWORK_SHAPE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Category,
        R.string.album_art_shape,
        R.string.set_artwork_shape_throughout_the_app,
        "cover circle cookie square rounded",
    ),
    SLIDER_STYLE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.LinearScale,
        R.string.slider_style,
        R.string.choose_the_slider_style,
        "seek bar wavy expressive thumb",
    ),
    ICON_STYLE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Widgets,
        R.string.player_icon_style,
        R.string.rounded_outlined_or_sharp_playback_icons,
        "icons skip shuffle repeat",
    ),
    PLAYER_PAGE_ENTRY(
        SettingsPage.APPEARANCE,
        Icons.Rounded.PlayCircle,
        R.string.player_and_mini_player,
        R.string.customize_the_mini_player,
        "mini player bar now playing",
    ),
    TYPOGRAPHY_ENTRY(
        SettingsPage.APPEARANCE,
        Icons.Rounded.TextFields,
        R.string.typography,
        R.string.adjust_player_typography,
        "font text size",
    ),
    MINI_STYLE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.ViewCarousel,
        R.string.mini_player_style,
        R.string.choose_a_mini_player_layout,
        "player bar layout",
    ),
    MINI_COLORS(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Palette,
        R.string.mini_player_colors,
        R.string.choose_mini_player_colors,
        "player bar color theme",
    ),
    MINI_PROGRESS(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.LinearScale,
        R.string.mini_player_progress,
        R.string.choose_the_progress_indicator,
        "progress bar ring wavy",
    ),
    MINI_ART_SHAPE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Image,
        R.string.mini_player_artwork,
        R.string.shape_of_the_cover_in_the_player_bar,
        "cover circle cookie",
    ),
    MINI_CORNER(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.RoundedCorner,
        R.string.mini_player_corner_radius,
        R.string.adjust_how_round_the_player_bar_is,
        "rounded shape corner",
    ),
    MINI_CONTROLS(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.SkipNext,
        R.string.skip_buttons,
        R.string.show_previous_and_next_controls,
        "previous next controls",
    ),
    MINI_ARTIST(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Person,
        R.string.show_artist,
        R.string.show_the_artist_name,
        "subtitle",
    ),
    MINI_SPIN(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Autorenew,
        R.string.spinning_artwork,
        R.string.rotate_round_artwork_while_a_song_plays,
        "rotate vinyl",
    ),
    MINI_SWIPE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Swipe,
        R.string.swipe_to_skip,
        R.string.swipe_sideways_to_skip_or_down_to_dismiss,
        "gesture next previous",
    ),
    PLAYER_LAYOUT(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.ViewCarousel,
        R.string.player_design_style,
        R.string.player_design_style_desc,
        "now playing layout design style classic modern minimal cinematic little expressive immersive extended material editorial",
    ),
    SEEK_STYLE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.GraphicEq,
        R.string.seek_bar_style,
        R.string.choose_the_full_player_progress_style,
        "progress slider wavy",
    ),
    PLAYER_ART_SHAPE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Image,
        R.string.player_artwork_shape,
        R.string.shape_of_the_large_cover_in_the_full_player,
        "cover circle cookie",
    ),
    TRANSPORT_SHAPE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.PlayCircle,
        R.string.control_buttons,
        R.string.shape_of_play_previous_and_next_buttons,
        "transport pill round square",
    ),
    TOGGLE_ROW(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Shuffle,
        R.string.shuffle_repeat_and_like_row,
        R.string.show_quick_controls_in_the_player,
        "shuffle repeat favorite",
    ),
    VOLUME_BAR(
        SettingsPage.PLAYER_STYLE,
        Icons.AutoMirrored.Rounded.VolumeUp,
        R.string.show_volume_bar,
        R.string.show_volume_bar_desc,
        "volume slider loudness sound bar",
    ),
    RESET_PLAYER_STYLE(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.RestartAlt,
        R.string.reset_player_style,
        R.string.restore_default_player_appearance,
        "default reset",
    ),
    PLAYBACK_ICON(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.PlayCircle,
        R.string.playback_icon,
        R.string.choose_the_play_icon,
        "play pause control icon",
    ),
    LIBRARY_LAYOUT(
        SettingsPage.APPEARANCE,
        Icons.Rounded.ViewCarousel,
        R.string.library_navigation,
        R.string.choose_the_navigation_style,
        "library tabs compact layout",
    ),
    LIBRARY_DENSITY(
        SettingsPage.APPEARANCE,
        Icons.Rounded.ViewAgenda,
        R.string.library_rows,
        R.string.use_compact_track_rows_in_the_library,
        "library compact dense spacing",
    ),
    APP_ICON(SettingsPage.APPEARANCE, Icons.Rounded.AutoAwesome, R.string.app_icon, R.string.choose_the_app_icon, "launcher logo"),
    APP_STYLE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.AutoAwesome,
        R.string.app_style,
        R.string.choose_how_kozu_looks_and_feels,
        "expressive classic material",
    ),
    THEME_PRESET(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Palette,
        R.string.theme_preset,
        R.string.theme_preset_desc,
        "theme preset palette color scheme dynamic wallpaper material you osu sage sunset ocean sunny grey",
    ),
    ACCENT_COLOR(SettingsPage.APPEARANCE, Icons.Rounded.Palette, R.string.accent_color, R.string.used_when_nothing_is_playing, "theme color seed"),
    ARTWORK_COLORS(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Image,
        R.string.artwork_colors,
        R.string.match_the_colors_to_the_current_album_art,
        "dynamic color cover",
    ),
    PALETTE_STYLE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Palette,
        R.string.palette_style,
        R.string.adjust_artwork_based_colors,
        "tonal vibrant expressive neutral monochrome",
    ),
    HIGH_REFRESH(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Speed,
        R.string.high_refresh_rate,
        R.string.prioritize_smooth_scrolling,
        "120hz fps smooth",
    ),
    PERFORMANCE_MODE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Bolt,
        R.string.performance_mode,
        R.string.reduce_effects_to_save_battery,
        "low end battery saver",
    ),
    BLUR_PLAYER(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.BlurOn,
        R.string.blurred_player_background,
        R.string.blend_the_album_art_behind_the_full_player,
        "backdrop",
    ),
    BLUR_INTENSITY(SettingsPage.PLAYER_STYLE, Icons.Rounded.BlurOn, R.string.blur_intensity, R.string.how_soft_the_player_backdrop_looks, "radius"),
    SHOW_FILE_INFO(
        SettingsPage.PLAYER_STYLE,
        Icons.Rounded.Info,
        R.string.show_player_file_info,
        R.string.show_audio_details_in_the_player,
        "format audio quality",
    ),
    NAV_STYLE(
        SettingsPage.APPEARANCE,
        Icons.Rounded.ViewCarousel,
        R.string.navigation_bar_style,
        R.string.customize_the_navigation_bar,
        "pill floating classic",
    ),
    COMPACT_NAV(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Compress,
        R.string.icons_only_navigation,
        R.string.use_a_compact_navigation_bar,
        "labels compact nav",
    ),
    NAV_RADIUS(
        SettingsPage.APPEARANCE,
        Icons.Rounded.RoundedCorner,
        R.string.navigation_bar_corner_radius,
        R.string.set_navigation_bar_corners,
        "rounded shape",
    ),
    DEFAULT_TAB(
        SettingsPage.APPEARANCE,
        Icons.Rounded.Home,
        R.string.default_tab,
        R.string.choose_the_screen_kozu_opens_with,
        "start launch home explore library game",
    ),

    KEEP_PLAYING(
        SettingsPage.PLAYBACK,
        Icons.Rounded.MusicNote,
        R.string.keep_playing_after_closing,
        R.string.keep_playback_running_after_leaving_the_app,
        "background service recents",
    ),
    BATTERY_OPT(
        SettingsPage.PLAYBACK,
        Icons.Rounded.BatteryChargingFull,
        R.string.battery_optimization,
        R.string.allow_reliable_background_playback,
        "doze background",
    ),
    RESUME_HEADSET(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Headphones,
        R.string.resume_when_headphones_reconnect,
        R.string.resume_when_headphones_reconnect,
        "bluetooth wired",
    ),
    PAUSE_HEADSET(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Headphones,
        R.string.pause_when_headphones_disconnect,
        R.string.pause_when_headphones_disconnect,
        "noisy bluetooth wired",
    ),
    CROSSFADE(
        SettingsPage.PLAYBACK,
        Icons.Rounded.GraphicEq,
        R.string.crossfade,
        R.string.fade_between_songs,
        "fade transition gapless",
    ),
    HIFI(
        SettingsPage.PLAYBACK,
        Icons.Rounded.HighQuality,
        R.string.hi_fi_mode,
        R.string.use_32_bit_audio_output,
        "float pcm lossless",
    ),
    OFFLOAD(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Memory,
        R.string.hardware_audio_offload,
        R.string.use_hardware_audio_decoding,
        "dsp",
    ),
    REMEMBER_SHUFFLE(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Shuffle,
        R.string.persistent_shuffle,
        R.string.remember_shuffle_and_repeat_settings,
        "repeat",
    ),
    SHOW_QUEUE_HISTORY(
        SettingsPage.PLAYBACK,
        Icons.AutoMirrored.Rounded.QueueMusic,
        R.string.show_queue_history,
        R.string.show_previously_played_songs_in_the_queue,
        "previous",
    ),
    AUTOPLAY(
        SettingsPage.PLAYBACK,
        Icons.AutoMirrored.Rounded.QueueMusic,
        R.string.dynamic_queue_autoplay,
        R.string.add_related_songs_when_the_queue_ends,
        "radio similar endless",
    ),
    AVOID_REPEATS(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Shuffle,
        R.string.avoid_repetitive_songs,
        R.string.avoid_recently_played_songs,
        "recent history",
    ),
    DEDUPE_QUEUE(
        SettingsPage.PLAYBACK,
        Icons.Rounded.FilterAlt,
        R.string.deduplicate_queue_entries,
        R.string.remove_duplicate_songs_from_the_queue,
        "duplicate",
    ),
    PRELOAD_NEXT(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Download,
        R.string.queue_preloading,
        R.string.preload_the_next_song,
        "buffer gapless",
    ),
    STREAM_QUALITY(SettingsPage.PLAYBACK, Icons.Rounded.Tune, R.string.stream_quality, R.string.play_full_songs_or_short_previews, "preview full"),
    WIFI_ONLY_STREAM(
        SettingsPage.PLAYBACK,
        Icons.Rounded.Wifi,
        R.string.wi_fi_only_streaming,
        R.string.stream_only_while_connected_to_wi_fi,
        "mobile data",
    ),
    SKIP_SILENCE(SettingsPage.PLAYBACK, Icons.AutoMirrored.Rounded.VolumeUp, R.string.skip_silence, R.string.skip_quiet_sections_of_a_track, "gap"),

    HAPTICS(SettingsPage.BEHAVIOR, Icons.Rounded.Vibration, R.string.haptic_feedback, R.string.enable_vibration_feedback, "vibration"),
    AUTO_OPEN_PLAYER(
        SettingsPage.BEHAVIOR,
        Icons.Rounded.PlayCircle,
        R.string.open_player_on_play,
        R.string.open_the_full_player_on_play,
        "now playing",
    ),
    ARTWORK_SWIPE(
        SettingsPage.BEHAVIOR,
        Icons.Rounded.Swipe,
        R.string.artwork_gestures,
        R.string.use_artwork_gestures_in_the_player,
        "swipe",
    ),
    KEEP_SCREEN_ON(
        SettingsPage.BEHAVIOR,
        Icons.Rounded.ScreenLockPortrait,
        R.string.keep_screen_on_in_player,
        R.string.keep_the_screen_on_in_the_player,
        "awake",
    ),
    DOUBLE_TAP_SEARCH(
        SettingsPage.BEHAVIOR,
        Icons.Rounded.Search,
        R.string.double_tap_search_to_type,
        R.string.double_tap_search_to_start_typing,
        "keyboard",
    ),
    OSU_LINKS(
        SettingsPage.BEHAVIOR,
        Icons.Rounded.Link,
        R.string.osu_link_integration,
        R.string.open_osu_beatmap_links_in_kozu,
        "intercept deep link url",
    ),

    EQUALIZER_BANDS(
        SettingsPage.EQUALIZER,
        Icons.Rounded.Equalizer,
        R.string.equalizer,
        R.string.adjust_audio_frequencies_and_presets,
        "bass treble preset eq",
    ),
    EQUALIZER_PRESET(
        SettingsPage.EQUALIZER,
        Icons.Rounded.Equalizer,
        R.string.equalizer_preset,
        R.string.equalizer_preset_desc,
        "flat bass vocal treble custom eq",
    ),
    BASS_BOOST(SettingsPage.EQUALIZER, Icons.Rounded.GraphicEq, R.string.bass_boost, R.string.strengthen_low_frequencies, "bass"),
    VIRTUALIZER(
        SettingsPage.EQUALIZER,
        Icons.Rounded.Headphones,
        R.string.spatial_audio,
        R.string.use_android_spatial_audio_when_supported,
        "surround spatial headphones",
    ),
    LOUDNESS(SettingsPage.EQUALIZER, Icons.AutoMirrored.Rounded.VolumeUp, R.string.loudness, R.string.boost_overall_perceived_volume, "gain amplify"),

    STREAM_CACHE(
        SettingsPage.STORAGE,
        Icons.Rounded.Storage,
        R.string.stream_cache_limit,
        R.string.maximum_disk_space_for_cached_streams,
        "playback cache size",
    ),
    CLEAR_STREAM_CACHE(
        SettingsPage.STORAGE,
        Icons.Rounded.CleaningServices,
        R.string.clear_stream_cache,
        R.string.delete_cached_audio_streams,
        "free space",
    ),
    CLEAR_IMAGE_CACHE(
        SettingsPage.STORAGE,
        Icons.Rounded.CleaningServices,
        R.string.clear_artwork_cache,
        R.string.delete_cached_cover_images,
        "free space thumbnails",
    ),
    AUTO_CLEAN(
        SettingsPage.STORAGE,
        Icons.Rounded.CleaningServices,
        R.string.automatic_cleanup,
        R.string.clean_old_stream_cache_on_launch,
        "smart storage",
    ),
    DOWNLOAD_LIMIT(
        SettingsPage.STORAGE,
        Icons.Rounded.Download,
        R.string.download_storage_limit,
        R.string.set_the_offline_storage_limit,
        "offline",
    ),
    SLIM_DOWNLOADS(
        SettingsPage.STORAGE,
        Icons.Rounded.CleaningServices,
        R.string.slim_down_downloads,
        R.string.slim_down_downloads_desc,
        "optimize shrink offline size space",
    ),
    FORGOTTEN_TRACKS(
        SettingsPage.STORAGE,
        Icons.Rounded.History,
        R.string.forgotten_tracks,
        R.string.review_downloads_unused_for_60_days,
        "unused old downloads review",
    ),
    CLEAN_ALL(
        SettingsPage.STORAGE,
        Icons.Rounded.DeleteSweep,
        R.string.clean_all,
        R.string.clear_stream_and_artwork_caches,
        "free space reclaim",
    ),

    REFRESH_DEVICE(
        SettingsPage.DEVICE,
        Icons.Rounded.Sync,
        R.string.refresh_device_info,
        R.string.refresh_device_information,
        "reload specs",
    ),

    KEEP_FULL(
        SettingsPage.DOWNLOADS,
        Icons.Rounded.Download,
        R.string.keep_full_beatmap,
        R.string.keep_the_full_osz_file_for_gameplay,
        "osz",
    ),
    WIFI_ONLY_DOWNLOADS(
        SettingsPage.DOWNLOADS,
        Icons.Rounded.Wifi,
        R.string.wi_fi_only_downloads,
        R.string.download_beatmaps_on_wi_fi_only,
        "mobile data",
    ),
    AUTO_UPDATE_DOWNLOADS(
        SettingsPage.DOWNLOADS,
        Icons.Rounded.Sync,
        R.string.auto_update_downloads,
        R.string.update_downloaded_beatmaps_automatically,
        "outdated",
    ),

    EXPORT_BACKUP(
        SettingsPage.BACKUP,
        Icons.Rounded.Upload,
        R.string.export_backup,
        R.string.export_settings_and_personal_data,
        "save",
    ),
    IMPORT_BACKUP(SettingsPage.BACKUP, Icons.Rounded.Restore, R.string.import_backup, R.string.import_a_kozu_backup, "restore load"),

    RESET_SETTINGS(
        SettingsPage.DEVELOPER,
        Icons.Rounded.RestartAlt,
        R.string.reset_all_settings,
        R.string.reset_all_preferences,
        "factory",
    ),
    REFRESH_CATALOG(
        SettingsPage.DEVELOPER,
        Icons.Rounded.Sync,
        R.string.force_catalog_refresh,
        R.string.refresh_cached_catalog_pages,
        "reload",
    ),
    REBUILD_LIBRARY(
        SettingsPage.DEVELOPER,
        Icons.Rounded.LibraryMusic,
        R.string.rebuild_library,
        R.string.sync_personal_library_data_from_osu,
        "resync repair",
    ),
    REGENERATE_STATS(
        SettingsPage.DEVELOPER,
        Icons.Rounded.BarChart,
        R.string.regenerate_stats,
        R.string.rebuild_stats_from_listening_history,
        "recompute repair",
    ),
    CLEAR_ALL_CACHES(
        SettingsPage.DEVELOPER,
        Icons.Rounded.CleaningServices,
        R.string.clear_all_caches,
        R.string.delete_stream_artwork_and_catalog_caches,
        "free space reset",
    ),

    CHECK_UPDATES(
        SettingsPage.UPDATES,
        Icons.Rounded.SystemUpdate,
        R.string.check_for_updates,
        R.string.look_for_a_newer_kozu_release_on_github,
        "version release",
    ),
    AUTO_UPDATE_CHECK(
        SettingsPage.UPDATES,
        Icons.Rounded.Sync,
        R.string.automatic_update_check,
        R.string.check_for_a_new_release_when_the_app_starts,
        "background",
    ),
    SHARE_APP(SettingsPage.ABOUT, Icons.Rounded.Share, R.string.share_app, R.string.share_kozu_with_others, "invite"),
    RATE_APP(SettingsPage.ABOUT, Icons.Rounded.Star, R.string.rate_kozu, R.string.rate_kozu_on_github, "github review"),
    LICENSES_ENTRY(
        SettingsPage.ABOUT,
        Icons.Rounded.Layers,
        R.string.licenses,
        R.string.open_source_notices,
        "credits third party",
    ),
    PRIVACY_ENTRY(SettingsPage.ABOUT, Icons.Rounded.Security, R.string.privacy_policy, R.string.how_kozu_handles_your_data, "data"),
}

@Composable
fun SettingsPage.titleText(): String = stringResource(titleRes)

@Composable
fun SettingsPage.subtitleText(): String = if (subtitleRes == 0) "" else stringResource(subtitleRes)

@Composable
fun Setting.titleText(): String = stringResource(titleRes)

@Composable
fun Setting.descriptionText(): String = stringResource(descriptionRes)

private val searchSkip = setOf(Setting.LICENSES_ENTRY)

object SettingsSearch {
    fun search(
        context: Context,
        query: String,
    ): List<Setting> {
        val tokens = fold(query).split(' ', '-', '_').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()
        return Setting.entries
            .filterNot { it in searchSkip }
            .map { it to score(context, it, tokens) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun fold(text: String): String =
        Normalizer
            .normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('\u0111', 'd')

    private fun score(
        context: Context,
        setting: Setting,
        tokens: List<String>,
    ): Int {
        val title = fold(context.getString(setting.titleRes))
        val description = fold(context.getString(setting.descriptionRes))
        val keywords = fold(setting.keywords)
        val page = fold(context.getString(setting.page.titleRes))
        var total = 0
        for (token in tokens) {
            var best = 0
            if (title == token) best = maxOf(best, 100)
            if (title.startsWith(token)) best = maxOf(best, 80)
            if (title.contains(token)) best = maxOf(best, 60)
            if (keywords.split(' ').any { it.startsWith(token) }) best = maxOf(best, 45)
            if (page.contains(token)) best = maxOf(best, 30)
            if (description.contains(token)) best = maxOf(best, 20)
            if (best == 0 && token.length >= 4) {
                val words = (title + " " + keywords).split(' ').filter { it.length >= 3 }
                if (words.any { distance(it.take(token.length + 1), token) <= 1 }) {
                    best = 25
                } else if (isSubsequence(token, title)) {
                    best = 12
                }
            }
            if (best == 0) return 0
            total += best
        }
        return total
    }

    private fun isSubsequence(
        needle: String,
        haystack: String,
    ): Boolean {
        var index = 0
        for (char in haystack) {
            if (index < needle.length && char == needle[index]) index++
        }
        return index == needle.length
    }

    private fun distance(
        a: String,
        b: String,
    ): Int {
        if (a == b) return 0
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }
}
