package vn.kozu.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.kozu.data.backup.BackupException
import vn.kozu.data.update.UpdateChecker
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.AppSettings
import vn.kozu.domain.model.LibraryNavigationMode
import vn.kozu.domain.model.NavigationStyle
import vn.kozu.domain.model.PlayerLayoutStyle
import vn.kozu.domain.model.ThemeMode
import vn.kozu.domain.model.ThemePreset
import vn.kozu.ui.i18n.KozuText as Text
import vn.kozu.ui.settings.ThemePresetRow
import vn.kozu.ui.theme.KozuCookies

/** Lets Settings > About reopen the wizard; provided by the app shell. */
val LocalReplayOnboarding = androidx.compose.runtime.compositionLocalOf<(() -> Unit)?> { null }

private const val STEP_COUNT = 10
private const val MAX_BACKUP_BYTES = 32 * 1024 * 1024

/**
 * First-run wizard: a handful of full-screen steps that set the app up the way the person
 * likes it before they ever see the home screen. Every step can be skipped, and every choice
 * is applied live so the wizard itself previews the theme.
 */
@Composable
fun OnboardingScreen(
    container: AppContainer,
    settings: AppSettings,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }

    val finish = {
        container.settings.onboardingCompleted = true
        onFinish()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = step,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val forward = targetState > initialState
                    val enter = slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { if (forward) it / 5 else -it / 5 }
                    val exit = slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { if (forward) -it / 5 else it / 5 }
                    (enter + fadeIn(tween(220))) togetherWith (exit + fadeOut(tween(140)))
                },
                label = "onboardingStep",
            ) { current ->
                when (current) {
                    0 -> WelcomeStep()
                    1 -> NotificationsStep()
                    2 -> BackupStep(container)
                    3 -> ThemeStep(container, settings)
                    4 -> ColorsStep(container, settings)
                    5 -> LibraryStep(container, settings)
                    6 -> NavigationStep(container, settings)
                    7 -> PlayerStep(container, settings)
                    8 -> BatteryStep()
                    else -> DoneStep()
                }
            }
        }
        BottomBar(
            step = step,
            onNext = { if (step >= STEP_COUNT - 1) finish() else step++ },
        )
    }
}

// ───────────────────────────── chrome ─────────────────────────────

@Composable
private fun BottomBar(
    step: Int,
    onNext: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val last = step >= STEP_COUNT - 1
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        color = colors.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    when {
                        step == 0 -> "Let's Go!"
                        last -> "All set"
                        else -> "Step ${step + 1} of $STEP_COUNT"
                    },
                style = if (step == 0) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                fontWeight = if (step == 0) FontWeight.Bold else FontWeight.Normal,
                color = if (step == 0) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = onNext,
                modifier = Modifier.size(width = 76.dp, height = 60.dp),
                shape = RoundedCornerShape(if (last) 30.dp else 22.dp),
                color = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (last) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = if (last) "Finish" else "Next",
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepPage(
    title: String,
    subtitle: String? = null,
    illustration: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (illustration != null) {
            Spacer(Modifier.height(28.dp))
            illustration()
        }
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(16.dp))
    }
}

