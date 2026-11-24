package pl.kijewski.synbee.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pl.kijewski.synbee.data.HubAction
import pl.kijewski.synbee.data.HubRoom
import pl.kijewski.synbee.data.StatusTile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A page of the pager: a room, or the actions that are in no room. */
private data class Page(val room: HubRoom?, val actions: List<HubAction>, val tiles: List<StatusTile>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColumnScope.PairedScreen(state: UiState, vm: AppViewModel) {
    val pages = remember(state.rooms, state.actions, state.tiles) {
        state.rooms.map { r -> Page(r, state.actions.filter { r.id in it.rooms }, state.tiles.filter { it.room == r.id }) } +
            Page(null, state.actions.filter { it.rooms.isEmpty() }, state.tiles.filter { it.room == null || state.rooms.none { r -> r.id == it.room } })
                .let { if (it.actions.isEmpty() && it.tiles.isEmpty()) emptyList() else listOf(it) }
    }
    if (pages.isEmpty()) return
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    PrimaryScrollableTabRow(selectedTabIndex = pager.currentPage.coerceAtMost(pages.size - 1), edgePadding = 0.dp) {
        pages.forEachIndexed { i, page ->
            val color = page.room?.let { hubColor(it.color) } ?: MaterialTheme.colorScheme.outline
            // A tap shows the room's actions; a long press opens the room page (the room's devices).
            Tab(selected = pager.currentPage == i, onClick = { scope.launch { pager.animateScrollToPage(i) } }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { scope.launch { pager.animateScrollToPage(i) } },
                            onLongClick = { page.room?.let { vm.openRoom(it.id) } },
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Symbol(page.room?.icon ?: "help_center", color = color)
                    Text(page.room?.name ?: "Other", maxLines = 1)
                }
            }
        }
    }
    HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.Top) { index ->
        val page = pages[index]
        val color = page.room?.let { hubColor(it.color) } ?: MaterialTheme.colorScheme.outline
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Status first: what the room looks like; then what can be done in it.
            items(page.tiles, key = { it.id }) { tile -> StatusTileCard(tile, vm) }
            items(page.actions, key = { it.id }) { action -> ActionTile(action, color, state.running, vm) }
        }
    }
    state.me?.alertsPaused?.takeIf { it.until > System.currentTimeMillis() }?.let { p ->
        Text("Alerts paused until ${hhmm(p.until)} by ${p.by}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    state.tileOpen?.let { tile ->
        AlertDialog(
            onDismissRequest = vm::closeTile,
            icon = { Symbol(tile.icon) },
            title = { Text(tile.name) },
            text = { Text(listOf(tile.text, *tile.why.toTypedArray()).joinToString("\n")) },
            confirmButton = { TextButton(onClick = vm::closeTile) { Text("Close") } },
        )
    }
}

/**
 * An action's tile, coloured by power: filled in the room's colour when what it switches is on, grey when off, pale
 * with a thick room-coloured border when only some is on. A check in the corner when its devices already show what it
 * sets.
 */
@Composable
private fun ActionTile(action: HubAction, roomColor: Color, running: Int?, vm: AppViewModel) {
    val surface = MaterialTheme.colorScheme.surface
    val fill = when (action.power) {
        "on" -> roomColor.copy(alpha = 0.85f).compositeOver(surface)
        "off" -> MaterialTheme.colorScheme.surfaceVariant
        else -> roomColor.copy(alpha = 0.18f).compositeOver(surface)
    }
    val onFill = if (action.power == "on") Color(0xFF1B1B1F) else MaterialTheme.colorScheme.onSurface
    Card(
        colors = CardDefaults.cardColors(containerColor = fill, contentColor = onFill),
        border = BorderStroke(if (action.power == "partly") 3.dp else 1.dp, if (action.power == "off") MaterialTheme.colorScheme.outlineVariant else roomColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .combinedClickable(enabled = running == null, onClick = { vm.run(action) }, onLongClick = { vm.openDetails(action) }),
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Symbol(action.icon, size = 32.sp, color = onFill)
                Text(action.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                if (action.status.isNotBlank()) Text(action.status, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            if (running == action.id) CircularProgressIndicator(Modifier.size(22.dp).align(Alignment.TopEnd), strokeWidth = 2.dp)
            else if (action.state == "active") Symbol("check_circle", Modifier.align(Alignment.TopEnd), size = 22.sp, color = onFill)
        }
    }
}

/** Fill colour of a status tile by its level: green, grey, orange, red. */
@Composable
private fun levelColor(level: String): Color = when (level) {
    "fine" -> Color(0xFF6BBF73)
    "warning" -> Color(0xFFF0A23B)
    "alarm" -> Color(0xFFE5534B)
    else -> MaterialTheme.colorScheme.outline
}

/** A read-only status tile: its state in words, coloured by level; a press explains it. */
@Composable
private fun StatusTileCard(tile: StatusTile, vm: AppViewModel) {
    val surface = MaterialTheme.colorScheme.surface
    val accent = tile.color?.let { hubColor(it) } ?: levelColor(tile.level)
    Card(
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.16f).compositeOver(surface), contentColor = MaterialTheme.colorScheme.onSurface),
        border = BorderStroke(2.dp, accent),
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .combinedClickable(onClick = { vm.openTile(tile) }, onLongClick = { vm.openTile(tile) }),
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Symbol(tile.icon.ifBlank { "sensors" }, size = 28.sp, color = accent)
            Text(tile.name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(tile.text, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}
