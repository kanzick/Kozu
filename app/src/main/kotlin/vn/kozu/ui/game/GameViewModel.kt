package vn.kozu.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import vn.kozu.data.local.DownloadedSet
import vn.kozu.di.AppContainer
import vn.kozu.game.OsuBeatmapParser
import vn.kozu.game.OsuDifficulty
import vn.kozu.ui.containerFactory

/** ViewModel managing rhythm game screen beatmaps and difficulty parsing */
class GameViewModel(private val container: AppContainer) : ViewModel() {

    val sets: StateFlow<List<DownloadedSet>> = container.downloadsStore.sets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun loadDifficulties(id: Long): List<OsuDifficulty> = withContext(Dispatchers.IO) {
        val folder = container.downloadsStore.folder(id)
        folder.walkTopDown()
            .filter { it.isFile && it.extension.equals("osu", ignoreCase = true) }
            .mapNotNull { file ->
                try {
                    OsuBeatmapParser.parse(file.name, file.readText())
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.objectCount }
            .toList()
    }

    companion object {
        val Factory = containerFactory { GameViewModel(it) }
    }
}