/** A big rounded blob with a glyph, orbited by two smaller tiles, standing in for artwork. */
@Composable
private fun IconCluster(
    main: ImageVector,
    left: ImageVector,
    right: ImageVector,
) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(width = 280.dp, height = 220.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(172.dp),
            shape = KozuCookies.Soft9,
            color = colors.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(main, contentDescription = null, tint = colors.primary, modifier = Modifier.size(84.dp))
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp).size(72.dp).rotate(-12f),
            shape = RoundedCornerShape(24.dp),
            color = colors.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(left, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(34.dp))
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).size(76.dp).rotate(10f),
            shape = RoundedCornerShape(28.dp),
            color = colors.tertiaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(right, contentDescription = null, tint = colors.onTertiaryContainer, modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    badge: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = if (selected) colors.primary else colors.surfaceContainerHighest,
                contentColor = if (selected) colors.onPrimary else colors.onSurfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.primary,
                        contentColor = colors.onPrimary,
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Text(badge, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                )
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
private fun SwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Surface(
        onClick = { onChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun SkipHint(text: String = "You can change this later in Settings") {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp),
    )
}

// ───────────────────────────── steps ─────────────────────────────

@Composable
private fun WelcomeStep() {
    val context = LocalContext.current
    val version = remember { UpdateChecker.installedVersion(context) }
    StepPage(
        title = "Welcome to",
        illustration = { IconCluster(Icons.Rounded.MusicNote, Icons.Rounded.Headphones, Icons.Rounded.Album) },
    ) {
        Text(
            text = "Kozu",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "v$version",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Let's get everything set up for you.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
private fun NotificationsStep() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(notificationsGranted(context)) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it || notificationsGranted(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = notificationsGranted(context) }

    StepPage(
        title = "Notifications",
        subtitle = "Control your music from the lock screen and notification shade.",
        illustration = { IconCluster(Icons.Rounded.Notifications, Icons.Rounded.MusicNote, Icons.AutoMirrored.Rounded.QueueMusic) },
    ) {
        if (granted) {
            Text("Notifications are enabled", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        } else {
            Button(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Enable Notifications") }
            SkipHint("You can skip this and enable it later")
        }
    }
}

@Composable
private fun BackupStep(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                busy = true
                result =
                    try {
                        val json =
                            withContext(Dispatchers.IO) {
                                val bytes =
                                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                        ?: throw java.io.IOException("Could not open the selected file")
                                if (bytes.size > MAX_BACKUP_BYTES) throw BackupException("That file is too large to be a backup")
                                String(bytes, Charsets.UTF_8)
                            }
                        val info = container.backup.inspect(json)
                        container.backup.restore(json, info.sections)
                    } catch (e: BackupException) {
                        e.message ?: "Invalid backup"
                    } catch (e: Exception) {
                        "Could not restore: ${e.message ?: "unknown error"}"
                    }
                busy = false
            }
        }

    StepPage(
        title = "Do you have a backup?",
        subtitle = "Restore an existing Kozu backup to skip setup on this device.",
        illustration = { IconCluster(Icons.Rounded.Restore, Icons.Rounded.LibraryMusic, Icons.Rounded.Tune) },
    ) {
        Button(onClick = { launcher.launch(arrayOf("*/*")) }, enabled = !busy) {
            Text(if (busy) "Restoring…" else "Import backup")
        }
        result?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
        SkipHint("Skip / Not now: just continue")
    }
}

@Composable
private fun ThemeStep(
    container: AppContainer,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    StepPage(
        title = "App Theme",
        subtitle = "Pick the look you want before you start exploring.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionCard(
                icon = Icons.Rounded.DarkMode,
                title = "Dark",
                subtitle = "Easy on the eyes, great for late-night listening.",
                selected = settings.themeMode == ThemeMode.DARK,
                badge = "Recommended",
                onClick = { scope.launch { container.settings.setThemeMode(ThemeMode.DARK) } },
            )
            OptionCard(
                icon = Icons.Rounded.LightMode,
                title = "Light",
                subtitle = "A brighter Material 3 look across the app.",
                selected = settings.themeMode == ThemeMode.LIGHT,
                onClick = { scope.launch { container.settings.setThemeMode(ThemeMode.LIGHT) } },
            )
            OptionCard(
                icon = Icons.Rounded.PhoneAndroid,
                title = "Follow system",
                subtitle = "Match your phone's appearance setting.",
                selected = settings.themeMode == ThemeMode.SYSTEM,
                onClick = { scope.launch { container.settings.setThemeMode(ThemeMode.SYSTEM) } },
            )
            SwitchCard(
                title = "AMOLED",
                subtitle = "Pure black dark theme",
                checked = settings.amoled,
                onChange = { scope.launch { container.settings.setAmoled(it) } },
            )
        }
        SkipHint("You can change this later in Settings > Appearance")
    }
}

@Composable
private fun ColorsStep(
    container: AppContainer,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    StepPage(
        title = "Theme Preset",
        subtitle = "Choose a color palette. osu! is the default.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePreset.entries.filter { it != ThemePreset.CUSTOM }.forEach { preset ->
                ThemePresetRow(
                    preset = preset,
                    accent = settings.accent,
                    selected = settings.themePreset == preset,
                    onClick = { scope.launch { container.settings.setThemePreset(preset) } },
                )
            }
            SwitchCard(
                title = "Adapt to artwork",
                subtitle = "While a song plays, colors follow its cover.",
                checked = settings.artworkColors,
                onChange = { scope.launch { container.settings.setArtworkColors(it) } },
            )
        }
        SkipHint("You can change this later in Settings > Appearance > Colors")
    }
}

@Composable
private fun LibraryStep(
    container: AppContainer,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    StepPage(
        title = "Library Layout",
        subtitle = "Choose how you move around your library.",
        illustration = { IconCluster(Icons.Rounded.LibraryMusic, Icons.Rounded.Album, Icons.AutoMirrored.Rounded.QueueMusic) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LibraryNavigationMode.entries.forEach { mode ->
                OptionCard(
                    icon = Icons.Rounded.LibraryMusic,
                    title = mode.label,
                    subtitle =
                        if (mode == LibraryNavigationMode.COMPACT) {
                            "Minimal pill navigation, swipe between tabs."
                        } else {
                            "Standard tab row, always visible."
                        },
                    selected = settings.libraryNavigationMode == mode,
                    onClick = { scope.launch { container.settings.setLibraryNavigationMode(mode) } },
                )
            }
        }
        SkipHint("You can change this later in Settings > Appearance")
    }
}

