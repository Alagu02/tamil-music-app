package com.tamilmusic.offline.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "imported", indices = [Index(value = ["uri"], unique = true)])
data class ImportedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationMs: Long,
    val dateAdded: Long
) {
    fun toSong() = Song(-id, uri, title, artist, album, genre, durationMs, dateAdded, null)
}

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val songId: Long)

@Entity(tableName = "playlists")
data class PlaylistEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(val playlistId: Long, val songId: Long, val addedAt: Long)

@Entity(tableName = "recents")
data class RecentEntity(@PrimaryKey val songId: Long, val playedAt: Long)

@Entity(tableName = "lyrics")
data class LyricsEntity(@PrimaryKey val songId: Long, val text: String)

@Dao
interface MusicDao {
    @Query("SELECT * FROM imported ORDER BY id DESC") fun imported(): Flow<List<ImportedEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertImported(e: ImportedEntity): Long
    @Query("DELETE FROM imported WHERE id = :id") suspend fun deleteImported(id: Long)

    @Query("SELECT songId FROM favorites") fun favoriteIds(): Flow<List<Long>>
    @Query("SELECT COUNT(*) FROM favorites WHERE songId = :id") suspend fun isFav(id: Long): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addFav(f: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE songId = :id") suspend fun removeFav(id: Long)

    @Query("SELECT * FROM playlists ORDER BY id DESC") fun playlists(): Flow<List<PlaylistEntity>>
    @Insert suspend fun insertPlaylist(p: PlaylistEntity): Long
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun deletePlaylist(id: Long)
    @Query("DELETE FROM playlist_songs WHERE playlistId = :id") suspend fun clearPlaylist(id: Long)
    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :pid ORDER BY addedAt") fun playlistSongIds(pid: Long): Flow<List<Long>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addToPlaylist(e: PlaylistSongEntity)
    @Query("DELETE FROM playlist_songs WHERE playlistId = :pid AND songId = :sid") suspend fun removeFromPlaylist(pid: Long, sid: Long)

    @Query("SELECT songId FROM recents ORDER BY playedAt DESC LIMIT 20") fun recentIds(): Flow<List<Long>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertRecent(r: RecentEntity)

    @Query("SELECT text FROM lyrics WHERE songId = :id") fun lyrics(id: Long): Flow<String?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveLyrics(l: LyricsEntity)
    @Query("DELETE FROM lyrics WHERE songId = :id") suspend fun deleteLyrics(id: Long)
}

@Database(
    entities = [ImportedEntity::class, FavoriteEntity::class, PlaylistEntity::class,
        PlaylistSongEntity::class, RecentEntity::class, LyricsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun dao(): MusicDao

    companion object {
        @Volatile private var inst: MusicDatabase? = null
        fun get(c: Context): MusicDatabase = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, MusicDatabase::class.java, "tamil_music.db")
                .build().also { inst = it }
        }
    }
}
