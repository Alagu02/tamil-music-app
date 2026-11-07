package com.tamilmusic.offline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tamilmusic.offline.MainViewModel
import com.tamilmusic.offline.ui.theme.Pink
import com.tamilmusic.offline.ui.theme.Purple

@Composable
fun HomeScreen(
    vm: MainViewModel,
    actions: Actions,
    onOpenLibrary: (Int) -> Unit,
    onOpenPlaylists: () -> Unit
) {
    val all by vm.songs.collectAsState()
    val recents by vm.recents.collectAsState()
    val currentId = vm.current.collectAsState().value?.id
    val latest = remember(all) { all.sortedByDescending { it.dateAdded }.take(5) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("வணக்கம் 👋", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Tamil Music",
                style = MaterialTheme.typography.headlineLarge.copy(brush = com.tamilmusic.offline.ui.theme.AccentBrush),
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientButton("Shuffle all", Icons.Rounded.Shuffle, { vm.shuffleAll(all) })
                Spacer(Modifier.width(10.dp))
                OutlinedButton(onClick = actions.importFiles) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Import")
                }
            }
        }

        if (all.isEmpty()) item { EmptyState(actions) }

        if (recents.isNotEmpty()) {
            item { SectionTitle("Recently played") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(recents, key = { _, s -> s.id }) { i, s ->
                        Column(Modifier.width(132.dp).clip(RoundedCornerShape(18.dp)).clickable { vm.play(recents, i) }) {
                            SongArt(s.artUri, Modifier.size(132.dp), corner = 18.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(
                                s.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (all.isNotEmpty()) {
            item { SectionTitle("Browse") }
            item {
                val artists = all.map { it.artist }.distinct().size
                val albums = all.map { it.album }.distinct().size
                val genres = all.map { it.genre }.distinct().size
                val favs by vm.favoriteIds.collectAsState()
                val pls by vm.playlists.collectAsState()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CategoryCard("Songs", "${all.size}", Icons.Rounded.MusicNote, Purple, Pink, Modifier.weight(1f)) { onOpenLibrary(0) }
                        CategoryCard("Artists", "$artists", Icons.Rounded.Person, Pink, Purple, Modifier.weight(1f)) { onOpenLibrary(1) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CategoryCard("Albums", "$albums", Icons.Rounded.Album, Color(0xFF6366F1), Purple, Modifier.weight(1f)) { onOpenLibrary(2) }
                        CategoryCard("Genres", "$genres", Icons.Rounded.Category, Color(0xFFD946EF), Pink, Modifier.weight(1f)) { onOpenLibrary(3) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CategoryCard("Favourites", "${favs.size}", Icons.Rounded.Favorite, Pink, Color(0xFFF43F5E), Modifier.weight(1f)) { onOpenPlaylists() }
                        CategoryCard("Playlists", "${pls.size}", Icons.Rounded.LibraryMusic, Purple, Color(0xFF6366F1), Modifier.weight(1f)) { onOpenPlaylists() }
                    }
                }
            }
            item { SectionTitle("Recently added") }
            songItems(latest, vm, currentId)
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CategoryCard(
    title: String, count: String, icon: ImageVector,
    c1: Color, c2: Color, modifier: Modifier, onClick: () -> Unit
) {
    Row(
        modifier.height(84.dp).clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(c1.copy(alpha = 0.85f), c2.copy(alpha = 0.85f))))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(count, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        }
    }
}
