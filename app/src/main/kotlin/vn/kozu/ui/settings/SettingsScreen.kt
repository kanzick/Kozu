package vn.kozu.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import vn.kozu.ui.KozuThemeMode

@Composable
internal fun SettingsScreen(
    themeMode: KozuThemeMode,
    onThemeModeChange: (KozuThemeMode) -> Unit,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Make Kozu feel like yours.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onBackToHome) {
                Icon(Icons.Filled.Home, contentDescription = "Go Home")
            }
        }
        Spacer(Modifier.height(24.dp))
        SettingsGroup(title = "APPEARANCE") {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Choose one theme for every screen in Kozu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KozuThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
                Text(
                    text = when (themeMode) {
                        KozuThemeMode.SYSTEM -> "Follows your phone's appearance setting."
                        KozuThemeMode.LIGHT -> "Light colors are used across Home, Explore, Library and Player."
                        KozuThemeMode.DARK -> "Dark colors are used across Home, Explore, Library and Player."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SettingsRow(
                icon = Icons.Filled.MusicNote,
                title = "Kozu design",
                subtitle = "Material 3 first, with subtle osu!-inspired accents"
            )
            SettingsRow(
                icon = Icons.Filled.FavoriteBorder,
                title = "Album progress color",
                subtitle = "Only the playback progress bar follows cover art"
            )
        }
        Spacer(Modifier.height(16.dp))
        SettingsGroup(title = "PLAYBACK") {
            SettingsRow(
                icon = Icons.Filled.PlayArrow,
                title = "Playback engine",
                subtitle = "Media3 · local demo audio is enabled"
            )
            SettingsRow(
                icon = Icons.Filled.LibraryMusic,
                title = "Your favorites",
                subtitle = "Saved on this device"
            )
        }
        Spacer(Modifier.height(16.dp))
        SettingsGroup(title = "ABOUT KOZU") {
            SettingsRow(
                icon = Icons.Filled.Search,
                title = "osu! beatmap discovery",
                subtitle = "Search currently uses sample data; live API is not connected yet"
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "KOZU · MUSIC, MAPPED DIFFERENTLY",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(9.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
