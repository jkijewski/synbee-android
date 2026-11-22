package pl.kijewski.synbee.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

/** Contents of the hub's pairing QR code (app API v1). */
@Serializable
data class PairingQr(
    val v: Int,
    val hub: String,
    /** The NAS's home network addresses, as ip:port. */
    val a: List<String>,
    /** SHA-256 of the hub key's SubjectPublicKeyInfo, base64. */
    val fp: String,
    /** One-time pairing code, valid for 10 minutes. */
    val c: String,
    /** The outside address and port, when the owner allowed the app away from home. */
    val o: List<String> = emptyList(),
)

private val json = Json { ignoreUnknownKeys = true }
private val ADDRESS = Regex("""^(\[[0-9a-fA-F:.]+]|[A-Za-z0-9.-]{1,253}):(\d{1,5})$""")
private val FINGERPRINT = Regex("""^[A-Za-z0-9+/]{43}=$""")
private val CODE = Regex("""^[A-Za-z0-9]{5}-?[A-Za-z0-9]{5}$""")

fun isValidAddress(address: String): Boolean {
    val m = ADDRESS.matchEntire(address.trim()) ?: return false
    return m.groupValues[2].toInt() in 1..65535
}

/** A home network address (private IP); anything else is an outside address. */
fun isHomeAddress(address: String): Boolean = isValidAddress(address) && isLocalHost(hostOf(address.trim()))

fun isValidCode(code: String): Boolean = CODE.matches(code.trim())

/** Parses and checks a scanned QR code; throws IllegalArgumentException with a readable message. */
fun parsePairingQr(text: String): PairingQr {
    val qr = try {
        json.decodeFromString<PairingQr>(text)
    } catch (e: Exception) {
        throw IllegalArgumentException("This isn't a SynBee pairing code")
    }
    require(qr.v == 1 && qr.hub == "synbee") { "This isn't a SynBee pairing code" }
    val home = qr.a.filter(::isHomeAddress)
    require(home.isNotEmpty()) { "The code has no home network address" }
    require(FINGERPRINT.matches(qr.fp)) { "The code has no valid key fingerprint" }
    require(isValidCode(qr.c)) { "The code is incomplete" }
    return qr.copy(a = home, o = qr.o.filter(::isValidAddress))
}

/** The fingerprint as the hub's App users page shows it for typed pairing: 16 hex digits in groups of four. */
fun shortFingerprint(base64: String): String {
    val hex = Base64.getDecoder().decode(base64).joinToString("") { "%02X".format(it) }
    return hex.take(16).chunked(4).joinToString(" ")
}
