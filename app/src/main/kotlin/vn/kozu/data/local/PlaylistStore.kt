package vn.kozu.data.local

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import vn.kozu.data.local.db.KozuDatabase
import vn.kozu.data.local.db.PlaylistEntity
import vn.kozu.data.local.db.SongEntity
import vn.kozu.data.local.db.SongInPlaylistEntity
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.Playlist
import java.util.UUID

/**
 * Playlist backend, stored in a local Room (SQLite) database:
 * playlists + songs + song_in_playlist (junction). Public API is unchanged from the
 * old DataStore/JSON version, so the UI and LibrarySync need no edits.
 */
class PlaylistStore(context: Context) {

    private val db = KozuDatabase.get(context)
    private val dao = db.playlistDao()

    val playlists: Flow<List<Playlist>> = dao.observeRows().map { rows ->
        // groupBy keeps first-seen order, and the query already sorts by playlist then position.
        rows.groupBy { it.playlist.id }.values.map { group ->
            val head = group.first().playlist
            Playlist(
                id = head.id,
                name = head.name,
                tracks = group.mapNotNull { it.song?.toBeatmap() },
                createdAt = head.createdAt
            )
        }
    }.distinctUntilChanged()

    suspend fun create(name: String, firstTrack: Beatmap? = null): String {
        val id = UUID.randomUUID().toString()
        db.withTransaction {
            dao.insertPlaylist(
                PlaylistEntity(
                    id = id,
                    name = name.trim().ifBlank { "New playlist" },
                    createdAt = System.currentTimeMillis()
                )
            )
            if (firstTrack != null) link(id, firstTrack)
        }
        return id
    }

    suspend fun rename(id: String, name: String) {
        val clean = name.trim()
        if (clean.isNotBlank()) dao.rename(id, clean)
    }

    suspend fun delete(id: String) = db.withTransaction {
        dao.deletePlaylist(id)
        dao.deleteOrphanSongs()
    }

    suspend fun addTrack(id: String, beatmap: Beatmap) = db.withTransaction { link(id, beatmap) }

    suspend fun removeTrack(id: String, beatmapId: Long) = db.withTransaction {
        dao.removeLink(id, beatmapId)
        dao.deleteOrphanSongs()
    }

    suspend fun updateMetadata(fresh: Map<Long, Beatmap>) = db.withTransaction {
        val changed = dao.getAllSongs().mapNotNull { old ->
            val newer = fresh[old.id] ?: return@mapNotNull null
            val merged = BeatmapJson.merge(old.toBeatmap(), newer).toEntity()
            if (merged != old) merged else null
        }
        if (changed.isNotEmpty()) dao.upsertSongs(changed)
    }

    suspend fun clear() = db.withTransaction {
        dao.deleteAllPlaylists()
        dao.deleteOrphanSongs()
    }

    /** Must be called inside a transaction. Adds the song to the end of the playlist (no duplicates). */
    private suspend fun link(playlistId: String, beatmap: Beatmap) {
        dao.upsertSong(beatmap.toEntity())
        dao.insertLink(
            SongInPlaylistEntity(
                playlistId = playlistId,
                songId = beatmap.id,
                position = dao.nextPosition(playlistId),
                addedAt = System.currentTimeMillis()
            )
        )
    }

    private fun Beatmap.toEntity() = SongEntity(id, title, artist, mapper, status, updatedAt)
    private fun SongEntity.toBeatmap() = Beatmap(id, title, artist, mapper, status, updatedAt)
}
