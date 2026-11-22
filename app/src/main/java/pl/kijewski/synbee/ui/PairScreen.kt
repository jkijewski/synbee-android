package pl.kijewski.synbee.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
internal fun PairScreen(state: UiState, vm: AppViewModel, activity: Activity) {
    var address by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    Text("Pair this phone with your SynBee hub. On the hub's App users page, choose your name and \"Pair a phone\". Works at home or away.")
    Button(
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
            GmsBarcodeScanning.getClient(activity, options).startScan()
                .addOnSuccessListener { barcode -> barcode.rawValue?.let(vm::pairFromQr) }
        },
    ) { Text("Scan the QR code") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Or type it", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Hub address, e.g. 192.168.1.20:8443 or myhome.synology.me:8443") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase() },
                label = { Text("Pairing code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(enabled = !state.busy, onClick = { vm.checkKey(address, code) }) { Text("Continue") }
        }
    }
    Text("No hub yet? See how the app works with a sample home; nothing is sent anywhere.", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(enabled = !state.busy, modifier = Modifier.fillMaxWidth(), onClick = vm::startDemo) { Text("Try the demo") }
}
