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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import vn.kozu.domain.model.StartTab
import vn.kozu.domain.model.ThemeMode
import vn.kozu.domain.model.ThemePreset
import vn.kozu.ui.navigation.KozuTab
import vn.kozu.ui.theme.KozuMotion
import vn.kozu.ui.components.FitText
import vn.kozu.ui.i18n.KozuText as Text
import vn.kozu.ui.player.PlayerDesignSheet
import vn.kozu.ui.settings.SwatchDots
import vn.kozu.ui.settings.rememberPresetSwatches

/** Lets Settings > About reopen the wizard; provided by the app shell. */
val LocalReplayOnboarding = androidx.compose.runtime.compositionLocalOf<(() -> Unit)?> { null }

/** Ten numbered setup steps plus the welcome page. The radius editor is a modal-like page. */
private const val STEP_COUNT = 10
private const val MAX_BACKUP_BYTES = 32 * 1024 * 1024

@Composable
fun OnboardingScreen(
    container: AppContainer,
    settings: AppSettings,
    onFinish: () -> Unit,
) {
    OnboardingContent(container = container, settings = settings, onFinish = onFinish)
}

@Composable
private fun OnboardingContent(
    container: AppContainer,
    settings: AppSettings,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }

    val finish = {
        container.settings.onboardingCompleted = true
        onFinish()
    }
    // Permissions are optional during setup; every step can advance like Pixel Music.
    val nextEnabled = true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = step,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val forward = targetState > initialState
                        val enter =
                            slideInHorizontally(
                                animationSpec = tween(360, easing = FastOutSlowInEasing),
                            ) { width -> if (forward) width / 6 else -width / 6 } +
                                fadeIn(tween(250))
                        val exit =
                            slideOutHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                            ) { width -> if (forward) -width / 8 else width / 8 } +
                                fadeOut(tween(190))
                        enter togetherWith exit
                    },
                    label = "pixelStyleSetupPager",
                ) { current ->
                    when (current) {
                        0 -> WelcomeStep()
                        1 -> NotificationsStep(next = { step++ })
                        2 -> BackupStep(container, next = { step++ })
                        3 -> ThemeStep(container, settings)
                        4 -> PaletteStep(container, settings)
                        5 -> LibraryStep(container, settings)
                        6 -> NavigationStep(
                            container = container,
                            settings = settings,
                        )
                        7 -> PlayerStep(container, settings)
                        8 -> StartScreenStep(container, settings)
                        9 -> BatteryStep(next = { step++ })
                        else -> DoneStep()
                    }
                }
            }
            BottomBar(
                step = step,
                enabled = nextEnabled,
                onNext = {
                    if (step >= STEP_COUNT) finish() else step++
                },
            )
        }
    }
}

// ───────────────────────────── wizard footer ─────────────────────────────

