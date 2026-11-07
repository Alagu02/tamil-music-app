package com.tamilmusic.offline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tamilmusic.offline.MainViewModel
import com.tamilmusic.offline.ThemeMode

@Composable
fun SettingsScreen(vm: MainViewModel, actions: Actions) {
    val theme by vm.theme.collectAsState()
    var showEq by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        SectionTitle("Appearance")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.values().forEach { t ->
                FilterChip(selected = theme == t, onClick = { vm.setTheme(t) }, label = { Text(t.label) })
            }
        }

        SectionTitle("Music")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = actions.importFiles) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Import files")
            }
            OutlinedButton(onClick = { if (actions.hasPermission) vm.refresh() else actions.requestPermission() }) {
                Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Rescan device")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Imported files stay where they are; the app only remembers access to them.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionTitle("Sound")
        OutlinedButton(onClick = { showEq = true }) {
            Icon(Icons.Rounded.GraphicEq, null); Spacer(Modifier.width(6.dp)); Text("Equalizer & bass")
        }

        SectionTitle("About")
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
        ) {
            Text("Tamil Music – Offline Player", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "100% offline. No ads, no login, no subscriptions, no tracking. " +
                    "Plays only the music already on your device or files you import.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(24.dp))
    }
    if (showEq) EqualizerDialog(vm.pm) { showEq = false }
}
