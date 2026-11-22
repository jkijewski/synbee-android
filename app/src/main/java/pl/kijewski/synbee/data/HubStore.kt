package pl.kijewski.synbee.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * What pairing left on the phone: the hub's key, the member's and phone's names, the token, and where to reach the hub:
 * its home addresses, the outside address the hub announces, and one the user entered (like Synology Photos).
 */
class HubStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("hub", Context.MODE_PRIVATE)
    private val tokens = TokenStore(context)

    init {
        migrate()
    }

    /**
     * Stored data versioning, like the hub's database: each step upgrades what an older app version stored, in order,
     * once. Never change a released step; add a new one.
     */
    private fun migrate() {
        val steps: List<(android.content.SharedPreferences.Editor) -> Unit> = listOf(
            // 1: the first data version (addresses, pin, names); nothing to change, it only records the version.
            { _ -> },
            // 2: away from home: the outside addresses start empty and "not allowed" until the hub says otherwise.
            { e -> if (!prefs.contains("outside")) e.putString("outside", "") },
        )
        val from = prefs.getInt("dataVersion", 0)
        if (from >= steps.size) return
        val edit = prefs.edit()
        for (i in from until steps.size) steps[i](edit)
        edit.putInt("dataVersion", steps.size).apply()
    }

    private fun list(key: String) = prefs.getString(key, null)?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()

    // Paired away from home, the phone knows only the outside address until the hub tells it the home ones.
    val isPaired get() = pin != null && (addresses.isNotEmpty() || outside.isNotEmpty()) && tokens.load() != null
    val pin: String? get() = prefs.getString("pin", null)
    /** The hub's home network addresses, as host:port. */
    val addresses: List<String> get() = list("addresses")
    /** The outside address the hub announces, when the owner switched on away-from-home use. */
    val announcedOutside: List<String> get() = list("outside")
    /** An outside address the user entered in Connection; it wins over the announced one. */
    val manualOutside: String? get() = prefs.getString("manualOutside", null)
    val outside: List<String> get() = listOfNotNull(manualOutside) + (announcedOutside - setOfNotNull(manualOutside))
    val member: String? get() = prefs.getString("member", null)
    val phone: String? get() = prefs.getString("phone", null)

    fun save(addresses: List<String>, outside: List<String>, pin: String, paired: Paired) {
        tokens.save(paired.token)
        prefs.edit()
            .putString("addresses", addresses.joinToString("\n"))
            .putString("outside", outside.joinToString("\n"))
            .putString("pin", pin)
            .putString("member", paired.member)
            .putString("phone", paired.phone)
            .apply()
    }

    /** Whether the owner allowed this phone away from home, as the hub said last; null until it has. */
    val outsideAllowed: Boolean? get() = if (prefs.contains("outsideAllowed")) prefs.getBoolean("outsideAllowed", false) else null

    /** What the hub said about where to reach it; an empty home list keeps the old one. */
    fun updateConnection(home: List<String>, outside: List<String>, allowed: Boolean) {
        val edit = prefs.edit().putString("outside", outside.joinToString("\n")).putBoolean("outsideAllowed", allowed)
        if (home.isNotEmpty()) edit.putString("addresses", home.joinToString("\n"))
        edit.apply()
    }

    fun setManualOutside(address: String?) {
        prefs.edit().apply { if (address.isNullOrBlank()) remove("manualOutside") else putString("manualOutside", address) }.apply()
    }

    /** Forgets the hub on this phone. The owner revokes the phone on the hub. */
    fun forget() {
        tokens.clear()
        val version = prefs.getInt("dataVersion", 0)
        prefs.edit().clear().putInt("dataVersion", version).apply()
    }

    private fun onWifi(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /** Home first on Wi-Fi (it's faster and works without the router's forwarding), the outside address first elsewhere. */
    fun order(): List<String> = orderAddresses(addresses, outside, onWifi())

    /** The same order for addresses not saved yet, as when pairing. */
    fun order(home: List<String>, outside: List<String>): List<String> = orderAddresses(home, outside, onWifi())

    fun client(): HubClient? = pin?.let { p -> HubClient(p, { order() }, { tokens.load() }) }

    companion object {
        /** A client for pairing: the token doesn't exist yet. */
        fun pairingClient(addresses: List<String>, pin: String) = HubClient(pin, { addresses }, { null })
    }
}