@Composable
private fun BottomBar(
    step: Int,
    enabled: Boolean,
    onNext: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val isWelcome = step == 0
    val isLast = step >= STEP_COUNT
    val shapeIndex = step % 3
    val targetCorners = when (shapeIndex) {
        0 -> listOf(50f, 50f, 50f, 50f)
        1 -> listOf(26f, 26f, 26f, 26f)
        else -> listOf(18f, 50f, 18f, 50f)
    }
    val topStart by animateDpAsState(targetCorners[0].dp, tween(600, easing = FastOutSlowInEasing), label = "wizardFabTopStart")
    val topEnd by animateDpAsState(targetCorners[1].dp, tween(600, easing = FastOutSlowInEasing), label = "wizardFabTopEnd")
    val bottomStart by animateDpAsState(targetCorners[2].dp, tween(600, easing = FastOutSlowInEasing), label = "wizardFabBottomStart")
    val bottomEnd by animateDpAsState(targetCorners[3].dp, tween(600, easing = FastOutSlowInEasing), label = "wizardFabBottomEnd")
    val rotation by animateFloatAsState(
        targetValue = step * 360f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "wizardFabRotation",
    )
    val actionEnabled = enabled || isLast || isWelcome
    val buttonColor = if (actionEnabled) colors.primaryContainer else colors.surfaceContainerHighest
    val iconColor = if (actionEnabled) colors.onPrimaryContainer else colors.onSurfaceVariant.copy(alpha = 0.65f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        color = colors.surfaceContainer,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 28.dp, end = 18.dp, top = 18.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isWelcome) "Let's Go!" else "Step $step of $STEP_COUNT",
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                style = if (isWelcome) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                fontWeight = if (isWelcome) FontWeight.Bold else FontWeight.Medium,
                color = if (isWelcome) colors.primary else colors.onSurfaceVariant,
                maxLines = 1,
            )

            FloatingActionButton(
                onClick = { if (actionEnabled) onNext() },
                shape = RoundedCornerShape(
                    topStart = topStart,
                    topEnd = topEnd,
                    bottomStart = bottomStart,
                    bottomEnd = bottomEnd,
                ),
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                containerColor = buttonColor,
                contentColor = iconColor,
                modifier = Modifier
                    .size(88.dp)
                    .rotate(rotation)
                    .then(if (actionEnabled) Modifier else Modifier.semantics { disabled() }),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.rotate(-rotation)) {
                    AnimatedContent(
                        targetState = !isLast,
                        transitionSpec = {
                            (fadeIn(tween(220, delayMillis = 70)) + scaleIn(initialScale = 0.9f, animationSpec = tween(220, delayMillis = 70))) togetherWith
                                (fadeOut(tween(90)) + scaleOut(targetScale = 0.9f, animationSpec = tween(90)))
                        },
                        label = "wizardFabIcon",
                    ) { hasNext ->
                        Icon(
                            imageVector = if (hasNext) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Check,
                            contentDescription = if (hasNext) "Next step" else "Finish setup",
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────────── shared setup visuals ─────────────────────────────

@Composable
private fun SetupHeading(
    title: String,
    subtitle: String? = null,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp, lineHeight = 38.sp),
            fontWeight = FontWeight.Bold,
            textAlign = if (alignment == Alignment.Start) TextAlign.Start else TextAlign.Center,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (alignment == Alignment.Start) TextAlign.Start else TextAlign.Center,
            )
        }
    }
}

/** Static layered icon collage. Page motion is reserved for the wizard pager and Next button. */
@Composable
private fun PermissionCollage(
    icons: List<ImageVector>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 220.dp,
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(height),
        contentAlignment = Alignment.Center,
    ) {
        val unit = minOf(200.dp, maxHeight)
        val centerSize = unit * 0.8f

        // The central tile and four surrounding tiles follow PixelMusic's collage geometry.
        Surface(
            modifier = Modifier.size(centerSize).rotate(-15f),
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icons.getOrElse(0) { Icons.Rounded.MusicNote },
                    contentDescription = null,
                    tint = colors.secondary,
                    modifier = Modifier.size(centerSize * 0.46f),
                )
            }
        }

        if (icons.size > 1) {
            FloatingIcon(
                icon = icons[1],
                modifier = Modifier.align(Alignment.TopStart)
                    .offset(x = maxWidth * 0.05f, y = maxHeight * 0.05f)
                    .size(unit * 0.4f),
                background = colors.surfaceContainerHigh,
                tint = colors.onSurface.copy(alpha = 0.8f),
                rotation = 15f,
                shape = CircleShape,
            )
        }
        if (icons.size > 2) {
            FloatingIcon(
                icon = icons[2],
                modifier = Modifier.align(Alignment.BottomEnd)
                    .offset(x = -(maxWidth * 0.05f), y = -(maxHeight * 0.05f))
                    .size(unit * 0.4f),
                background = colors.surfaceContainerHigh,
                tint = colors.primary,
                rotation = 5f,
                shape = CircleShape,
            )
        }
        if (icons.size > 3) {
            FloatingIcon(
                icon = icons[3],
                modifier = Modifier.align(Alignment.TopEnd)
                    .offset(x = -(maxWidth * 0.10f), y = maxHeight * 0.10f)
                    .size(unit * 0.5f),
                background = colors.surfaceContainerHigh,
                tint = colors.onSurface.copy(alpha = 0.5f),
                rotation = -20f,
                shape = RoundedCornerShape(20.dp),
            )
        }
        if (icons.size > 4) {
            FloatingIcon(
                icon = icons[4],
                modifier = Modifier.align(Alignment.BottomStart)
                    .offset(x = maxWidth * 0.10f, y = -(maxHeight * 0.10f))
                    .size(unit * 0.35f),
                background = colors.surfaceContainerHigh,
                tint = colors.tertiary,
                rotation = 10f,
                shape = RoundedStarShape,
            )
        }
    }
}