@Composable
private fun NavigationStep(
    container: AppContainer,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val radiusState = rememberSliderState(
        value = settings.navCornerRadius.toFloat(),
        trackRange = 0f..32f,
    )
    val radius = radiusState.value
    val floating = settings.navigationStyle == NavigationStyle.FLOATING
    StepPage(
        title = "App Navigation",
        subtitle = "Choose the style of the bottom navigation bar.",
        illustration = { NavPreview(floating = floating, radius = radius) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionCard(
                icon = Icons.Rounded.ViewCarousel,
                title = "Expressive pill",
                subtitle = "Floating capsule with a custom corner radius.",
                selected = floating,
                onClick = { scope.launch { container.settings.setNavigationStyle(NavigationStyle.FLOATING) } },
            )
            OptionCard(
                icon = Icons.Rounded.Tune,
                title = "Classic",
                subtitle = "Edge-to-edge bar across the bottom.",
                selected = !floating,
                onClick = { scope.launch { container.settings.setNavigationStyle(NavigationStyle.CLASSIC) } },
            )
            if (floating) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Corner radius", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text("${radius.toInt()} dp", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            state = radiusState,
                            onValueChange = { radiusState.value = it },
                            onValueChangeFinished = {
                                scope.launch { container.settings.setNavCornerRadius(radiusState.value.toInt()) }
                            },
                        )
                    }
                }
            }
        }
        SkipHint("You can change this later in Settings > Appearance > Navigation bar")
    }
}

/** A tiny mock-up of the screen with the chosen navigation bar at the bottom. */
@Composable
private fun NavPreview(
    floating: Boolean,
    radius: Float,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().height(132.dp),
        shape = RoundedCornerShape(26.dp),
        color = colors.surfaceContainerHigh,
    ) {
        Box(Modifier.padding(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth(0.55f).height(10.dp).background(colors.surfaceContainerHighest, CircleShape))
                Box(Modifier.fillMaxWidth(0.8f).height(10.dp).background(colors.surfaceContainerHighest, CircleShape))
            }
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .then(if (floating) Modifier.fillMaxWidth(0.82f) else Modifier.fillMaxWidth())
                        .height(44.dp),
                shape = if (floating) RoundedCornerShape((14 + radius).dp.coerceAtMost(22.dp)) else RoundedCornerShape(10.dp),
                color = colors.surfaceContainerHighest,
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(4) { i ->
                        Box(
                            Modifier
                                .size(if (i == 0) 24.dp else 16.dp)
                                .background(if (i == 0) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.5f), CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PlayerStep(
    container: AppContainer,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val current = settings.appearance.playerLayoutStyle
    StepPage(
        title = "Player Design",
        subtitle = "Pick how the full player looks. Tap one to try it.",
        illustration = { IconCluster(Icons.Rounded.ViewCarousel, Icons.Rounded.Palette, Icons.Rounded.MusicNote) },
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PlayerLayoutStyle.entries.forEach { style ->
                FilterChip(
                    selected = style == current,
                    onClick = { scope.launch { container.settings.updateAppearance { copy(playerLayoutStyle = style) } } },
                    label = { Text(style.label) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = current.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        SkipHint("You can change this later in Settings > Appearance > Player")
    }
}

private fun batteryUnrestricted(context: Context): Boolean =
    runCatching {
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)
    }.getOrDefault(false)

@Composable
private fun BatteryStep() {
    val context = LocalContext.current
    var unrestricted by remember { mutableStateOf(batteryUnrestricted(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { unrestricted = batteryUnrestricted(context) }
    StepPage(
        title = "Battery Optimization",
        subtitle = "Turn off battery optimization to prevent playback interruptions in the background.",
        illustration = { IconCluster(Icons.Rounded.BatteryChargingFull, Icons.Rounded.MusicNote, Icons.Rounded.Headphones) },
    ) {
        if (unrestricted) {
            Text("Battery optimization is already off", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        } else {
            FilledTonalButton(
                onClick = {
                    val intents =
                        listOf(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                        )
                    for (intent in intents) {
                        if (runCatching { context.startActivity(intent) }.isSuccess) break
                    }
                },
            ) { Text("Disable Optimization") }
            SkipHint("You can skip this for now")
        }
    }
}

@Composable
private fun DoneStep() {
    StepPage(
        title = "You're all set",
        subtitle = "Search for a song, tap play and enjoy. Everything here lives in Settings whenever you want to tweak it.",
        illustration = { IconCluster(Icons.Rounded.Check, Icons.Rounded.Headphones, Icons.Rounded.MusicNote) },
    )
}
