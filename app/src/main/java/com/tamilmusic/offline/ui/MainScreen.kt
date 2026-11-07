package com.tamilmusic.offline.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tamilmusic.offline.MainViewModel
import com.tamilmusic.offline.data.Song
import com.tamilmusic.offline.ui.theme.Pink

@Composable
fun MainScreen(vm: MainViewModel, actions: Actions) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var libMode by rememberSaveable { mutableIntStateOf(0) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    val current by vm.current.collectAsState()
    BackHandler(showPlayer) { showPlayer = false }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column {
                    current?.let { MiniPlayer(vm, it) { showPlayer = true } }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        val items = listOf(
                            Triple("Home", Icons.Rounded.Home, 0),
                            Triple("Library", Icons.Rounded.LibraryMusic, 1),
                            Triple("Playlists", Icons.Rounded.Favorite, 2),
                            Triple("Settings", Icons.Rounded.Settings, 3)
                        )
                        items.forEach { (label, icon, idx) ->
                            NavigationBarItem(
                                selected = tab == idx, onClick = { tab = idx },
                                icon = { Icon(icon, label) }, label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad)) {
                Crossfade(targetState = tab, label = "tabs") { t ->
                    when (t) {
                        0 -> HomeScreen(vm, actions, onOpenLibrary = { libMode = it; tab = 1 }, onOpenPlaylists = { tab = 2 })
                        1 -> LibraryScreen(vm, libMode, actions)
                        2 -> PlaylistsScreen(vm)
                        else -> SettingsScreen(vm, actions)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showPlayer,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            PlayerScreen(vm) { showPlayer = false }
        }
    }
}

@Composable
private fun MiniPlayer(vm: MainViewModel, song: Song, onOpen: () -> Unit) {
    val st by vm.pm.state.collectAsState()
    val pos by rememberPosition(vm.pm)
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.padding(horizontal = 10.dp, vertical = 6.dp).clip(RoundedCornerShape(22.dp))
            .background(cs.surfaceVariant).clickable(onClick = onOpen)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SongArt(song.artUri, Modifier.size(44.dp), corner = 12.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            IconButton(onClick = { vm.pm.previous() }) { Icon(Icons.Rounded.SkipPrevious, "Previous") }
            IconButton(onClick = { vm.pm.togglePlay() }) {
                Icon(if (st.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/Pause", tint = Pink, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { vm.pm.next() }) { Icon(Icons.Rounded.SkipNext, "Next") }
        }
        val p = if (st.durationMs > 0) (pos.toFloat() / st.durationMs).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { p }, modifier = Modifier.fillMaxWidth().height(3.dp),
            color = Pink, trackColor = Color.Transparent
        )
    }
}