private val RoundedStarShape = GenericShape { size, _ ->
    val count = 16
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val outerRadius = minOf(size.width, size.height) / 2f
    val innerRadius = outerRadius * 0.45f
    val vertices = (0 until count).map { index ->
        val angle = -Math.PI.toFloat() / 2f + index * Math.PI.toFloat() / 8f
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        androidx.compose.ui.geometry.Offset(
            x = centerX + kotlin.math.cos(angle) * radius,
            y = centerY + kotlin.math.sin(angle) * radius,
        )
    }
    val smooth = 0.18f
    fun toward(from: androidx.compose.ui.geometry.Offset, to: androidx.compose.ui.geometry.Offset) =
        androidx.compose.ui.geometry.Offset(
            from.x + (to.x - from.x) * smooth,
            from.y + (to.y - from.y) * smooth,
        )
    val firstEntry = toward(vertices[0], vertices[count - 1])
    moveTo(firstEntry.x, firstEntry.y)
    vertices.forEachIndexed { index, vertex ->
        val next = vertices[(index + 1) % count]
        val exit = toward(vertex, next)
        val nextEntry = toward(next, vertex)
        quadraticTo(vertex.x, vertex.y, exit.x, exit.y)
        lineTo(nextEntry.x, nextEntry.y)
    }
    close()
}

@Composable
private fun FloatingIcon(
    icon: ImageVector,
    modifier: Modifier,
    background: Color,
    tint: Color,
    rotation: Float,
    shape: Shape,
) {
    Surface(modifier = modifier.rotate(rotation), shape = shape, color = background) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(16.dp).fillMaxSize())
        }
    }
}

