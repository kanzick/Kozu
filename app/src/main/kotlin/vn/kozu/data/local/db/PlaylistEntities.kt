package vn.kozu.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A user-created playlist. */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long
)

/** Cached beatmap info for every song that is in at least one playlist. */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val mapper: String,
    val status: String,
    val updatedAt: Long
)

/**
 * Junction table: which song is in which playlist, and at what position.
 * Deleting a playlist or a song automatically deletes its links (CASCADE).
 */
@Entity(
    tableName = "song_in_playlist",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("songId")]
)
data class SongInPlaylistEntity(
    val playlistId: String,
    val songId: Long,
    val position: Int,
    val addedAt: Long
)
