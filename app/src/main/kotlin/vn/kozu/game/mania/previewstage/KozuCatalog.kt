package vn.kozu.game.mania.previewstage

import vn.kozu.game.mania.previewstage.model.SongEntry

/** In-memory copy of kozu.db, replaced atomically so it can be read safely from the UI thread. */
class KozuCatalog {
    @Volatile
    private var songs: List<SongEntry> = emptyList()

    @Volatile
    private var index: Map<Long, SongEntry> = emptyMap()

    internal fun replace(newSongs: List<SongEntry>) {
        songs = newSongs
        index = newSongs.associateBy { it.songId }
    }

    internal fun upsert(song: SongEntry) {
        val updated = if (index.containsKey(song.songId)) {
            songs.map { if (it.songId == song.songId) song else it }
        } else {
            (songs + song).sortedBy { it.songId }
        }
        songs = updated
        index = updated.associateBy { it.songId }
    }

    internal fun remove(songId: Long) {
        val updated = songs.filterNot { it.songId == songId }
        songs = updated
        index = updated.associateBy { it.songId }
    }

    fun all(): List<SongEntry> = songs
    fun find(songId: Long): SongEntry? = index[songId]
}
