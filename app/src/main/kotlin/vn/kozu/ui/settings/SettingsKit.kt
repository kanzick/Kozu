package vn.kozu.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import kotlinx.coroutines.delay
import vn.kozu.ui.components.LocalPerformanceMode
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.rememberKozuHaptics

val LocalSettingsHighlight = compositionLocalOf<String?> { null }

private val LargeCorner = 26.dp
private val SmallCorner = 5.dp

fun groupShape(index: Int, count: Int): Shape {
    val top = if (index == 0) LargeCorner else SmallCorner
    val bottom = if (index == count - 1) LargeCorner else SmallCorner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val maxPx = with(density) { (statusTop + 176.dp).toPx() }
    val minPx = with(density) { (statusTop + 64.dp).toPx() }
    var headerPx by remember(maxPx) { mutableFloatStateOf(maxPx) }
    val scroll = rememberScrollState()

    val connection = remember(minPx, maxPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy > 0f && scroll.value > 0) return Offset.Zero
                val old = headerPx
                val updated = (old + dy).coerceIn(minPx, maxPx)
                headerPx = updated
                return Offset(0f, updated - old)
            }
        }
    }

    LaunchedEffect(scroll.isScrollInProgress, minPx, maxPx) {
        if (scroll.isScrollInProgress) return@LaunchedEffect
        val expand = headerPx > (minPx + maxPx) / 2f && scroll.value == 0
        val target = if (expand) maxPx else minPx
        if (headerPx != target) {
            animate(
                initialValue = headerPx,
                targetValue = target,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) { value, _ -> headerPx = value }
        }
    }

    val fraction = ((maxPx - headerPx) / (maxPx - minPx)).coerceIn(0f, 1f)
    val headerDp = with(density) { headerPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(connection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = headerDp)
                .imePadding()
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 32.dp)
        ) {
            content()
        }

        CollapsingHeader(
            title = title,
            fraction = fraction,
            height = headerDp,
            statusTop = statusTop,
            onBack = onBack
        )
    }
}

@Composable
private fun CollapsingHeader(
    title: String,
    fraction: Float,
    height: Dp,
    statusTop: Dp,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(lerp(colors.surface, colors.surfaceContainer, fraction))
    ) {
        Surface(
            modifier = Modifier
                .padding(start = 16.dp, top = statusTop + 8.dp)
                .size(48.dp)
                .bounceClick(onClick = onBack),
            shape = CircleShape,
            color = colors.surfaceContainerHighest
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = lerpDp(24.dp, 80.dp, fraction),
                    end = 24.dp,
                    bottom = lerpDp(20.dp, 17.dp, fraction)
                )
                .graphicsLayer {
                    val scale = 1f - 0.4f * fraction
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 1f)
                }
        )
    }
}

class SettingsGroupScope {
    internal val entries = mutableListOf<@Composable (Shape) -> Unit>()

    fun custom(content: @Composable (Shape) -> Unit) {
        entries += content
    }

    fun switch(
        setting: Setting,
        checked: Boolean,
        enabled: Boolean = true,
        description: String = setting.description,
        onChange: (Boolean) -> Unit
    ) = custom { shape ->
        SwitchItem(setting, checked, enabled, description, shape, onChange)
    }

    fun <T> dropdown(
        setting: Setting,
        value: T,
        options: List<T>,
        label: (T) -> String,
        description: String = setting.description,
        leading: (@Composable (T) -> Unit)? = null,
        onSelect: (T) -> Unit
    ) = custom { shape ->
        DropdownItem(setting, value, options, label, description, leading, shape, onSelect)
    }

    fun slider(
        setting: Setting,
        value: Float,
        range: ClosedFloatingPointRange<Float>,
        valueLabel: (Float) -> String,
        steps: Int = 0,
        enabled: Boolean = true,
        description: String = setting.description,
        onChange: (Float) -> Unit
    ) = custom { shape ->
        SliderItem(setting, value, range, steps, valueLabel, enabled, description, shape, onChange)
    }

    fun navigation(
        setting: Setting,
        value: String? = null,
        description: String = setting.description,
        onClick: () -> Unit
    ) = custom { shape ->
        NavigationItem(setting, value, description, shape, onClick)
    }

    fun action(
        setting: Setting,
        label: String,
        description: String = setting.description,
        enabled: Boolean = true,
        destructive: Boolean = false,
        onClick: () -> Unit
    ) = custom { shape ->
        ActionItem(setting, label, description, enabled, destructive, shape, onClick)
    }

    fun info(
        icon: ImageVector,
        title: String,
        value: String,
        description: String? = null
    ) = custom { shape ->
        InfoItem(icon, title, value, description, shape)
    }
}

