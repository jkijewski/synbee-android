package pl.kijewski.synbee.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import pl.kijewski.synbee.data.DeviceView
import pl.kijewski.synbee.data.RoomPage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A room's devices in words: opened by a long press on its tab, closed with Back. */
@Composable
internal fun RoomPageView(room: RoomPage, vm: AppViewModel) {
    BackHandler(onBack = vm::closeRoom)
    val color = hubColor(room.color)
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = vm::closeRoom) { Text("‹ Back") }
                Symbol(room.icon, size = 30.sp, color = color)
                Text(room.name, style = MaterialTheme.typography.titleLarge)
            }
            if (room.devices.isEmpty()) Text("No devices in this room.")
            room.devices.forEach { d ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f).compositeOver(MaterialTheme.colorScheme.surface)),
                    border = BorderStroke(1.dp, color),
                    modifier = Modifier.fillMaxWidth(),
                ) { Box(Modifier.padding(12.dp)) { DeviceRow(d, indent = false) } }
            }
        }
    }
}

internal fun ago(iso: String?): String? {
    val t = iso?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() } ?: return null
    val s = java.time.Duration.between(t, java.time.Instant.now()).seconds.coerceAtLeast(0)
    return when {
        s < 60 -> "just now"
        s < 3600 -> "${s / 60} min ago"
        s < 86400 -> "${s / 3600} h ago"
        else -> "${s / 86400} d ago"
    }
}

@Composable
internal fun DeviceRow(d: DeviceView, indent: Boolean) {
    Column(Modifier.padding(start = if (indent) 16.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(d.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (d.online == false) Text("offline", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (d.summary.isNotBlank()) Text(d.summary, style = MaterialTheme.typography.bodyMedium)
        d.readings.filter { it.property != "battery" && !d.summary.contains(it.text) }.forEach { r ->
            Text(
                "${r.label}: ${r.text}",
                style = MaterialTheme.typography.bodySmall,
                color = if (r.attention == "alarm") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val extra = listOfNotNull(d.room, d.battery?.let { "battery ${it.toInt()} %" }, ago(d.lastSeen)?.let { "heard $it" })
        if (extra.isNotEmpty()) Text(extra.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
