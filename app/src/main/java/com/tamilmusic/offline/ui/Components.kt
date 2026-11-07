package com.tamilmusic.offline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.tamilmusic.offline.MainViewModel
import com.tamilmusic.offline.data.Song
import com.tamilmusic.offline.data.isImported
import com.tamilmusic.offline.player.PlaybackManager
import com.tamilmusic.offline.ui.theme.AccentBrush
import com.tamilmusic.offline.ui.theme.Pink
import kotlinx.coroutines.delay

class Actions(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit,
    val importFiles: () -> Unit
)

fun fmt(ms: Long): String {
    val s = ms.coerceAtLeast(0L) / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Current playback position, polled only while the app is visible (saves battery). */
@Composable
fun rememberPosition(pm: PlaybackManager): State<Long> {
    val owner = LocalLifecycleOwner.current
    return produceState(0L, pm) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = pm.player.currentPosition
                delay(500)
            }
        }
    }
}

@Composable
fun SongArt(artUri: String?, modifier: Modifier = Modifier, corner: Dp = 12.dp) {
    Box(
        modifier.clip(RoundedCornerShape(corner)).background(AccentBrush),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.9f))
        if (artUri != null) {
            AsyncImage(
                model = artUri, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun GradientButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(AccentBrush)
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 20.dp, bottom = 10.dp)
    )
}

@Composable
fun EmptyState(actions: Actions) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(AccentBrush), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("No music yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            if (actions.hasPermission) "Import audio files or add music to your device."
            else "Allow access to audio files so your offline music shows up here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        if (!actions.hasPermission) {
            Button(onClick = actions.requestPermission) { Text("Allow music access") }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = actions.importFiles) { Text("Import music files") }
    }
}

@Composable
fun SongRow(
    song: Song,
    vm: MainViewModel,
    current: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val favs by vm.favoriteIds.collectAsState()
    val isFav = song.id in favs
    var menu by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongArt(song.artUri, Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold,
                color = if (current) cs.secondary else cs.onSurface
            )
            Text(
                "${song.artist} • ${fmt(song.durationMs)}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
            )
        }
        IconButton(onClick = { vm.toggleFav(song.id) }) {
            Icon(
                if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite",
                tint = if (isFav) Pink else cs.onSurfaceVariant
            )
        }
        if (trailing != null) trailing() else Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More", tint = cs.onSurfaceVariant) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { menu = false; addDialog = true })
                if (song.isImported) {
                    DropdownMenuItem(text = { Text("Remove from app") }, onClick = { menu = false; vm.removeImported(song) })
                }
            }
        }
    }
    if (addDialog) AddToPlaylistDialog(song, vm) { addDialog = false }
}

fun LazyListScope.songItems(songs: List<Song>, vm: MainViewModel, currentId: Long?) {
    itemsIndexed(songs, key = { _, s -> s.id }) { i, s ->
        SongRow(s, vm, s.id == currentId, onClick = { vm.play(songs, i) })
    }
}
