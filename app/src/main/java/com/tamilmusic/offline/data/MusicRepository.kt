package com.tamilmusic.offline.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val ctx: Context) {

    /** Reads all music from MediaStore (needs audio permission). */
    suspend fun scanDevice(): List<Song> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= 29)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val proj = mutableListOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATE_ADDED
        )
        if (Build.VERSION.SDK_INT >= 30) proj += MediaStore.Audio.Media.GENRE

        try {
            ctx.contentResolver.query(
                collection, proj.toTypedArray(),
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000",
                null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val iGenre = if (Build.VERSION.SDK_INT >= 30) c.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
                val artBase = Uri.parse("content://media/external/audio/albumart")
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val artist = c.getString(iArtist).clean("Unknown artist")
                    val album = c.getString(iAlbum).clean("Unknown album")
                    val genre = if (iGenre >= 0) c.getString(iGenre).clean("Unknown genre") else "Unknown genre"
                    list += Song(
                        id = id,
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString(),
                        title = c.getString(iTitle).clean("Unknown title"),
                        artist = artist, album = album, genre = genre,
                        durationMs = c.getLong(iDur),
                        dateAdded = c.getLong(iAdded),
                        artUri = ContentUris.withAppendedId(artBase, c.getLong(iAlbumId)).toString()
                    )
                }
            }
        } catch (_: SecurityException) { /* permission missing */ }
        list
    }

    /** Licensed sample tracks bundled in assets/samples (optional). */
    fun scanAssets(): List<Song> {
        val exts = setOf("mp3", "ogg", "wav", "m4a", "flac", "opus")
        val files = runCatching { ctx.assets.list("samples")?.toList() }.getOrNull().orEmpty()
            .filter { it.substringAfterLast('.', "").lowercase() in exts }.sorted()
        return files.mapIndexed { i, name ->
            Song(
                id = -1_000_000_000L - i,
                uri = "asset:///samples/$name",
                title = name.substringBeforeLast('.').replace('_', ' ').replace('-', ' '),
                artist = "Sample", album = "Sample tracks", genre = "Sample",
                durationMs = assetDuration(name), dateAdded = 0, artUri = null
            )
        }
    }

    private fun assetDuration(name: String): Long = runCatching {
        val r = MediaMetadataRetriever()
        try {
            ctx.assets.openFd("samples/$name").use { r.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally { r.release() }
    }.getOrDefault(0L)

    /** Reads tags of a user-picked audio file. */
    suspend fun readMetadata(uri: Uri): ImportedEntity? = withContext(Dispatchers.IO) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(ctx, uri)
            fun tag(k: Int) = r.extractMetadata(k)
            ImportedEntity(
                uri = uri.toString(),
                title = tag(MediaMetadataRetriever.METADATA_KEY_TITLE).clean(displayName(uri)),
                artist = tag(MediaMetadataRetriever.METADATA_KEY_ARTIST).clean("Unknown artist"),
                album = tag(MediaMetadataRetriever.METADATA_KEY_ALBUM).clean("Unknown album"),
                genre = tag(MediaMetadataRetriever.METADATA_KEY_GENRE).clean("Unknown genre"),
                durationMs = tag(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                dateAdded = System.currentTimeMillis() / 1000
            )
        } catch (e: Exception) {
            null
        } finally {
            runCatching { r.release() }
        }
    }

    private fun displayName(uri: Uri): String =
        runCatching {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull()?.substringBeforeLast('.') ?: "Unknown title"

    private fun String?.clean(fallback: String): String =
        if (this.isNullOrBlank() || this == "<unknown>") fallback else this
}
