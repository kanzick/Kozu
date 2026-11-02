package vn.kozu.game.mania.previewstage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.kozu.game.mania.previewstage.model.SongEntry

class KozuCatalog {
    @Volatile
    private var songs: List<SongEntry> = emptyList()

    @Volatile
    private var index: Map<Long, SongEntry> = emptyMap()

    private val _entries = MutableStateFlow<List<SongEntry>>(emptyList())
    val entries: StateFlow<List<SongEntry>> = _entries.asStateFlow()

    internal fun replace(newSongs: List<SongEntry>) {
        songs = newSongs
        index = newSongs.associateBy { it.songId }
        _entries.value = newSongs
    }

    internal fun upsert(song: SongEntry) {
        val updated = if (index.containsKey(song.songId)) {
            songs.map { if (it.songId == song.songId) song else it }
        } else {
            (songs + song).sortedBy { it.songId }
        }
        songs = updated
        index = updated.associateBy { it.songId }
        _entries.value = updated
    }

    internal fun remove(songId: Long) {
        val updated = songs.filterNot { it.songId == songId }
        songs = updated
        index = updated.associateBy { it.songId }
        _entries.value = updated
    }

    fun all(): List<SongEntry> = songs
    fun find(songId: Long): SongEntry? = index[songId]
}
