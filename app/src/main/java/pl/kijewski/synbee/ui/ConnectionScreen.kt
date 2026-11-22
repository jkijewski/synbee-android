package pl.kijewski.synbee.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import pl.kijewski.synbee.data.hostOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Where the app reaches the hub, like Synology Photos: the home addresses from pairing, and an outside address and port
 * for away from home, announced by the hub or typed here.
 */
@Composable
internal fun ConnectionView(state: UiState, vm: AppViewModel) {
    BackHandler(onBack = vm::closeConnection)
    val store = vm.store
    val manual = store.manualOutside
    var address by rememberSaveable { mutableStateOf(manual?.let { hostOf(it) } ?: "") }
    var port by rememberSaveable { mutableStateOf(manual?.substringAfterLast(':') ?: "") }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = vm::closeConnection) { Text("‹ Back") }
                Text("Connection", style = MaterialTheme.typography.titleLarge)
            }
            Text("At home", style = MaterialTheme.typography.titleSmall)
            Text(store.addresses.joinToString("\n").ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
            Text("Away from home", style = MaterialTheme.typography.titleSmall)
            if (store.outsideAllowed == false) {
                Text(
                    "This phone may be used at home only. The owner can allow it away from home in SynBee: App users → this phone → Away from home.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                if (store.announcedOutside.isEmpty()) "The hub doesn't announce an outside address. The owner switches it on in SynBee: App users → Using the app away from home."
                else "The hub announces ${store.announcedOutside.joinToString()}.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("Or enter it yourself, as in Synology Photos:", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = address,
                onValueChange = { address = it.trim() },
                label = { Text("Address, e.g. myhome.synology.me") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter(Char::isDigit).take(5) },
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !state.busy, onClick = { vm.saveOutside(address, port) }) { Text("Save") }
                if (manual != null) OutlinedButton(enabled = !state.busy, onClick = { address = ""; port = ""; vm.saveOutside("", "") }) { Text("Clear") }
            }
            Text(
                "On Wi-Fi the app tries home first; elsewhere the outside address first. It always checks it is talking to your hub (its key from pairing), so a wrong address gets nothing.",
                style = MaterialTheme.typography.bodySmall,
            )
            state.message?.let { Text(it, color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) }
        }
    }
}
