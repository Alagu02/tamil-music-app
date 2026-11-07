package com.tamilmusic.offline.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import com.tamilmusic.offline.MainViewModel
import com.tamilmusic.offline.ui.theme.AccentBrush
import com.tamilmusic.offline.ui.theme.Pink

private const val FAV_ID = -1L

@Composable
fun PlaylistsScreen(vm: MainViewModel) {
    val playlists by vm.playlists.collectAsState()
    val favs by vm.favoriteIds.collectAsState()
    var open by rememberSaveable { mutableStateOf<Long?>(null) }
    var create by remember { mutableStateOf(false) }
    BackHandler(open != null) { open = null }

    val openId = open
    if (openId != null) {
        val name = if (openId == FAV_ID) "Favourites" else playlists.firstOrNull { it.id == openId }?.name
        if (name == null) { open = null } else PlaylistDetail(vm, openId, name) { open = null }
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Playlists", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { create = true }) { Icon(Icons.Rounded.Add, "New playlist") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            item {
                PlaylistRow("Favourites", "${favs.size} songs", true, onClick = { open = FAV_ID }, onDelete = null)
            }
            itemsIndexed(playlists, key = { _, p -> p.id }) { _, p ->
                PlaylistRow(p.name, "Playlist", false, onClick = { open = p.id }, onDelete = { vm.deletePlaylist(p.id) })
            }
            if (playlists.isEmpty()) item {
                Text(
                    "Tap + to create your first playlist.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
    if (create) CreatePlaylistDialog(onCreate = { vm.createPlaylist(it) }, onDismiss = { create = false })
}

@Composable
private fun PlaylistRow(title: String, subtitle: String, fav: Boolean, onClick: () -> Unit, onDelete: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(AccentBrush), contentAlignment = Alignment.Center) {
            Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.LibraryMusic, null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(subtitle,
