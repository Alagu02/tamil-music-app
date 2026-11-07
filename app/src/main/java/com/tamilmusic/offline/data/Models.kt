package com.tamilmusic.offline.data

data class Song(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationMs: Long,
    val dateAdded: Long,
    val artUri: String?
)

/** Imported (user picked) songs have ids in -1..-999_999_999. Bundled asset samples are <= -1_000_000_000. */
val Song.isImported: Boolean get() = id < 0 && id > -1_000_000_000L
