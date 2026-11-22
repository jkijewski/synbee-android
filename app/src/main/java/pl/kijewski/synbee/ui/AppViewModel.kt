package pl.kijewski.synbee.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.kijewski.synbee.data.APP_API_VERSION
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import pl.kijewski.synbee.data.ActionDetails
import pl.kijewski.synbee.data.HubAction
import pl.kijewski.synbee.data.RoomPage
import pl.kijewski.synbee.data.DemoHub
import pl.kijewski.synbee.data.Hub
import pl.kijewski.synbee.data.HubClient
import pl.kijewski.synbee.data.HubRoom
import pl.kijewski.synbee.data.HubStore
import pl.kijewski.synbee.data.Me
import pl.kijewski.synbee.data.UnauthorizedException
import pl.kijewski.synbee.data.isHomeAddress
import pl.kijewski.synbee.data.isValidAddress
import pl.kijewski.synbee.data.isValidCode
import pl.kijewski.synbee.data.parsePairingQr
import pl.kijewski.synbee.data.shortFingerprint

/** Pairing by typed code: the key read from the address, waiting for the user to compare it with the hub's page. */
data class KeyCheck(val address: String, val code: String, val pin: String) {
    val short get() = shortFingerprint(pin)
}

data class UiState(
    val paired: Boolean,
    val member: String? = null,
    val phone: String? = null,
    val busy: Boolean = false,
    val keyCheck: KeyCheck? = null,
    val me: Me? = null,
    val rooms: List<HubRoom> = emptyList(),
    val actions: List<HubAction> = emptyList(),
    /** The action running now, by id. */
    val running: Int? = null,
    /** The details sheet of a long-pressed action, while open. */
    val details: ActionDetails? = null,
    /** The room page of a long-pressed room tab, while open. */
    val roomPage: RoomPage? = null,
    /** The Connection screen is open. */
    val connectionOpen: Boolean = false,
    val aboutOpen: Boolean = false,
    /** Showing the sample home of the demo, not a real hub. */
    val demo: Boolean = false,
    /** The address that answered last: at home or away. */
    val via: String? = null,
    val message: String? = null,
    val error: Boolean = false,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val store = HubStore(app)
    private val _state = MutableStateFlow(UiState(paired = store.isPaired, member = store.member, phone = store.phone))
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** The demo's sample home while it is shown; it replaces the paired hub until the user leaves it. */
    private var demo: DemoHub? = null

    /** The hub the screens talk to: the demo while it is on, else the paired hub. */
    private fun hub(): Hub? = demo ?: store.client()

    /** Shows a sample home, for trying the app without a hub. Nothing is sent anywhere. */
    fun startDemo() {
        demo = DemoHub()
        stopRefreshing()
        _state.update { UiState(paired = true, member = "Demo", phone = "sample home", demo = true, message = "A sample home, not connected to any hub. Leave it under ⋮.") }
        loadActions()
        startRefreshing()
    }

    fun leaveDemo() {
        demo = null
        stopRefreshing()
        _state.update { UiState(paired = store.isPaired, member = store.member, phone = store.phone) }
        if (store.isPaired) {
            loadActions()
            startRefreshing()
        }
    }

    private val phoneName = listOfNotNull(Build.MANUFACTURER.replaceFirstChar { it.uppercase() }, Build.MODEL).distinct().joinToString(" ").take(60).ifBlank { "Phone" }

    private fun fail(text: String) = _state.update { it.copy(busy = false, message = text, error = true) }

    /** A QR code from the hub's App users page. */
    fun pairFromQr(text: String) {
        val qr = try {
            parsePairingQr(text)
        } catch (e: IllegalArgumentException) {
            return fail(e.message ?: "This isn't a SynBee pairing code")
        }
        pair(qr.a, qr.fp, qr.c, qr.o)
    }

    /** Typed pairing, step 1: read the key at the address, for the user to compare with the hub's page. */
    fun checkKey(address: String, code: String) {
        val a = address.trim()
        if (!isValidAddress(a)) return fail("Type the hub's address and port: at home e.g. 192.168.1.20:8443, away the outside address, e.g. myhome.synology.me:8443")
        if (!isValidCode(code)) return fail("The code has 10 letters and digits, e.g. K7M4Q-X92PA")
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                val pin = HubClient.readFingerprint(a)
                _state.update { it.copy(busy = false, keyCheck = KeyCheck(a, code.trim(), pin)) }
            } catch (e: Exception) {
                fail(e.message ?: "The hub can't be reached")
            }
        }
    }

    /** Typed pairing, step 2: the user saw the same key on the hub's page. */
    fun confirmKey() {
        val check = _state.value.keyCheck ?: return
        _state.update { it.copy(keyCheck = null) }
        // A typed outside address (pairing away from home) is kept as the user's own, like one typed in Connection.
        if (isHomeAddress(check.address)) pair(listOf(check.address), check.pin, check.code)
        else pair(emptyList(), check.pin, check.code, outside = listOf(check.address), typedOutside = true)
    }

    fun cancelKeyCheck() = _state.update { it.copy(keyCheck = null) }

    private fun pair(addresses: List<String>, pin: String, code: String, outside: List<String> = emptyList(), typedOutside: Boolean = false) {
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                // At home or away: the home addresses first on Wi-Fi, the outside one first elsewhere.
                val client = HubStore.pairingClient(store.order(addresses, outside), pin)
                val ping = client.ping()
                if (ping.api < APP_API_VERSION) return@launch fail("This hub (SynBee ${ping.version}) is too old for this app. Update SynBee first.")
                val paired = client.pair(code, phoneName)
                store.save(addresses, outside, pin, paired)
                if (typedOutside) store.setManualOutside(outside.first())
                _state.update { UiState(paired = true, member = paired.member, phone = paired.phone, message = "Paired as ${paired.member}.") }
                loadActions()
                startRefreshing()
            } catch (e: Exception) {
                fail(e.message ?: "Pairing failed")
            }
        }
    }

    /** The test request (⋮ → Test connection). */
    fun test() {
        val client = hub() ?: return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                val me = client.me()
                _state.update { it.copy(busy = false, me = me, message = "The hub answered.", error = false) }
            } catch (e: UnauthorizedException) {
                unpaired()
            } catch (e: Exception) {
                fail(e.message ?: "The hub can't be reached")
            }
        }
    }

    private var poller: Job? = null

    /** Re-reads states every 5 seconds while the app is in front (0011); nothing runs in the background. */
    fun startRefreshing() {
        if (poller?.isActive == true || hub() == null) return
        poller = viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                refreshQuietly()
            }
        }
    }

    fun stopRefreshing() {
        poller?.cancel()
        poller = null
    }

    /** A refresh nobody asked for: no spinner, and a failure only shows when the user asks again. */
    private suspend fun refreshQuietly() {
        val client = hub() ?: return
        try {
            val home = client.home()
            _state.update { it.copy(rooms = home.rooms, actions = home.actions) }
            _state.value.details?.let { d -> _state.update { it.copy(details = client.details(d.id)) } }
            _state.value.roomPage?.let { r -> _state.update { it.copy(roomPage = client.room(r.id)) } }
        } catch (e: UnauthorizedException) {
            unpaired()
        } catch (_: Exception) {
        }
    }

    fun openDetails(action: HubAction) {
        val client = hub() ?: return
        viewModelScope.launch {
            try {
                val d = client.details(action.id)
                _state.update { it.copy(details = d) }
            } catch (e: Exception) {
                fail(e.message ?: "The hub can't be reached")
            }
        }
    }

    fun closeDetails() = _state.update { it.copy(details = null) }

    fun openRoom(roomId: Int) {
        val client = hub() ?: return
        viewModelScope.launch {
            try {
                val r = client.room(roomId)
                _state.update { it.copy(roomPage = r) }
            } catch (e: Exception) {
                fail(e.message ?: "The hub can't be reached")
            }
        }
    }

    fun closeRoom() = _state.update { it.copy(roomPage = null) }

    fun openConnection() = _state.update { it.copy(connectionOpen = true, message = null) }

    fun closeConnection() = _state.update { it.copy(connectionOpen = false) }

    fun openAbout() = _state.update { it.copy(aboutOpen = true) }

    fun closeAbout() = _state.update { it.copy(aboutOpen = false) }

    /**
     * The outside address typed by the user, as in Synology Photos: a host name or IP and a port. Checked by reaching
     * the hub there with its pinned key before it is kept; empty clears it.
     */
    fun saveOutside(address: String, port: String) {
        val host = address.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
        if (host.isEmpty()) {
            store.setManualOutside(null)
            _state.update { it.copy(message = "Outside address cleared; the hub's own is used, if it announced one.", error = false) }
            return
        }
        val p = port.trim().toIntOrNull()
        if (p == null || p !in 1..65535) return fail("The port is a number from 1 to 65535")
        val target = "$host:$p"
        if (!isValidAddress(target)) return fail("Use a host name or IP address, such as myhome.synology.me")
        val pin = store.pin ?: return
        _state.update { it.copy(busy = true, message = "Checking $target…", error = false) }
        viewModelScope.launch {
            try {
                HubStore.pairingClient(listOf(target), pin).ping()
                store.setManualOutside(target)
                _state.update { it.copy(busy = false, message = "The hub answered at $target. It's used away from home.", error = false) }
            } catch (e: Exception) {
                // Kept anyway: away from home is often only testable from outside (mobile data).
                store.setManualOutside(target)
                _state.update {
                    it.copy(busy = false, message = "Saved, but the hub didn't answer at $target from here (${e.message}). Try it on mobile data.", error = true)
                }
            }
        }
    }

    /** Loads the member's actions from the hub. */
    fun loadActions() {
        val client = hub() ?: return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val home = client.home()
                _state.update {
                    it.copy(
                        busy = false,
                        rooms = home.rooms,
                        actions = home.actions,
                        via = client.lastGood,
                        message = if (home.actions.isEmpty()) "No actions yet. Create them in SynBee's web interface." else null,
                        error = false,
                    )
                }
                // The hub says where it can be reached, so a new outside address reaches the phone by itself.
                if (client is HubClient) runCatching { client.connection() }.getOrNull()?.let { c -> store.updateConnection(c.home, c.outside, c.outsideAllowed) }
            } catch (e: UnauthorizedException) {
                unpaired()
            } catch (e: Exception) {
                fail(e.message ?: "The hub can't be reached")
            }
        }
    }

    fun run(action: HubAction) {
        val client = hub() ?: return
        if (_state.value.running != null) return
        _state.update { it.copy(running = action.id, message = null) }
        viewModelScope.launch {
            try {
                val r = client.run(action.id)
                _state.update {
                    it.copy(running = null, message = if (r.ok) "${action.name}: done" else "${action.name}: ${r.error ?: "failed"}", error = !r.ok)
                }
                refreshQuietly()
            } catch (e: UnauthorizedException) {
                unpaired()
            } catch (e: Exception) {
                _state.update { it.copy(running = null, message = "${action.name}: ${e.message ?: "the hub can't be reached"}", error = true) }
            }
        }
    }

    private fun unpaired() {
        if (demo != null) return leaveDemo()
        store.forget()
        _state.update { UiState(paired = false, message = "The hub no longer knows this phone. Pair it again.", error = true) }
    }

    fun forget() {
        if (demo != null) return leaveDemo()
        store.forget()
        _state.update { UiState(paired = false, message = "Forgotten on this phone. Revoke it on the hub's App users page too.") }
    }
}
