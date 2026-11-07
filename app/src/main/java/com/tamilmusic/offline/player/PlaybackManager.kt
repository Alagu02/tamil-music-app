package com.tamilmusic.offline.player

import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tamilmusic.offline.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayState(
    val mediaId: String? = null,
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
    val durationMs: Long = 0L
)

data class EqInfo(val bands: Int, val minLevel: Int, val maxLevel: Int, val freqsHz: List<Int>)

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title).setArtist(artist).setAlbumTitle(album)
            .setArtworkUri(artUri?.let { Uri.parse(it) })
            .build()
    ).build()

/** Single in-process owner of the ExoPlayer; shared by UI and MusicService. */
class PlaybackManager private constructor(private val app: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(app)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()

    private val prefs = app.getSharedPreferences("tm_prefs", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(PlayState())
    val state: StateFlow<PlayState> = _state.asStateFlow()

    // Equalizer / bass
    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    val eqInfo = MutableStateFlow<EqInfo?>(null)
    val eqEnabled = MutableStateFlow(prefs.getBoolean("eq_on", false))
    val eqLevels = MutableStateFlow<List<Int>>(emptyList())
    val bassStrength = MutableStateFlow(prefs.getInt("bass", 0))
    val bassSupported = MutableStateFlow(false)

    // Sleep timer
    val sleepEndAt = MutableStateFlow(0L)
    private val sleepRunnable = Runnable {
        player.pause()
        sleepEndAt.value = 0L
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) = publish()
            override fun onAudioSessionIdChanged(audioSessionId: Int) = attachEffects(audioSessionId)
        })
        attachEffects(player.audioSessionId)
    }

    private fun publish() {
        _state.value = PlayState(
            mediaId = player.currentMediaItem?.mediaId,
            isPlaying = player.isPlaying,
            shuffle = player.shuffleModeEnabled,
            repeat = player.repeatMode,
            speed = player.playbackParameters.speed,
            durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        )
    }

    fun playQueue(songs: List<Song>, index: Int, shuffle: Boolean? = null) {
        if (songs.isEmpty()) return
        if (shuffle != null) player.shuffleModeEnabled = shuffle
        player.setMediaItems(songs.map { it.toMediaItem() }, index.coerceIn(0, songs.lastIndex), 0L)
        player.prepare()
        player.play()
        runCatching { app.startService(Intent(app, MusicService::class.java)) }
    }

    fun togglePlay() { if (player.isPlaying) player.pause() else player.play() }
    fun next() = player.seekToNext()
    fun previous() = player.seekToPrevious()
    fun seekTo(ms: Long) = player.seekTo(ms)
    fun toggleShuffle() { player.shuffleModeEnabled = !player.shuffleModeEnabled }
    fun cycleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
    fun setSpeed(s: Float) = player.setPlaybackSpeed(s)

    // ---------- Sleep timer ----------
    fun setSleepTimer(minutes: Int) {
        handler.removeCallbacks(sleepRunnable)
        if (minutes <= 0) { sleepEndAt.value = 0L; return }
        val ms = minutes * 60_000L
        sleepEndAt.value = System.currentTimeMillis() + ms
        handler.postDelayed(sleepRunnable, ms)
    }

    // ---------- Equalizer ----------
    private fun attachEffects(session: Int) {
        if (session == C.AUDIO_SESSION_ID_UNSET || session == 0) return
        runCatching { eq?.release() }
        runCatching { bass?.release() }
        eq = runCatching { Equalizer(0, session) }.getOrNull()
        bass = runCatching { BassBoost(0, session) }.getOrNull()

        val e = eq
        if (e != null) {
            val range = e.bandLevelRange
            val n = e.numberOfBands.toInt()
            eqInfo.value = EqInfo(n, range[0].toInt(), range[1].toInt(),
                (0 until n).map { e.getCenterFreq(it.toShort()) / 1000 })
            val saved = prefs.getString("eq_levels", null)?.split(",")?.mapNotNull { it.toIntOrNull() }
            eqLevels.value = if (saved != null && saved.size == n) saved else List(n) { 0 }
            applyEq()
        } else {
            eqInfo.value = null
        }
        bassSupported.value = bass?.strengthSupported == true
        applyBass()
    }

    private fun applyEq() {
        val e = eq ?: return
        runCatching {
            e.setEnabled(eqEnabled.value)
            eqLevels.value.forEachIndexed { i, l -> e.setBandLevel(i.toShort(), l.toShort()) }
        }
    }

    private fun applyBass() {
        val b = bass ?: return
        runCatching {
            b.setEnabled(bassStrength.value > 0)
            if (b.strengthSupported) b.setStrength(bassStrength.value.toShort())
        }
    }

    fun setEqEnabled(on: Boolean) {
        eqEnabled.value = on
        prefs.edit().putBoolean("eq_on", on).apply()
        applyEq()
    }

    fun setBandLevel(i: Int, level: Int) {
        val l = eqLevels.value.toMutableList()
        if (i !in l.indices) return
        l[i] = level
        eqLevels.value = l
        prefs.edit().putString("eq_levels", l.joinToString(",")).apply()
        applyEq()
    }

    fun setBass(strength: Int) {
        bassStrength.value = strength
        prefs.edit().putInt("bass", strength).apply()
        applyBass()
    }

    fun resetEq() {
        eqLevels.value = eqLevels.value.map { 0 }
        prefs.edit().putString("eq_levels", eqLevels.value.joinToString(",")).apply()
        setBass(0)
        applyEq()
    }

    companion object {
        @Volatile private var inst: PlaybackManager? = null
        fun get(ctx: Context): PlaybackManager = inst ?: synchronized(this) {
            inst ?: PlaybackManager(ctx.applicationContext).also { inst = it }
        }
    }
}
