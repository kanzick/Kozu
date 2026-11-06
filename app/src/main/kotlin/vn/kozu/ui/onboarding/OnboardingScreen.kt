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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import vn.kozu.ui.player.PlayerDesignPreview
import vn.kozu.ui.player.PlayerDesignSheet
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
                    1 -> NotificationsStep(next = { step++ })
                    2 -> BackupStep(container, next = { step++ })
                    3 -> ThemeStep(container, settings)
                    4 -> ColorsStep(container, settings)
                    5 -> LibraryStep(container, settings)
                    6 -> NavigationStep(container, settings)
                    7 -> PlayerStep(container, settings)
                    8 -> BatteryStep(next = { step++ })
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
    // The arrow button changes shape from step to step, a small nod to PixelMusic's wizard.
    val corners = remember { listOf(32, 18, 28, 14, 30, 20, 26, 16, 32, 36) }
    val corner by animateDpAsState(
        targetValue = corners[step.coerceIn(0, corners.lastIndex)].dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "wizardButtonCorner",
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        color = colors.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.navigationBarsPadding().padding(start = 28.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
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
                modifier = Modifier.size(width = 92.dp, height = 68.dp),
                shape = RoundedCornerShape(corner),
                color = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (last) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = if (last) "Finish" else "Next",
                        modifier = Modifier.size(30.dp),
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

/** A big rounded blob with a glyph, orbited by tilted tiles and bubbles, standing in for artwork. */
@Composable
private fun IconCluster(
    main: ImageVector,
    left: ImageVector,
    right: ImageVector,
) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(width = 300.dp, height = 240.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 22.dp).size(64.dp),
            shape = CircleShape,
            color = colors.surfaceContainerHighest,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(left, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(28.dp))
            }
        }
        Surface(
            modifier = Modifier.size(180.dp),
            shape = KozuCookies.Soft9,
            color = colors.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(main, contentDescription = null, tint = colors.onSurface.copy(alpha = 0.85f), modifier = Modifier.size(92.dp))
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.CenterEnd).size(width = 96.dp, height = 78.dp).rotate(10f),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainerHighest,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(right, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(40.dp))
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 8.dp).size(44.dp),
            shape = CircleShape,
            color = colors.primaryContainer,
        ) {}
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 26.dp).size(26.dp),
            shape = CircleShape,
            color = colors.tertiaryContainer,
        ) {}
    }
}

/** Wavy line used under the welcome illustration. */
@Composable
private fun WaveLine(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxWidth().height(24.dp)) {
        val path = Path()
        val amplitude = size.height * 0.32f
        val mid = size.height / 2f
        val wavelength = size.width / 9f
        path.moveTo(0f, mid)
        var x = 0f
        while (x <= size.width) {
            path.lineTo(x, mid + amplitude * sin((x / wavelength) * 2f * Math.PI.toFloat()))
            x += 4f
        }
        drawPath(path, color, style = Stroke(width = 5f, cap = StrokeCap.Round))
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
            Spacer(Modifier.width(8.dp))
            if (selected) {
                Surface(shape = CircleShape, color = colors.primary, contentColor = colors.onPrimary, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = CircleShape,
                        color = colors.onSurface.copy(alpha = 0.18f),
                        modifier = Modifier.size(12.dp),
                    ) {}
                }
            }
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
    val colors = MaterialTheme.colorScheme
    val version = remember { UpdateChecker.installedVersion(context) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(36.dp))
        Text(
            text = "Welcome\nto",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 52.sp,
        )
        Text(
            text = "Kozu",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.ExtraBold,
            color = colors.primary,
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainerHigh,
        ) {
            Text(
                text = "v$version",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerLowest,
        ) {
            Box(Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(bottom = 20.dp).size(150.dp),
                    shape = KozuCookies.Soft9,
                    color = colors.surfaceContainerHigh,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Headphones, null, tint = colors.primary, modifier = Modifier.size(84.dp))
                    }
                }
                Icon(
                    Icons.Rounded.MusicNote, null, tint = colors.tertiary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 44.dp).size(46.dp).rotate(12f),
                )
                Icon(
                    Icons.Rounded.Album, null, tint = colors.secondary,
                    modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 36.dp).size(40.dp).rotate(-14f),
                )
                Icon(
                    Icons.Rounded.MusicNote, null, tint = colors.primary.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp, top = 40.dp).size(30.dp),
                )
                WaveLine(Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 14.dp))
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = "Let's get everything set up for you.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
private fun NotificationsStep(next: () -> Unit) {
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
            TextButton(onClick = next) { Text("Skip for now") }
        }
    }
}