@Composable
private fun PermissionStepLayout(
    title: String,
    description: String,
    icons: List<ImageVector>,
    actionText: String,
    actionEnabled: Boolean = true,
    granted: Boolean = false,
    onAction: () -> Unit,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            SetupHeading(title, description)
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            PermissionCollage(icons = icons, height = 230.dp)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onAction,
                enabled = actionEnabled,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 30.dp, vertical = 15.dp),
                shape = CircleShape,
            ) {
                if (granted) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Text(actionText, style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Text(actionText, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
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
    val background = if (selected) colors.primaryContainer else colors.surfaceContainer
    val tile = if (selected) colors.primary else colors.surfaceContainerHighest
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(46.dp), shape = RoundedCornerShape(16.dp), color = tile) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = if (selected) colors.onPrimary else colors.onSurfaceVariant, modifier = Modifier.size(23.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (badge != null) {
                    Surface(shape = CircleShape, color = if (selected) colors.primary else colors.primaryContainer, contentColor = if (selected) colors.onPrimary else colors.onPrimaryContainer) {
                        Text(badge, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                    }
                }
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Surface(shape = CircleShape, color = if (selected) colors.primary else Color.Transparent, modifier = Modifier.size(28.dp).then(if (!selected) Modifier.border(2.dp, colors.outline, CircleShape) else Modifier)) {
                Box(contentAlignment = Alignment.Center) {
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = colors.onPrimary, modifier = Modifier.size(18.dp))
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
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun SkipHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// ───────────────────────────── setup steps ─────────────────────────────

@Composable
private fun WelcomeStep() {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val version = remember { UpdateChecker.installedVersion(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 34.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp, lineHeight = 44.sp),
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        WelcomeMusicArtwork(
            modifier = Modifier.fillMaxWidth().height(280.dp),
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Kozu",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp, lineHeight = 66.sp),
            fontWeight = FontWeight.ExtraBold,
            color = colors.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "v$version",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(42.dp))

        Text(
            text = "Let's get everything set up for you.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun WelcomeMusicArtwork(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val tileSize = minOf(88.dp, maxHeight * 0.34f)
        val discSize = minOf(224.dp, maxHeight * 0.82f)

        Canvas(Modifier.size(discSize)) {
            val path = androidx.compose.ui.graphics.Path()
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = size.minDimension * 0.48f
            val steps = 120
            for (index in 0..steps) {
                val angle = (index.toFloat() / steps) * (2f * Math.PI.toFloat())
                val ripple = 0.93f + 0.07f * kotlin.math.cos(angle * 7f + 0.35f)
                val x = centerX + radius * ripple * kotlin.math.cos(angle)
                val y = centerY + radius * ripple * kotlin.math.sin(angle)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, colors.surfaceContainerHigh)
        }

        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(72.dp),
        )

        Surface(
            modifier = Modifier.align(Alignment.TopStart)
                .offset(x = maxWidth * 0.07f, y = maxHeight * 0.13f)
                .size(tileSize)
                .rotate(-10f),
            shape = RoundedCornerShape(27.dp),
            color = colors.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Headphones, null, tint = colors.onSecondaryContainer, modifier = Modifier.size(tileSize * 0.48f))
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomEnd)
                .offset(x = -(maxWidth * 0.07f), y = -(maxHeight * 0.12f))
                .size(tileSize)
                .rotate(9f),
            shape = RoundedCornerShape(27.dp),
            color = colors.tertiaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Album, null, tint = colors.onTertiaryContainer, modifier = Modifier.size(tileSize * 0.48f))
            }
        }
    }
}

private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
private fun NotificationsStep(next: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(notificationsGranted(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result || notificationsGranted(context)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = notificationsGranted(context) }

    PermissionStepLayout(
        title = "Notifications",
        description = "Enable notifications to control Kozu from the lock screen and notification shade.",
        icons = listOf(Icons.Rounded.Notifications, Icons.Rounded.MusicNote, Icons.Rounded.LibraryMusic, Icons.Rounded.Album, Icons.Rounded.Headphones),
        actionText = if (granted) "Permission granted" else "Enable Notifications",
        granted = granted,
        actionEnabled = !granted,
        onAction = { if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
    ) {
        if (granted) {
            Text("Notifications are enabled", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        } else {
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
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            result = try {
                val json = withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
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

    PermissionStepLayout(
        title = "Do you have a backup?",
        description = "Restore an existing Kozu backup to bring your settings and library preferences to this device.",
        icons = listOf(Icons.Rounded.Restore, Icons.Rounded.LibraryMusic, Icons.Rounded.Tune, Icons.Rounded.Palette, Icons.Rounded.MusicNote),
        actionText = when {
            busy -> "Restoring…"
            else -> "Import backup"
        },
        actionEnabled = !busy,
        onAction = { launcher.launch(arrayOf("*/*")) },
    ) {
        result?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
        TextButton(onClick = next, enabled = !busy) { Text("Skip / Not now") }
    }
}

@Composable
private fun ThemeStep(
    container: AppContainer,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SetupHeading("App Theme", "Pick the look you want before exploring Kozu.")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OptionCard(Icons.Rounded.DarkMode, "Dark", "A comfortable low-light look.", settings.themeMode == ThemeMode.DARK, { scope.launch { container.settings.setThemeMode(ThemeMode.DARK) } }, "Recommended")
            OptionCard(Icons.Rounded.LightMode, "Light", "A brighter look across the app.", settings.themeMode == ThemeMode.LIGHT, { scope.launch { container.settings.setThemeMode(ThemeMode.LIGHT) } })
            OptionCard(Icons.Rounded.PhoneAndroid, "Follow system", "Match your device appearance.", settings.themeMode == ThemeMode.SYSTEM, { scope.launch { container.settings.setThemeMode(ThemeMode.SYSTEM) } })
            SwitchCard("AMOLED", "Pure black dark theme.", settings.amoled) {
                scope.launch { container.settings.setAmoled(it) }
            }
            SkipHint("You can change this later in Settings  >  Appearance")
        }
    }
}

@Composable
private fun PaletteStep(
    container: AppContainer,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val paletteOptions = remember { ThemePreset.entries.toList() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SetupHeading("Theme preset", "Choose one of Kozu's built-in colour palettes.")
        paletteOptions.forEach { preset ->
            OnboardingThemePresetRow(
                preset = preset,
                accent = settings.accent,
                selected = settings.themePreset == preset,
                onClick = { scope.launch { container.settings.setThemePreset(preset) } },
            )
        }
        SwitchCard("Adapt to artwork", "While music plays, player colours can follow its cover art.", settings.artworkColors) {
            scope.launch { container.settings.setArtworkColors(it) }
        }
        SkipHint("You can change this later in Settings  >  Appearance  >  Colors")
    }
}

@Composable
private fun OnboardingThemePresetRow(
    preset: ThemePreset,
    accent: vn.kozu.domain.model.AccentColor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val unavailable = preset == ThemePreset.DYNAMIC && Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    Surface(
        onClick = onClick,
        enabled = !unavailable,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(preset.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (unavailable) "Requires Android 12 or newer" else preset.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            SwatchDots(rememberPresetSwatches(preset, accent))
            Box(Modifier.width(36.dp), contentAlignment = Alignment.CenterEnd) {
                if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", modifier = Modifier.size(21.dp))
            }
        }
    }
}

@Composable
private fun LibraryStep(
    container: AppContainer,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val compact = settings.libraryNavigationMode == LibraryNavigationMode.COMPACT
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        SetupHeading("Library Layout", "Choose your preferred way to navigate your library.")
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            LibraryPreview(compact = compact)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { scope.launch { container.settings.setLibraryNavigationMode(if (compact) LibraryNavigationMode.TAB_ROW else LibraryNavigationMode.COMPACT) } },
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Compact Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (compact) "Using minimal pill navigation" else "Using a standard tab row", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = compact, onCheckedChange = { enabled -> scope.launch { container.settings.setLibraryNavigationMode(if (enabled) LibraryNavigationMode.COMPACT else LibraryNavigationMode.TAB_ROW) } })
                }
            }
            SkipHint("You can change this later in Settings  >  Appearance  >  Library")
        }
    }
}

