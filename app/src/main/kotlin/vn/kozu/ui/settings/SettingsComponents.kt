package vn.kozu.ui.settings

import vn.kozu.ui.i18n.kozuUiString
import vn.kozu.ui.components.KozuSheetHandle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import vn.kozu.ui.i18n.KozuText as Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.Tune
import vn.kozu.ui.components.bounceClick
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsPageScaffold(title = title, onBack = onBack, modifier = modifier, content = content)
}

@Composable
fun SettingsEntry(
    icon: ImageVector,
    container: Color,
    content: Color,
    title: String,
    subtitle: String,
    avatarUrl: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(pressedScale = 0.98f, onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = container) {
                Box(contentAlignment = Alignment.Center) {
                    if (avatarUrl.isNullOrBlank()) {
                        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(26.dp))
                    } else {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = kozuUiString("$title avatar"),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String?, items: List<@Composable () -> Unit>) {
    Column {
        if (title != null) SettingsSectionTitle(title)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { index, item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = groupShape(index, items.size),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) { item() }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    subtitle: String? = null,
    leading: (@Composable (T) -> Unit)? = null,
    icon: ImageVector = Icons.Rounded.Tune
) {
    var showOptions by rememberSaveable { mutableStateOf(false) }
    Surface(
        onClick = { showOptions = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        leading?.invoke(selected)
                        Text(
                            label(selected),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showOptions) {
        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
        ModalBottomSheet(
            onDismissRequest = { showOptions = false },
            dragHandle = { KozuSheetHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = sheetState
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
                    Surface(
                        onClick = {
                            onSelect(option)
                            showOptions = false
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = if (option == selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            leading?.invoke(option)
                            Text(
                                label(option),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = if (leading == null) 0.dp else 12.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (option == selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (option == selected) {
                                    androidx.compose.ui.text.font.FontWeight.SemiBold
                                } else {
                                    androidx.compose.ui.text.font.FontWeight.Normal
                                }
                            )
                            if (option == selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = kozuUiString("Selected"),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    if (index < options.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    Column(
        Modifier
            .fillMaxWidth()
            .bounceClick(pressedScale = 0.98f, onClick = onClick)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PrimaryPill(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.bounceClick(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 13.dp),
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
fun SecondaryPill(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.bounceClick(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = localContentColorAlpha()
            )
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun localContentColorAlpha(): Color =
    androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.7f)

fun formatInt(value: Number): String = "%,d".format(value.toLong())

fun formatDurationLong(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${formatInt(hours)}h ${minutes}m" else "${minutes}m"
}

fun formatCompact(value: Long): String = when {
    value >= 1_000_000_000L -> "%.2fB".format(value / 1_000_000_000.0)
    value >= 1_000_000L -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000L -> "%.1fK".format(value / 1_000.0)
    else -> value.toString()
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes > 0L -> "${bytes / 1024 + 1} KB"
    else -> "0 MB"
}