@Composable
private fun BackupStep(
    container: AppContainer,
    next: () -> Unit,
) {
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
        TextButton(onClick = next) { Text("Skip / Not now") }
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
    val colors = MaterialTheme.colorScheme
    val compact = settings.libraryNavigationMode == LibraryNavigationMode.COMPACT
    StepPage(
        title = "Library Layout",
        subtitle = "Choose your preferred way to navigate your library.",
    ) {
        // A live miniature of the Library header in the chosen layout.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerLow,
        ) {
            Box(
                Modifier
                    .background(Brush.verticalGradient(listOf(colors.primaryContainer.copy(alpha = 0.55f), Color.Transparent)))
                    .padding(horizontal = 20.dp, vertical = 22.dp),
            ) {
                Column {
                    Text("Library", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = colors.primary)
                    Spacer(Modifier.height(14.dp))
                    if (compact) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
                                Text("SONGS", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                            Surface(shape = CircleShape, color = colors.surfaceContainerHigh) {
                                Text("PLAYLISTS", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                            Surface(shape = CircleShape, color = colors.surfaceContainerHigh) {
                                Text("FAVORITES", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            Column {
                                Text("SONGS", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                                Box(Modifier.padding(top = 6.dp).size(width = 44.dp, height = 3.dp).background(colors.primary, CircleShape))
                            }
                            Text("PLAYLISTS", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                            Text("FAVORITES", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SwitchCard(
            title = "Compact Mode",
            subtitle = if (compact) "Using minimal pill navigation" else "Using standard tab row",
            checked = compact,
            onChange = { on ->
                scope.launch {
                    container.settings.setLibraryNavigationMode(
                        if (on) LibraryNavigationMode.COMPACT else LibraryNavigationMode.TAB_ROW,
                    )
                }
            },
        )
        SkipHint("You can change this later in Settings > Appearance > Library")
    }
}
@Composable
private fun NavigationStep(
    container: AppContainer,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    var radius by remember { mutableFloatStateOf(settings.navCornerRadius.toFloat()) }
    val floating = settings.navigationStyle == NavigationStyle.FLOATING
    StepPage(
        title = "App Navigation",
        subtitle = "Choose the style of the bottom navigation bar.",
    ) {
        NavPreview(floating = floating, radius = radius)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionCard(
                icon = Icons.Rounded.ViewCarousel,
                title = "Expressive Pill",
                subtitle = "Floating capsule with a custom corner radius.",
                selected = floating,
                onClick = { scope.launch { container.settings.setNavigationStyle(NavigationStyle.FLOATING) } },
            )
            OptionCard(
                icon = Icons.Rounded.Tune,
                title = "Classic Bar",
                subtitle = "Edge-to-edge navigation bar across the bottom.",
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
                            value = radius,
                            onValueChange = { radius = it },
                            onValueChangeFinished = { scope.launch { container.settings.setNavCornerRadius(radius.toInt()) } },
                            valueRange = 0f..32f,
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
    val colors = MaterialTheme.colorScheme
    val current = settings.appearance.playerLayoutStyle
    var sheetOpen by remember { mutableStateOf(false) }
    StepPage(
        title = "Player Design",
        subtitle = "Pick how the full player looks. You can try them all.",
    ) {
        Surface(
            onClick = { sheetOpen = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainer,
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
                ) {
                    PlayerDesignPreview(current, Modifier.width(112.dp))
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(current.label, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        current.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    FilledTonalButton(onClick = { sheetOpen = true }) { Text("Change design") }
                }
            }
        }
        SkipHint("You can change this later in Settings > Appearance > Player")
    }
    if (sheetOpen) {
        PlayerDesignSheet(
            selected = current,
            onSelect = { style ->
                scope.launch { container.settings.updateAppearance { copy(playerLayoutStyle = style) } }
                sheetOpen = false
            },
            onDismiss = { sheetOpen = false },
        )
    }
}
private fun batteryUnrestricted(context: Context): Boolean =
    runCatching {
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)
    }.getOrDefault(false)

@Composable
private fun BatteryStep(next: () -> Unit) {
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
            TextButton(onClick = next) { Text("Skip for now") }
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