@Composable
private fun LibraryPreview(compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp,
        ),
        color = colors.surface,
    ) {
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        colors.primaryContainer.copy(alpha = 0.5f),
                        colors.primaryContainer.copy(alpha = 0.25f),
                        Color.Transparent,
                    ),
                ),
            ),
        ) {
            if (compact) {
                // Match LibraryScreen.SplitTitle: content-sized title + fixed 52dp chevron.
                // Only the heights are equal; the title keeps its natural width.
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Surface(
                        modifier = Modifier.heightIn(min = 52.dp),
                        shape = RoundedCornerShape(
                            topStart = 28.dp,
                            bottomStart = 28.dp,
                            topEnd = 6.dp,
                            bottomEnd = 6.dp,
                        ),
                        color = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(22.dp))
                            FitText(
                                text = "Songs",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.onPrimaryContainer,
                                minScale = 0.7f,
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.size(width = 52.dp, height = 52.dp),
                        shape = RoundedCornerShape(
                            topStart = 6.dp,
                            bottomStart = 6.dp,
                            topEnd = 28.dp,
                            bottomEnd = 28.dp,
                        ),
                        color = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 18.dp, start = 20.dp, end = 20.dp),
                ) {
                    Text(
                        text = "Library",
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.primary,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp),
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf("SONGS", "LIKED", "PLAYLISTS", "DOWNLOADS").forEachIndexed { index, label ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = CircleShape,
                                    color = colors.surfaceContainerLowest,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium,
                                            color = if (index == 0) colors.primary else colors.onSurfaceVariant,
                                            maxLines = 1,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                                        )
                                    }
                                }
                                Box(
                                    Modifier.width(20.dp).height(3.dp).clip(CircleShape)
                                        .background(if (index == 0) colors.primary else Color.Transparent),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationStep(
    container: AppContainer,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val floating = settings.navigationStyle == NavigationStyle.FLOATING
    val compact = settings.compactNavigation
    val selectedStyle = when {
        !floating -> 2
        compact -> 1
        else -> 0
    }
    var radius by remember(settings.navCornerRadius) {
        mutableFloatStateOf(settings.navCornerRadius.toFloat().coerceIn(0f, 32f))
    }
    val radiusSliderState = rememberSliderState(
        value = radius,
        trackRange = 0f..32f,
    )
    // Keep the slider in sync if the saved setting is reloaded while this step is visible.
    LaunchedEffect(settings.navCornerRadius) {
        radiusSliderState.value = settings.navCornerRadius.toFloat().coerceIn(0f, 32f)
    }

    fun selectNavigation(style: Int) {
        scope.launch {
            when (style) {
                0 -> {
                    container.settings.setNavigationStyle(NavigationStyle.FLOATING)
                    container.settings.setCompactNavigation(false)
                }
                1 -> {
                    container.settings.setNavigationStyle(NavigationStyle.FLOATING)
                    container.settings.setCompactNavigation(true)
                }
                else -> {
                    container.settings.setNavigationStyle(NavigationStyle.CLASSIC)
                    container.settings.setCompactNavigation(false)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SetupHeading("App Navigation", "Choose the style of Kozu's bottom navigation.")
        NavPreview(floating = floating, compact = compact, radius = radius)
        if (floating) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Corner Radius",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                radius = 32f
                                radiusSliderState.value = 32f
                                scope.launch { container.settings.setNavCornerRadius(32) }
                            },
                        ) {
                            Text("Reset")
                        }
                        Text(
                            "${radius.toInt()} dp",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        state = radiusSliderState,
                        onValueChange = { value ->
                            val clampedValue = value.coerceIn(0f, 32f)
                            radiusSliderState.value = clampedValue
                            radius = clampedValue
                        },
                        onValueChangeFinished = {
                            scope.launch { container.settings.setNavCornerRadius(radius.toInt()) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Adjust the preview above. Your choice is saved for the floating navigation style.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        OptionCard(
            icon = Icons.Rounded.ViewCarousel,
            title = "Expressive Pill",
            subtitle = "A spacious floating bar with a separate search button.",
            selected = selectedStyle == 0,
            onClick = { selectNavigation(0) },
        )
        OptionCard(
            icon = Icons.Rounded.Tune,
            title = "Classic Pill",
            subtitle = "A compact floating bar with the same detached search action.",
            selected = selectedStyle == 1,
            onClick = { selectNavigation(1) },
        )
        OptionCard(
            icon = Icons.Rounded.PhoneAndroid,
            title = "Full Width Bar",
            subtitle = "Edge-to-edge navigation along the bottom.",
            selected = selectedStyle == 2,
            onClick = { selectNavigation(2) },
        )

        SkipHint("You can change this later in Settings  >  Appearance  >  Navigation")
    }
}

@Composable
private fun NavPreview(
    floating: Boolean,
    compact: Boolean,
    radius: Float,
) {
    val colors = MaterialTheme.colorScheme
    var previewSelected by rememberSaveable { mutableStateOf(0) }
    val barHeight by animateDpAsState(
        targetValue = if (compact) 56.dp else 64.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "wizardNavBarHeight",
    )
    val animatedRadius by animateFloatAsState(
        targetValue = radius.coerceIn(0f, 32f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "wizardNavRadius",
    )
    val searchSize by animateDpAsState(
        targetValue = if (compact) 48.dp else 56.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "wizardNavSearchSize",
    )
    val barShape = RoundedCornerShape((barHeight.value * animatedRadius / 64f).dp)
    val itemShape = RoundedCornerShape(((barHeight.value - 18f) * animatedRadius / 64f).dp)

    Surface(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerHigh,
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(colors.surfaceContainerHighest))
                Box(Modifier.fillMaxWidth(0.72f).height(12.dp).clip(CircleShape).background(colors.surfaceContainerHighest))
                Box(Modifier.fillMaxWidth(0.9f).height(12.dp).clip(CircleShape).background(colors.surfaceContainerHighest))
            }
            if (floating) {
                val tabs = KozuTab.entries.filter { it.inPill }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.weight(1f).height(barHeight),
                        shape = barShape,
                        color = colors.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            tabs.forEachIndexed { index, tab ->
                                val selected = previewSelected == index
                                val itemWeight by animateFloatAsState(
                                    targetValue = if (selected && !compact) 2.4f else 1f,
                                    animationSpec = KozuMotion.spatial(),
                                    label = "wizardNavItemWeight$index",
                                )
                                val itemContainer by animateColorAsState(
                                    targetValue = if (selected) colors.primary else Color.Transparent,
                                    animationSpec = tween(200),
                                    label = "wizardNavItemContainer$index",
                                )
                                val itemContent by animateColorAsState(
                                    targetValue = if (selected) colors.onPrimary else colors.primary,
                                    animationSpec = tween(200),
                                    label = "wizardNavItemContent$index",
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(itemWeight.coerceAtLeast(0.2f))
                                        .height(barHeight - 18.dp)
                                        .clickable { previewSelected = index }
                                        .clip(itemShape)
                                        .background(itemContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (selected) tab.icon else tab.outlinedIcon,
                                            contentDescription = null,
                                            tint = itemContent,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        AnimatedVisibility(
                                            visible = selected && !compact,
                                            enter = fadeIn(tween(160)) + expandHorizontally(KozuMotion.spatial()),
                                            exit = fadeOut(tween(90)) + shrinkHorizontally(tween(160)),
                                        ) {
                                            Text(
                                                text = tab.title,
                                                modifier = Modifier.padding(start = 8.dp),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = itemContent,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val searchSelected = previewSelected == tabs.size
                    val searchContainer by animateColorAsState(
                        targetValue = if (searchSelected) colors.primary else colors.surfaceContainerHigh,
                        animationSpec = tween(200),
                        label = "wizardNavSearchContainer",
                    )
                    val searchContent by animateColorAsState(
                        targetValue = if (searchSelected) colors.onPrimary else colors.primary,
                        animationSpec = tween(200),
                        label = "wizardNavSearchContent",
                    )
                    Surface(
                        modifier = Modifier.size(searchSize).clickable { previewSelected = tabs.size },
                        shape = RoundedCornerShape((searchSize.value * animatedRadius / 64f).dp),
                        color = searchContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (searchSelected) KozuTab.SEARCH.icon else KozuTab.SEARCH.outlinedIcon,
                                contentDescription = null,
                                tint = searchContent,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(78.dp),
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    color = colors.surfaceContainer,
                    tonalElevation = 5.dp,
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 5.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val navItems = listOf(
                            Icons.Rounded.Home to "Home",
                            Icons.Rounded.Explore to "Explore",
                            Icons.Rounded.Search to "Search",
                            Icons.Rounded.LibraryMusic to "Library",
                            Icons.Rounded.SportsEsports to "Game",
                        )
                        navItems.forEachIndexed { index, (icon, label) ->
                            Column(
                                modifier = Modifier.weight(1f).clickable { previewSelected = index },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (index == 0) colors.primary else colors.onSurfaceVariant,
                                    modifier = Modifier.size(21.dp),
                                )
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (index == 0) colors.primary else colors.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerStep(
    container: AppContainer,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val current = settings.appearance.playerLayoutStyle
    var sheetOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SetupHeading("Player Design", "Preview the full player, then choose the layout you like.")
        vn.kozu.ui.player.PlayerDesignLivePreview(
            style = current,
            modifier = Modifier.fillMaxWidth().height(340.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(current.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(current.description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        FilledTonalButton(onClick = { sheetOpen = true }, modifier = Modifier.fillMaxWidth(), shape = CircleShape) {
            Text("Choose player design")
        }
        SkipHint("You can change this later in Settings  >  Appearance  >  Player")
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

@Composable
private fun StartScreenStep(
    container: AppContainer,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SetupHeading("Start Page", "Choose which Kozu page opens first.")
        OptionCard(Icons.Rounded.Home, "Home", "Your personalized overview.", settings.startTab == StartTab.HOME, { scope.launch { container.settings.setStartTab(StartTab.HOME) } }, "Recommended")
        OptionCard(Icons.Rounded.Explore, "Explore", "Discover new beatmaps and music.", settings.startTab == StartTab.EXPLORE, { scope.launch { container.settings.setStartTab(StartTab.EXPLORE) } })
        OptionCard(Icons.Rounded.LibraryMusic, "Library", "Jump straight to your saved collection.", settings.startTab == StartTab.LIBRARY, { scope.launch { container.settings.setStartTab(StartTab.LIBRARY) } })
        OptionCard(Icons.Rounded.SportsEsports, "Game", "Open your rhythm-game workspace.", settings.startTab == StartTab.GAME, { scope.launch { container.settings.setStartTab(StartTab.GAME) } })
        SkipHint("You can change this later in Settings  >  General")
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
    PermissionStepLayout(
        title = "Battery Optimization",
        description = "Some Android devices restrict background apps. Adjust battery settings to help keep playback reliable.",
        icons = listOf(Icons.Rounded.BatteryChargingFull, Icons.Rounded.MusicNote, Icons.Rounded.Headphones, Icons.Rounded.Check, Icons.Rounded.Album),
        actionText = if (unrestricted) "Optimization disabled" else "Open Battery Settings",
        granted = unrestricted,
        actionEnabled = !unrestricted,
        onAction = {
            val intents = listOf(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
            )
            for (intent in intents) if (runCatching { context.startActivity(intent) }.isSuccess) break
        },
    ) {
        if (unrestricted) {
            Text("Kozu is unrestricted", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        } else {
            TextButton(onClick = next) { Text("Skip for now") }
        }
    }
}

@Composable
private fun DoneStep() {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SetupHeading("All Set!", "You're ready to explore Kozu and enjoy your music.")
        Spacer(Modifier.height(24.dp))
        PermissionCollage(
            icons = listOf(Icons.Rounded.Check, Icons.Rounded.Headphones, Icons.Rounded.MusicNote, Icons.Rounded.Album, Icons.Rounded.LibraryMusic),
            height = 230.dp,
        )
    }
}
