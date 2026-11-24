package pl.kijewski.synbee.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.X509TrustManager

/** The app API version this app speaks; the hub says its own in ping. */
const val APP_API_VERSION = 1
private const val PREFIX = "/api/app/v1"

/** The hub refused the phone: it was revoked, or its member removed. Pair again. */
class UnauthorizedException(message: String) : IOException(message)
class HubUnreachableException(message: String) : IOException(message)
class HubRefusedException(val status: Int, message: String) : IOException(message)

private val json = Json { ignoreUnknownKeys = true }
private val JSON_TYPE = "application/json".toMediaType()

private fun clientFor(trust: X509TrustManager, pin: String?): OkHttpClient {
    val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), null) }
    return OkHttpClient.Builder()
        .sslSocketFactory(ssl.socketFactory, trust)
        // The hub is reached by its IP address, so the host name can't be checked against its certificate. The
        // identity check is the pinned key instead: the session's certificate must carry it, as the trust manager
        // already required. Nothing is accepted without a pin.
        .hostnameVerifier { _, session ->
            pin != null && (runCatching { session.peerCertificates.firstOrNull() }.getOrNull() as? X509Certificate)?.let { spkiFingerprint(it) == pin } == true
        }
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()
}

/**
 * Talks to the hub's app API through the paired addresses, trusting exactly the paired key. Tries addresses in
 * order; an address whose server has another key never receives the token, or the pairing code.
 */
class HubClient(pin: String, private val addresses: () -> List<String>, private val token: () -> String?) : Hub {
    private val http = clientFor(PinnedKeyTrustManager(pin), pin)
    @Volatile override var lastGood: String? = null
        private set

    private suspend fun <T> call(path: String, body: String? = null, authed: Boolean = true, parse: (String) -> T): T = withContext(Dispatchers.IO) {
        val order = addresses().let { list -> lastGood?.let { listOf(it) + (list - it) } ?: list }
        var last: Exception? = null
        for (address in order) {
            val req = Request.Builder().url("https://$address$PREFIX$path").apply {
                if (authed) header("Authorization", "Bearer ${token() ?: throw UnauthorizedException("This phone isn't paired")}")
                if (body != null) post(body.toRequestBody(JSON_TYPE))
            }.build()
            try {
                http.newCall(req).execute().use { res ->
                    val text = res.body.string()
                    lastGood = address
                    when {
                        res.code == 401 -> throw UnauthorizedException(errorOf(text) ?: "Pair this phone again")
                        !res.isSuccessful -> throw HubRefusedException(res.code, errorOf(text) ?: "The hub answered ${res.code}")
                        else -> return@withContext parse(text)
                    }
                }
            } catch (e: SSLHandshakeException) {
                last = e // Another key, or not the hub: nothing was sent. Try the next address.
            } catch (e: UnauthorizedException) {
                throw e
            } catch (e: HubRefusedException) {
                throw e
            } catch (e: IOException) {
                last = e
            }
        }
        throw HubUnreachableException(
            if (last is SSLHandshakeException) "The hub's identity doesn't match the pairing code" else last?.message ?: "The hub can't be reached",
        )
    }

    suspend fun ping(): Ping = call("/ping", authed = false) { json.decodeFromString<Ping>(it) }

    suspend fun pair(code: String, phoneName: String): Paired =
        call("/pair", buildJsonObject {
            put("code", code)
            put("phoneName", phoneName)
        }.toString(), authed = false) { json.decodeFromString<Paired>(it) }

    /** The test request: who this phone is, and how much its member may use. */
    override suspend fun me(): Me = call("/me") { json.decodeFromString<Me>(it) }

    /** The actions this phone's member may run, in the owner's order. */
    suspend fun actions(): List<HubAction> = call("/actions") { json.decodeFromString<List<HubAction>>(it) }

    /** Where the hub can be reached: its home addresses, and its outside one when the owner allowed it. */
    override suspend fun connection(): Connection = call("/connection") { json.decodeFromString<Connection>(it) }

    override suspend fun home(): Home = call("/home") { json.decodeFromString<Home>(it) }

    override suspend fun details(id: Int): ActionDetails = call("/actions/$id/details") { json.decodeFromString<ActionDetails>(it) }

    override suspend fun room(id: Int): RoomPage = call("/rooms/$id") { json.decodeFromString<RoomPage>(it) }

    override suspend fun run(id: Int): RunResult = call("/actions/$id/run", "{}") { json.decodeFromString<RunResult>(it) }

    override suspend fun pauseAlerts(minutes: Int): PauseResult = call("/alerts/pause", "{\"minutes\":$minutes}") { json.decodeFromString<PauseResult>(it) }

    private fun errorOf(text: String): String? =
        runCatching { json.decodeFromString<JsonObject>(text)["error"]?.jsonPrimitive?.content }.getOrNull()

    companion object {
        /**
         * Reads the key fingerprint at an address, for pairing by typed code. First the handshake only shows the key
         * and is then refused, so nothing is sent; then a ping, trusting exactly that key, checks it is a SynBee hub.
         */
        suspend fun readFingerprint(address: String): String = withContext(Dispatchers.IO) {
            val capture = CaptureTrustManager()
            try {
                clientFor(capture, null).newCall(Request.Builder().url("https://$address$PREFIX/ping").build()).execute().close()
            } catch (_: IOException) {
                // Expected: the capture refuses every handshake once it has seen the key.
            }
            val cert: X509Certificate = capture.seen ?: throw HubUnreachableException("The hub can't be reached")
            val pin = spkiFingerprint(cert)
            try {
                clientFor(PinnedKeyTrustManager(pin), pin).newCall(Request.Builder().url("https://$address$PREFIX/ping").build()).execute().use { r ->
                    val ping = runCatching { json.decodeFromString<Ping>(r.body.string()) }.getOrNull()
                    if (!r.isSuccessful || ping?.hub != "synbee") throw HubUnreachableException("This address isn't a SynBee hub")
                }
            } catch (e: HubUnreachableException) {
                throw e
            } catch (e: IOException) {
                throw HubUnreachableException(e.message ?: "The hub can't be reached")
            }
            pin
        }
    }
}
