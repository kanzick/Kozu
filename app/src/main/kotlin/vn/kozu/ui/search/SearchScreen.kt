package vn.kozu.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import vn.kozu.domain.model.Beatmap

@Composable
fun SearchScreen(
    onBeatmapSelected: (Beatmap) -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(
                    text = "SEARCH THE BEATMAPS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Find your song.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "A favorite track might be one map away.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            label = { Text("Song or artist") },
            placeholder = { Text("Try YOASOBI or Lemon") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChanged("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() })
        )
        Spacer(Modifier.height(11.dp))
        Button(
            onClick = viewModel::search,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = state.query.isNotBlank() && !state.isLoading,
            shape = RoundedCornerShape(17.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Search beatmaps", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(13.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("YOASOBI", "Lemon", "Toby Fox").forEach { suggestion ->
                FilterChip(
                    selected = state.query.equals(suggestion, ignoreCase = true),
                    onClick = {
                        viewModel.onQueryChanged(suggestion)
                        viewModel.search()
                    },
                    label = { Text(suggestion) }
                )
            }
        }
        Spacer(Modifier.height(18.dp))

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Looking through the demo collection…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            state.errorMessage != null -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        modifier = Modifier.padding(18.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            state.results.isNotEmpty() -> {
                Text(
                    text = "MATCHES  ·  ${state.results.size}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(9.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = state.results, key = { it.id }) { beatmap ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onBeatmapSelected(beatmap) },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (beatmap.coverUrl != null) {
                                    AsyncImage(
                                        model = beatmap.coverUrl,
                                        contentDescription = "${beatmap.title} cover",
                                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(27.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = beatmap.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = beatmap.artist,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Mapped by ${beatmap.mapper}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            state.hasSearched -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("No matching beatmaps", fontWeight = FontWeight.Bold)
                        Text("Try another song or artist.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            else -> {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(22.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier.size(58.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Start with a song you love.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Search currently uses sample data. Live osu! API results will be connected in a later step.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
}
