package pl.kijewski.synbee.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import pl.kijewski.synbee.R
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

/** About: what the app is, who made it, its licence (MIT, shown in full as it requires) and the no-warranty notice. */
@Composable
internal fun AboutView(vm: AppViewModel) {
    BackHandler(onBack = vm::closeAbout)
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = vm::closeAbout) { Text("‹ Back") }
                Text("About", style = MaterialTheme.typography.titleLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.bee), contentDescription = null, modifier = Modifier.size(56.dp))
                Column {
                    Text("SynBee", style = MaterialTheme.typography.titleLarge)
                    Text("Version $version", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "The mobile app for SynBee, a private Zigbee hub that runs on your own Synology NAS. " +
                    "It shows the rooms and actions the hub's owner set up for you and runs them with a tap, at home or away. " +
                    "Nothing leaves your network: it talks only to your hub, checking its key, and nothing goes through anyone's cloud.",
            )
            Text(
                "Away from home (if your hub's owner allows it), the app's requests travel over the internet to your home, encrypted and " +
                    "accepted only by your own hub. Scanning the pairing QR code uses Google's code scanner, which may send Google diagnostic data.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("Made by Jacek Kijewski.", style = MaterialTheme.typography.bodyMedium)
            Column {
                TextButton(onClick = { uri.openUri(Links.PRIVACY) }) { Text("Privacy policy") }
                TextButton(onClick = { uri.openUri(Links.SOURCE) }) { Text("Source code and help") }
                TextButton(onClick = { uri.openUri(Links.SUPPORT_MAIL) }) { Text("Write to support") }
                TextButton(onClick = { uri.openUri(Links.COFFEE) }) { Text("Buy me a coffee") }
            }
            Text("No warranty", style = MaterialTheme.typography.titleSmall)
            Text(
                "SynBee is free software, provided as is, without warranty of any kind. It switches real devices in your home: " +
                    "don't rely on it for safety, security or anything where a missed or wrong switch could cause harm or loss. " +
                    "You use it at your own risk.",
            )
            Text("Licence", style = MaterialTheme.typography.titleSmall)
            Text("MIT License. It also uses libraries under their own licences: AndroidX and Jetpack Compose, OkHttp, kotlinx.serialization and Material Symbols (Apache 2.0), and Google's code scanner (Google Play services terms).", style = MaterialTheme.typography.bodyMedium)
            Card(Modifier.fillMaxWidth()) {
                Text(MIT_LICENSE, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

private const val MIT_LICENSE = """MIT License

Copyright (c) 2026 Jacek Kijewski

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE."""
