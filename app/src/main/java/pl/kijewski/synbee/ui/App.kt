package pl.kijewski.synbee.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.app.Activity
import pl.kijewski.synbee.data.hostOf
import pl.kijewski.synbee.data.isLocalHost
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import pl.kijewski.synbee.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App(state: UiState, vm: AppViewModel, activity: Activity) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            // Paired, the room pager fills the screen and scrolls itself; pairing scrolls as a whole.
            Modifier.safeDrawingPadding().padding(16.dp).let { if (state.paired) it else it.verticalScroll(rememberScrollState()) },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(state, vm)
            if (state.paired) PairedScreen(state, vm) else PairScreen(state, vm, activity)
            if (state.busy) CircularProgressIndicator()
            state.message?.let { Text(it, color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) }
        }
    }
    if (state.connectionOpen) ConnectionView(state, vm)
    if (state.aboutOpen) AboutView(vm)
    state.details?.let { DetailsSheet(it, vm) }
    state.roomPage?.let { RoomPageView(it, vm) }
    state.keyCheck?.let { check ->
        AlertDialog(
            onDismissRequest = vm::cancelKeyCheck,
            title = { Text("Is this your hub?") },
            text = {
                Text(
                    "Check that SynBee's App users page shows the same key:\n\n${check.short}\n\n" +
                        "If it doesn't, something else answered at ${check.address}. Don't continue.",
                )
            },
            confirmButton = { TextButton(onClick = vm::confirmKey) { Text("Same key, pair") } },
            dismissButton = { TextButton(onClick = vm::cancelKeyCheck) { Text("Cancel") } },
        )
    }
}

/** One compact line, so the rooms keep their height in landscape too: the bee, the name, who this is, and the menu. */
@Composable
internal fun Header(state: UiState, vm: AppViewModel) {
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(painterResource(R.drawable.bee), contentDescription = null, modifier = Modifier.size(40.dp))
        Column(Modifier.weight(1f)) {
            Text("SynBee", style = MaterialTheme.typography.titleLarge)
            if (state.paired) {
                val where = state.via?.let { v -> if (isLocalHost(hostOf(v))) " · at home" else " · away" } ?: ""
                Text("${state.member ?: "?"} · ${state.phone ?: "?"}$where", style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
        if (state.paired) TextButton(enabled = !state.busy, onClick = vm::loadActions) { Text("Refresh") }
        Box {
            TextButton(onClick = { menu = true }) { Text("⋮") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (state.demo) {
                    DropdownMenuItem(text = { Text("Leave the demo") }, onClick = { menu = false; vm.leaveDemo() })
                } else if (state.paired) {
                    DropdownMenuItem(text = { Text("Connection") }, onClick = { menu = false; vm.openConnection() })
                    DropdownMenuItem(text = { Text("Test connection") }, onClick = { menu = false; vm.test() })
                    DropdownMenuItem(text = { Text("Forget hub") }, onClick = { menu = false; vm.forget() })
                }
                if (!state.demo) DropdownMenuItem(text = { Text("Try the demo") }, onClick = { menu = false; vm.startDemo() })
                DropdownMenuItem(text = { Text("About") }, onClick = { menu = false; vm.openAbout() })
            }
        }
    }
}
