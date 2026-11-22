package pl.kijewski.synbee.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                val state by vm.state.collectAsStateWithLifecycle()
                App(state, vm, this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.loadActions() // the paired hub, or the demo; nothing when neither
        vm.startRefreshing()
    }

    override fun onPause() {
        vm.stopRefreshing()
        super.onPause()
    }
}
