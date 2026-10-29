package vn.kozu.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, SongEntity::class, SongInPlaylistEntity::class],
    version = 1,
    exportSchema = true
)
abstract class KozuDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile private var instance: KozuDatabase? = null

        fun get(context: Context): KozuDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                KozuDatabase::class.java,
                "kozu.db"
            ).build().also { instance = it }
        }
    }
}
