package com.tamilmusic.offline.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tamilmusic.offline.MainViewModel
import com.tamilmusic.offline.SortOrder
import com.tamilmusic.offline.data.Song
import com.tamilmusic.offline.ui.theme.AccentBrush

private fun keyOf(s: Song, mode: Int) = when (mode) { 1 -> s.artist; 2 -> s.album; else -> s.genre }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(vm: MainViewModel, initialMode: Int, actions: Actions) {
    val songs by vm.visibleSongs.collectAsState()
    val all by vm.songs.collectAsState()
    val query by vm.query.collectAsState()
    val sort by vm.sort.collectAsState()
    val loading by vm.loading.collectAsState()
    val currentId = vm.current.collectAsState().value?.id

    var mode by remember(initialMode) { mutableIntStateOf(initialMode) }
    var group by remember { mutableStateOf<String?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
    val modes = listOf("Songs", "Artists", "Albums", "Genres")
    BackHandler(group != null) { group = null }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Library", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { vm.refresh() }) { Icon(Icons.Rounded.Refresh, "Rescan") }
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Rounded.Sort, "Sort") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortOrder.values().forEach { o ->
                        DropdownMenuItem(
                            text = { Text(o.label + if (o == sort) "  ✓" else "") },
                            onClick = { vm.sort.value = o; sortMenu = false }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = query, onValueChange = { vm.query.value = it },
            placeholder = { Text("Search songs, artists, albums") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Rounded.Close, "Clear") }
            },
            singleLine = true, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEachIndexed { i, m ->
                FilterChip(selected = mode == i, onClick = { mode = i; group = null }, label = { Text(m) })
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
        Spacer(Modifier.height(6.dp))

        if (all.isEmpty()) {
            LazyColumn { item { EmptyState(actions) } }
        } else if (mode == 0 || group != null) {
            val g = group
            val list = if (g == null) songs else songs.filter { keyOf(it, mode) == g }
            if (g != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { group = null }) { Icon(Icons.Rounded.KeyboardArrowDown, "Back", modifier = Modifier.size(28.dp)) }
                    Text(g, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    GradientButton("Shuffle", Icons.Rounded.Shuffle, { vm.shuffleAll(list) })
                }
            }
            if (list.isEmpty()) Text("No songs found.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            LazyColumn(Modifier.weight(1f)) { songItems(list, vm, currentId) }
        } else {
            val groups = remember(songs, mode) {
                songs.groupBy { keyOf(it, mode) }.toList().sortedBy { it.first.lowercase() }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(groups, key = { it.first }) { (name, list) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { group = name }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(AccentBrush), contentAlignment = Alignment.Center) {
                            Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                "${list.size} song${if (list.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
