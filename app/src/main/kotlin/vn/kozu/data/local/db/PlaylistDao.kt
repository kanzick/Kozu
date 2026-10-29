package vn.kozu.data.local.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * One row per (playlist, song) pair. A playlist with no songs still produces one row
 * with [song] = null (LEFT JOIN), so empty playlists are not lost.
 */
data class PlaylistRow(
    @Embedded val playlist: PlaylistEntity,
    @Embedded(prefix = "song_") val song: SongEntity?
)

@Dao
interface PlaylistDao {

    /** Single query => every emission is one consistent snapshot of playlists and their songs. */
    @Query(
        """
        SELECT p.id AS id, p.name AS name, p.createdAt AS createdAt,
               s.id AS song_id, s.title AS song_title, s.artist AS song_artist,
               s.mapper AS song_mapper, s.status AS song_status, s.updatedAt AS song_updatedAt
        FROM playlists p
        LEFT JOIN song_in_playlist l ON l.playlistId = p.id
        LEFT JOIN songs s ON s.id = l.songId
        ORDER BY p.createdAt ASC, p.id ASC, l.position ASC, l.addedAt ASC
        """
    )
    fun observeRows(): Flow<List<PlaylistRow>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Query("DELETE FROM playlists")
    suspend fun deleteAllPlaylists()

    // @Upsert (UPDATE on conflict), NOT REPLACE: REPLACE deletes the row first, which would
    // CASCADE-delete the song out of every playlist.
    @Upsert
    suspend fun upsertSong(song: SongEntity)

    @Upsert
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("SELECT * FROM songs")
    suspend fun getAllSongs(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: SongInPlaylistEntity): Long

    @Query("DELETE FROM song_in_playlist WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeLink(playlistId: String, songId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM song_in_playlist WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: String): Int

    @Query("DELETE FROM songs WHERE id NOT IN (SELECT songId FROM song_in_playlist)")
    suspend fun deleteOrphanSongs()
}
