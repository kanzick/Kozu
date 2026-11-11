package vn.kozu.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import vn.kozu.data.update.UpdateChecker
import vn.kozu.domain.model.OsuProfile
import vn.kozu.domain.model.UpdateChannel
import vn.kozu.R
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.scrollReveal

private data class SettingsGroup(
    val titleRes: Int,
    val pages: List<SettingsPage>,
)

private val rootGroups = listOf(
    SettingsGroup(R.string.settings_group_personalize, listOf(SettingsPage.APPEARANCE, SettingsPage.BEHAVIOR, SettingsPage.LANGUAGE)),
    SettingsGroup(
        R.string.settings_group_music,
        listOf(
            SettingsPage.MUSIC,
            SettingsPage.CONTENT,
            SettingsPage.PLAYBACK,
            SettingsPage.EQUALIZER,
            SettingsPage.SOURCES,
            SettingsPage.DOWNLOADS,
        ),
    ),
    SettingsGroup(R.string.settings_group_library_data, listOf(SettingsPage.STATS, SettingsPage.STORAGE, SettingsPage.BACKUP)),
    SettingsGroup(R.string.settings_group_system, listOf(SettingsPage.DEVICE, SettingsPage.DEVELOPER)),
    SettingsGroup(R.string.settings_group_app, listOf(SettingsPage.UPDATES, SettingsPage.ABOUT)),
)

