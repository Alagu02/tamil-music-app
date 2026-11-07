package com.tamilmusic.offline

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tamilmusic.offline.data.*
import com.tamilmusic.offline.player.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class SortOrder(val label: String) {
    TITLE("Title"), ARTIST("Artist"), RECENT("Recently added"), DURATION("Duration")
}

enum class ThemeMode(val label: String) { SYSTEM("System"), DARK("Dark"), LIGHT("Light") }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository(app)
    private val dao = MusicDatabase.get(app).dao()
    private val prefs = app.getSharedPreferences("tm_prefs", Context.MODE_PRIVATE)
    val pm: PlaybackManager = PlaybackManager.get(app)

    private val device = MutableStateFlow<List<Song>>(emptyList())
    private val bundled = repo.scanAssets()
    val loading = MutableStateFlow(false)

    val songs: StateFlow<List<Song>> =
        combine(device, dao.imported()) { d, i -> bundled + i.map { it.toSong() } + d }
            .stateIn(viewModelScope, SharingStarted.Eagerly, bundled)

    val query = MutableStateFlow("")
    val sort = MutableStateFlow(SortOrder.TITLE)

    val visibleSongs: StateFlow<List<Song>> = combine(songs, query, sort) { list, q, order ->
        val f = if (q.isBlank()) list else {
            val k = q.trim().lowercase()
            list.filter {
                it.title.lowercase().contains(k) || it.artist.lowercase().contains(k) ||
                    it.album.lowercase().contains(k) || it.genre.lowercase().contains(k)
            }
        }
        when (order) {
            SortOrder.TITLE -> f.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> f.sortedBy { it.artist.lowercase() }
            SortOrder.RECENT -> f.sortedByDescending { it.dateAdded }
            SortOrder.DURATION -> f.sortedBy { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteIds: StateFlow<Set<Long>> =
        dao.favoriteIds().map { it.toSet() }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val playlists: StateFlow<List<PlaylistEntity>> =
        dao.playlists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recents: StateFlow<List<Song>> = combine(dao.recentIds(), songs) { ids, all ->
        val m = all.associateBy { it.id }
        ids.mapNotNull { m[it] }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val current: StateFlow<Song?> = combine(pm.state, songs) { st, all ->
        all.firstOrNull { it.id.toString() == st.mediaId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString("theme", "DARK") ?: "DARK") }.getOrDefault(ThemeMode.DARK)
    )

    init {
        // Track "recently played"
        viewModelScope.launch {
            pm.state.map { it.mediaId }.distinctUntilChanged().filterNotNull().collect {
                it.toLongOrNull()?.let { id -> dao.upsertRecent(RecentEntity(id, System.currentTimeMillis())) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            device.value = repo.scanDevice()
            loading.value = false
        }
    }

    fun importUris(uris: List<Uri>) {
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            uris.forEach { u ->
                runCatching { ctx.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                repo.readMetadata(u)?.let { dao.insertImported(it) }
            }
        }
    }

    fun removeImported(song: Song) {
        if (song.isImported) viewModelScope.launch { dao.deleteImported(-song.id) }
    }

    // ---- playback ----
    fun play(list: List<Song>, index: Int) = pm.playQueue(list, index)
    fun shuffleAll(list: List<Song>) {
        if (list.isNotEmpty()) pm.playQueue(list, Random.nextInt(list.size), true)
    }

    // ---- favourites ----
    fun toggleFav(id: Long) {
        viewModelScope.launch {
            if (dao.isFav(id) > 0) dao.removeFav(id) else dao.addFav(FavoriteEntity(id))
        }
    }

    // ---- playlists ----
    fun createPlaylist(name: String, addSongId: Long? = null) {
        viewModelScope.launch {
            val n = name.trim()
            if (n.isEmpty()) return@launch
            val id = dao.insertPlaylist(PlaylistEntity(name = n))
            if (addSongId != null) dao.addToPlaylist(PlaylistSongEntity(id, addSongId, System.currentTimeMillis()))
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { dao.clearPlaylist(id); dao.deletePlaylist(id) }
    }

    fun addToPlaylist(pid: Long, sid: Long) {
        viewModelScope.launch { dao.addToPlaylist(PlaylistSongEntity(pid, sid, System.currentTimeMillis())) }
    }

    fun removeFromPlaylist(pid: Long, sid: Long) {
        viewModelScope.launch { dao.removeFromPlaylist(pid, sid) }
    }

    fun playlistSongs(id: Long): Flow<List<Song>> = combine(dao.playlistSongIds(id), songs) { ids, all ->
        val m = all.associateBy { it.id }
        ids.mapNotNull { m[it] }
    }

    // ---- lyrics (user supplied, stored locally) ----
    fun lyrics(id: Long): Flow<String?> = dao.lyrics(id)
    fun saveLyrics(id: Long, text: String) {
        viewModelScope.launch {
            if (text.isBlank()) dao.deleteLyrics(id) else dao.saveLyrics(LyricsEntity(id, text))
        }
    }

    // ---- theme ----
    fun setTheme(t: ThemeMode) {
        theme.value = t
        prefs.edit().putString("theme", t.name).apply()
    }
}
