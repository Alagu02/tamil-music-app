package com.tamilmusic.offline.ui

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.tamilmusic.offline.MainViewModel
import com.tamilmusic.offline.data.Song
import com.tamilmusic.offline.ui.theme.AccentBrush
import com.tamilmusic.offline.ui.theme.Pink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
private fun rememberEmbeddedArt(song: Song?): State<ImageBitmap?> {
    val ctx = LocalContext.current
    return produceState<ImageBitmap?>(null, song?.id) {
        value = null
        val s = song ?: return@produceState
        if (s.uri.startsWith("asset:")) return@produceState
        value = withContext(Dispatchers.IO) {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(ctx, Uri.parse(s.uri))
                r.embeddedPicture?.let {
                    val o = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeByteArray(it, 0, it.size, o)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            } finally {
                runCatching { r.release() }
            }
        }
    }
}

@Composable
fun PlayerScreen(vm: MainViewModel, onClose: () -> Unit) {
    val song by vm.current.collectAsState()
    val st by vm.pm.state.collectAsState()
    val favs by vm.favoriteIds.collectAsState()
    val pos by rememberPosition(vm.pm)
    val art by rememberEmbeddedArt(song)
    val s = song
    if (s == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val cs = MaterialTheme.colorScheme
    var dragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableFloatStateOf(0f) }
    var showSpeed by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showEq by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    val artScale by animateFloatAsState(if (st.isPlaying) 1f else 0.88f, label = "artScale")
    val dur = st.durationMs.toFloat().coerceAtLeast(1f)
    val sleepOn by vm.pm.sleepEndAt.collectAsState()
    val isFav = s.id in favs

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.35f), cs.background, cs.background))
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Close", modifier = Modifier.size(32.dp)) }
                Text("Now playing", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { showLyrics = true }) { Icon(Icons.Rounded.Notes, "Lyrics") }
            }
            Spacer(Modifier.height(12.dp))

            val a = art
            Box(Modifier.fillMaxWidth(0.85f).aspectRatio(1f).scale(artScale)) {
                if (a != null) {
                    Image(
                        a, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))
                    )
                } else {
                    SongArt(s.artUri, Modifier.fillMaxSize(), corner = 28.dp)
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(s.artist, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.album, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { vm.toggleFav(s.id) }) {
                    Icon(
                        if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite",
                        tint = if (isFav) Pink else cs.onSurfaceVariant, modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Slider(
                value = if (dragging) dragPos else pos.toFloat().coerceIn(0f, dur),
                onValueChange = { dragging = true; dragPos = it },
                onValueChangeFinished = { vm.pm.seekTo(dragPos.toLong()); dragging = false },
                valueRange = 0f..dur
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmt(if (dragging) dragPos.toLong() else pos), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Text(fmt(st.durationMs), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.pm.toggleShuffle() }) {
                    Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (st.shuffle) Pink else cs.onSurfaceVariant)
                }
                IconButton(onClick = { vm.pm.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", modifier = Modifier.size(36.dp))
                }
                Box(
                    Modifier.size(76.dp).clip(CircleShape).background(AccentBrush).clickable { vm.pm.togglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (st.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (st.isPlaying) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(44.dp)
                    )
                }
                IconButton(onClick = { vm.pm.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, "Next", modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { vm.pm.cycleRepeat() }) {
                    Icon(
                        if (st.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat",
                        tint = if (st.repeat == Player.REPEAT_MODE_OFF) cs.onSurfaceVariant else Pink
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cs.surfaceVariant).padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ToolButton(Icons.Rounded.Speed, if (st.speed == 1f) "Speed" else "${st.speed}x", st.speed != 1f) { showSpeed = true }
                ToolButton(Icons.Rounded.Timer, "Sleep", sleepOn > 0) { showSleep = true }
                ToolButton(Icons.Rounded.GraphicEq, "Equalizer", false) { showEq = true }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSpeed) SpeedDialog(vm.pm) { showSpeed = false }
    if (showSleep) SleepDialog(vm.pm) { showSleep = false }
    if (showEq) EqualizerDialog(vm.pm) { showEq = false }
    if (showLyrics) LyricsDialog(s, vm) { showLyrics = false }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = if (active) Pink else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) Pink else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