private fun parentOf(page: SettingsPage): SettingsPage = when (page) {
    SettingsPage.LICENSES -> SettingsPage.ABOUT
    SettingsPage.TYPOGRAPHY, SettingsPage.PLAYER_STYLE -> SettingsPage.APPEARANCE
    else -> SettingsPage.ROOT
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: SettingsPage = SettingsPage.ROOT,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    var page by rememberSaveable(initialPage) { mutableStateOf(initialPage) }
    val pageStateHolder = rememberSaveableStateHolder()
    var highlight by rememberSaveable { mutableStateOf<String?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var proxyOpen by rememberSaveable { mutableStateOf(false) }
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { keyboard?.hide() }

    BackHandler(enabled = true) {
        when {
            proxyOpen -> proxyOpen = false
            searchOpen -> searchOpen = false
            page != SettingsPage.ROOT -> {
                page = parentOf(page)
                highlight = null
            }
            else -> onBack()
        }
    }

    val close = {
        page = parentOf(page)
        highlight = null
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        CompositionLocalProvider(LocalSettingsHighlight provides highlight) {
            AnimatedContent(
                targetState = page,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val forward = (initialState == SettingsPage.ROOT && targetState != SettingsPage.ROOT) ||
                        targetState == SettingsPage.LICENSES ||
                        targetState == SettingsPage.TYPOGRAPHY ||
                        targetState == SettingsPage.PLAYER_STYLE
                    if (forward) {
                        (slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 10 } +
                            fadeIn(tween(180))) togetherWith
                            (slideOutHorizontally(tween(160, easing = FastOutSlowInEasing)) { -it / 20 } +
                                fadeOut(tween(120)))
                    } else {
                        (slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 20 } +
                            fadeIn(tween(180))) togetherWith
                            (slideOutHorizontally(tween(160, easing = FastOutSlowInEasing)) { it / 10 } +
                                fadeOut(tween(120)))
                    }
                },
                label = "settingsPages"
            ) { current ->
                pageStateHolder.SaveableStateProvider(current.name) {
                    when (current) {
                        SettingsPage.ROOT -> SettingsRoot(
                            profile = profile,
                            loggedIn = loggedIn,
                            updateChannel = settings.updateChannel,
                            onBack = onBack,
                            onOpen = { page = it },
                            onSearch = { searchOpen = true }
                        )
                        SettingsPage.ACCOUNT -> AccountPage(viewModel, close)
                        SettingsPage.MUSIC -> MusicPage(viewModel, close)
                        SettingsPage.CONTENT -> ContentPage(viewModel, { proxyOpen = true }, close)
                        SettingsPage.LANGUAGE -> LanguagePage(close)
                        SettingsPage.APPEARANCE -> AppearancePage(
                            viewModel,
                            onOpenTypography = { page = SettingsPage.TYPOGRAPHY },
                            onOpenPlayerStyle = { page = SettingsPage.PLAYER_STYLE },
                            onBack = close
                        )
                        SettingsPage.TYPOGRAPHY -> TypographyPage(viewModel, close)
                        SettingsPage.PLAYER_STYLE -> PlayerStylePage(viewModel, close)
                        SettingsPage.PLAYBACK -> PlaybackPage(viewModel, { page = SettingsPage.EQUALIZER }, close)
                        SettingsPage.BEHAVIOR -> BehaviorPage(viewModel, close)
                        SettingsPage.BACKUP -> BackupPage(viewModel, close)
                        SettingsPage.DEVELOPER -> DeveloperPage(viewModel, close)
                        SettingsPage.STORAGE -> StoragePage(viewModel, close)
                        SettingsPage.EQUALIZER -> EqualizerPage(viewModel, close)
                        SettingsPage.DEVICE -> DevicePage(viewModel, close)
                        SettingsPage.DOWNLOADS -> OfflinePage(viewModel, close)
                        SettingsPage.SOURCES -> SourcesPage(viewModel, close)
                        SettingsPage.STATS -> StatsPage(viewModel, close)
                        SettingsPage.UPDATES -> UpdatesPage(viewModel, close)
                        SettingsPage.ABOUT -> AboutPage({ page = SettingsPage.LICENSES }, close)
                        SettingsPage.LICENSES -> LicensesPage(close)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = searchOpen,
            enter = fadeIn(tween(220)) + slideInVertically(tween(240)) { -it / 8 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(180)) { -it / 8 },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            SettingsSearchOverlay(
                onClose = { searchOpen = false },
                onPick = { setting ->
                    searchOpen = false
                    highlight = setting.name
                    page = setting.page
                }
            )
        }

        if (proxyOpen) {
            ProxySettingsDialog(
                current = settings.proxy,
                onDismiss = { proxyOpen = false },
                onSave = { value ->
                    viewModel.save { setProxy(value) }
                    proxyOpen = false
                }
            )
        }

    }
}

@Composable
private fun SettingsRoot(
    profile: OsuProfile?,
    loggedIn: Boolean,
    updateChannel: UpdateChannel,
    onBack: () -> Unit,
    onOpen: (SettingsPage) -> Unit,
    onSearch: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = remember { UpdateChecker.installedVersion(context) }

    SettingsPageScaffold(title = stringResource(R.string.settings), onBack = onBack) {
        Spacer(Modifier.height(4.dp))
        SearchEntryBar(onClick = onSearch)
        Spacer(Modifier.height(12.dp))
        AccountSummaryCard(profile = profile, loggedIn = loggedIn, onClick = { onOpen(SettingsPage.ACCOUNT) })
        Spacer(Modifier.height(16.dp))
        rootGroups.forEachIndexed { index, group ->
            SettingsGroupCard(
                group = group,
                installedVersion = version,
                updateChannel = updateChannel,
                groupIndex = index,
                onOpen = onOpen,
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SettingsGroupCard(
    group: SettingsGroup,
    installedVersion: String,
    updateChannel: UpdateChannel,
    groupIndex: Int,
    onOpen: (SettingsPage) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val (iconContainer, iconContent) = when (groupIndex % 4) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.secondaryContainer to colors.onSecondaryContainer
        2 -> colors.tertiaryContainer to colors.onTertiaryContainer
        else -> colors.surfaceContainerHighest to colors.onSurface
    }

    Column {
        Text(text = stringResource(group.titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = colors.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = colors.surfaceContainer,
        ) {
            Column {
                group.pages.forEachIndexed { index, page ->
                    val title = page.titleText()
                    val subtitle = when (page) {
                        SettingsPage.UPDATES -> "v$installedVersion · ${updateChannel.label}"
                        else -> page.subtitleText()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scrollReveal()
                            .bounceClick(pressedScale = 0.99f) { onOpen(page) }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = iconContainer,
                            contentColor = iconContent,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(page.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(text = title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                        )
                    }
                    if (index < group.pages.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                            color = colors.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSearchOverlay(onClose: () -> Unit, onPick: (Setting) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val results = remember(query, context) { SettingsSearch.search(context, query) }

    LaunchedEffect(Unit) {
        delay(120)
        focus.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    keyboard?.hide()
                    onClose()
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focus),
                    singleLine = false,
                    minLines = 1,
                    maxLines = 2,
                    placeholder = { Text(stringResource(R.string.search_settings)) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear))
                    }
                }
            }
        }

        if (query.isBlank()) {
            SearchHint()
        } else if (results.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Rounded.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.no_settings_found), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.try_a_different_word_like_theme_crossfade_or_cache),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.navigationBarsPadding().padding(bottom = vn.kozu.ui.components.LocalOverlayBottomInset.current)
            ) {
                items(results, key = { it.name }) { setting ->
                    val index = results.indexOf(setting)
                    val resultTitle = setting.titleText()
                    val resultDescription = setting.descriptionText()
                    val resultPageTitle = setting.page.titleText()
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(pressedScale = 0.985f) {
                                keyboard?.hide()
                                onPick(setting)
                            },
                        shape = groupShape(index, results.size),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBadge(setting.icon)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(resultTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(resultDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(resultPageTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
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
private fun SearchHint() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = colors.onSurfaceVariant.copy(alpha = 0.32f),
        )
        Spacer(Modifier.height(14.dp))
        Text(text = stringResource(R.string.find_a_setting),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant.copy(alpha = 0.58f),
        )
        Spacer(Modifier.height(6.dp))
        Text(text = stringResource(R.string.try_theme_playback_cache_or_downloads),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant.copy(alpha = 0.38f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun SearchEntryBar(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = stringResource(R.string.search_settings),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Text(stringResource(R.string.search_settings),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountSummaryCard(profile: OsuProfile?, loggedIn: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(pressedScale = 0.985f, onClick = onClick),
        shape = RoundedCornerShape(30.dp),
        color = colors.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val avatar = profile?.avatarUrl?.takeIf { loggedIn && it.isNotBlank() }
                    if (avatar != null) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = stringResource(R.string.avatar),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(text = (profile?.username?.firstOrNull() ?: 'G').uppercase(),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = if (loggedIn && profile != null) profile.username else stringResource(R.string.guest),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = stringResource(if (loggedIn) R.string.account_manage_summary else R.string.account_sign_in_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
