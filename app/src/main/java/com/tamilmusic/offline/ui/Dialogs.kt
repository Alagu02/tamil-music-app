package com.tamilmusic.offline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tamilmusic.offline.MainViewModel
import com.tamilmusic.offline.data.Song
import com.tamilmusic.offline.player.PlaybackManager

@Composable
fun CreatePlaylistDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Playlist name") })
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name); onDismiss() }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddToPlaylistDialog(song: Song, vm: MainViewModel, onDismiss: () -> Unit) {
    val playlists by vm.playlists.collectAsState()
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                playlists.forEach { p ->
                    TextButton(
                        onClick = { vm.addToPlaylist(p.id, song.id); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(p.name, modifier = Modifier.fillMaxWidth()) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("New playlist") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { vm.createPlaylist(name, song.id); onDismiss() },
                enabled = name.isNotBlank()
            ) { Text("Create & add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SpeedDialog(pm: PlaybackManager, onDismiss: () -> Unit) {
    val st by pm.state.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback speed") },
        text = {
            Column {
                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                    TextButton(onClick = { pm.setSpeed(s); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${s}x" + if (s == st.speed) "  ✓" else "",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun SleepDialog(pm: PlaybackManager, onDismiss: () -> Unit) {
    val end by pm.sleepEndAt.collectAsState()
    val remaining = if (end > 0) ((end - System.currentTimeMillis()) / 60_000L + 1).coerceAtLeast(1) else 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                if (remaining > 0) Text("Stopping in about $remaining min", color = MaterialTheme.colorScheme.secondary)
                listOf(15, 30, 45, 60, 90).forEach { m ->
                    TextButton(onClick = { pm.setSleepTimer(m); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                        Text("$m minutes", modifier = Modifier.fillMaxWidth())
                    }
                }
                TextButton(onClick = { pm.setSleepTimer(0); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Turn off", modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun EqualizerDialog(pm: PlaybackManager, onDismiss: () -> Unit) {
    val info by pm.eqInfo.collectAsState()
    val on by pm.eqEnabled.collectAsState()
    val levels by pm.eqLevels.collectAsState()
    val bass by pm.bassStrength.collectAsState()
    val bassOk by pm.bassSupported.collectAsState()
    val i = info

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Equalizer") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (i == null) {
                    Text("Equalizer isn't available yet. Start playing a song first, or your device doesn't support it.")
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enabled", modifier = Modifier.weight(1f))
                        Switch(checked = on, onCheckedChange = { pm.setEqEnabled(it) })
                    }
                    i.freqsHz.forEachIndexed { idx, hz ->
                        val label = if (hz >= 1000) "%.1f kHz".format(hz / 1000f) else "$hz Hz"
                        val lv = levels.getOrElse(idx) { 0 }
                        Text("$label   ${"%+.1f".format(lv / 100f)} dB", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = lv.toFloat(),
                            onValueChange = { pm.setBandLevel(idx, it.toInt()) },
                            valueRange = i.minLevel.toFloat()..i.maxLevel.toFloat(),
                            enabled = on
                        )
                    }
                }
                if (bassOk) {
                    Spacer(Modifier.height(8.dp))
                    Text("Bass boost", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = bass.toFloat(),
                        onValueChange = { pm.setBass(it.toInt()) },
                        valueRange = 0f..1000f
                    )
                } else if (i != null) {
                    Text("Bass boost not supported on this device.", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = { pm.resetEq() }) { Text("Reset") } }
    )
}

@Composable
fun LyricsDialog(song: Song, vm: MainViewModel, onDismiss: () -> Unit) {
    val saved by remember(song.id) { vm.lyrics(song.id) }.collectAsState(initial = null)
    var editing by remember { mutableStateOf(false) }
    var draft by remember(saved, editing) { mutableStateOf(saved ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lyrics") },
        text = {
            if (editing) {
                OutlinedTextField(
                    draft, { draft = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp),
                    placeholder = { Text("Paste lyrics you have the right to use") }
                )
            } else {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        saved ?: "No lyrics saved.\nTap Add to paste lyrics you have the right to use.",
                        color = if (saved == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            if (editing) TextButton(onClick = { vm.saveLyrics(song.id, draft); editing = false }) { Text("Save") }
            else TextButton(onClick = { editing = true }) { Text(if (saved == null) "Add" else "Edit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