@Composable
fun SettingsSection(
    title: String?,
    modifier: Modifier = Modifier,
    content: SettingsGroupScope.() -> Unit
) {
    val scope = SettingsGroupScope().apply(content)
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            SettingsSectionTitle(title)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val count = scope.entries.size
            scope.entries.forEachIndexed { index, entry ->
                entry(groupShape(index, count))
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 10.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingCard(
    setting: Setting?,
    shape: Shape,
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val highlight = LocalSettingsHighlight.current
    val isTarget = setting != null && highlight == setting.name
    val bring = remember { BringIntoViewRequester() }
    val flash = remember { Animatable(0f) }
    val performance = LocalPerformanceMode.current

    LaunchedEffect(isTarget) {
        if (!isTarget) return@LaunchedEffect
        delay(380)
        bring.bringIntoView()
        if (performance) {
            flash.snapTo(1f)
            delay(900)
            flash.snapTo(0f)
        } else {
            repeat(2) {
                flash.animateTo(1f, tween(260))
                flash.animateTo(0f, tween(480))
            }
        }
    }

    val colors = MaterialTheme.colorScheme
    val container = lerp(colors.surfaceContainer, colors.primaryContainer, flash.value)
    val modifier = Modifier
        .fillMaxWidth()
        .bringIntoViewRequester(bring)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = shape,
            color = container,
            contentColor = colors.onSurface
        ) { content() }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = container,
            contentColor = colors.onSurface
        ) { content() }
    }
}

@Composable
internal fun IconBadge(icon: ImageVector, enabled: Boolean = true) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (enabled) 1f else 0.5f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
internal fun ItemText(
    title: String,
    description: String?,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    extra: (@Composable () -> Unit)? = null
) {
    val alpha = if (enabled) 1f else 0.5f
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
        extra?.invoke()
    }
}

@Composable
private fun SwitchItem(
    setting: Setting,
    checked: Boolean,
    enabled: Boolean,
    description: String,
    shape: Shape,
    onChange: (Boolean) -> Unit
) {
    val haptics = rememberKozuHaptics()
    SettingCard(
        setting = setting,
        shape = shape,
        enabled = enabled,
        onClick = {
            haptics.tick()
            onChange(!checked)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(setting.icon, enabled)
            Spacer(Modifier.width(14.dp))
            ItemText(setting.title, description, enabled, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = {
                    haptics.tick()
                    onChange(it)
                },
                enabled = enabled,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun <T> DropdownItem(
    setting: Setting,
    value: T,
    options: List<T>,
    label: (T) -> String,
    description: String,
    leading: (@Composable (T) -> Unit)?,
    shape: Shape,
    onSelect: (T) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    SettingCard(setting = setting, shape = shape, onClick = { open = true }) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            IconBadge(setting.icon)
            Spacer(Modifier.width(14.dp))
            ItemText(setting.title, description, modifier = Modifier.weight(1f)) {
                Spacer(Modifier.height(10.dp))
                ValueChip(label(value), leading?.let { { it(value) } })
            }
        }
    }
    if (open) {
        OptionSheet(
            title = setting.title,
            options = options,
            selected = value,
            label = label,
            leading = leading,
            onSelect = {
                onSelect(it)
                open = false
            },
            onDismiss = { open = false }
        )
    }
}

@Composable
fun ValueChip(text: String, leading: (@Composable () -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leading?.invoke()
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    leading: (@Composable (T) -> Unit)? = null,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
            options.forEachIndexed { index, option ->
                val isSelected = option == selected
                Surface(
                    onClick = { onSelect(option) },
                    shape = groupShape(index, options.size),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 62.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        leading?.let {
                            it(option)
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            label(option),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                if (index < options.lastIndex) Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun SliderItem(
    setting: Setting,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: (Float) -> String,
    enabled: Boolean,
    description: String,
    shape: Shape,
    onChange: (Float) -> Unit
) {
    var draft by remember(value) { mutableFloatStateOf(value) }
    SettingCard(setting = setting, shape = shape, onClick = null) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(setting.icon, enabled)
                Spacer(Modifier.width(14.dp))
                ItemText(setting.title, description, enabled, Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                ValueChip(valueLabel(draft))
            }
            Slider(
                value = draft,
                onValueChange = { draft = it },
                onValueChangeFinished = { onChange(draft) },
                valueRange = range,
                steps = steps,
                enabled = enabled,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun NavigationItem(
    setting: Setting,
    value: String?,
    description: String,
    shape: Shape,
    onClick: () -> Unit
) {
    SettingCard(setting = setting, shape = shape, onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(setting.icon)
            Spacer(Modifier.width(14.dp))
            ItemText(setting.title, description, modifier = Modifier.weight(1f)) {
                if (value != null) {
                    Spacer(Modifier.height(10.dp))
                    ValueChip(value)
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionItem(
    setting: Setting,
    label: String,
    description: String,
    enabled: Boolean,
    destructive: Boolean,
    shape: Shape,
    onClick: () -> Unit
) {
    SettingCard(setting = setting, shape = shape, onClick = null) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            IconBadge(setting.icon, enabled)
            Spacer(Modifier.width(14.dp))
            ItemText(setting.title, description, enabled, Modifier.weight(1f)) {
                Spacer(Modifier.height(12.dp))
                if (destructive) {
                    OutlinedButton(onClick = onClick, enabled = enabled) {
                        Text(label, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    FilledTonalButton(onClick = onClick, enabled = enabled) { Text(label) }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    description: String?,
    shape: Shape
) {
    SettingCard(setting = null, shape = shape, onClick = null) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon)
            Spacer(Modifier.width(14.dp))
            ItemText(title, description, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp)
            )
        }
    }
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LargeCorner),
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), content = content)
    }
}