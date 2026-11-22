package pl.kijewski.synbee.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.ModalBottomSheet
import pl.kijewski.synbee.data.ActionDetails
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** What an action touches, in words: opened by a long press on its tile. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailsSheet(details: ActionDetails, vm: AppViewModel) {
    ModalBottomSheet(onDismissRequest = vm::closeDetails) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(details.name, style = MaterialTheme.typography.titleLarge)
            val state = when (details.state) { "active" -> "Active"; "partly" -> "Partly"; "inactive" -> "Not active"; else -> null }
            Text(listOfNotNull(state, details.status.ifBlank { null }).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            details.targets.forEach { t ->
                if (t.kind == "group") {
                    Text("${t.name} (light group)${if (t.summary.isNotBlank()) " · ${t.summary}" else ""}", style = MaterialTheme.typography.titleSmall)
                    t.devices.forEach { DeviceRow(it, indent = true) }
                } else t.device?.let { DeviceRow(it, indent = false) }
            }
        }
    }
}
