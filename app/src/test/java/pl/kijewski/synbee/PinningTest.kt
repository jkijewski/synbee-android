package pl.kijewski.synbee

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import pl.kijewski.synbee.data.HubClient
import pl.kijewski.synbee.data.HubUnreachableException
import pl.kijewski.synbee.data.UnauthorizedException
import pl.kijewski.synbee.data.spkiFingerprint

/** Runs the real client against TLS servers with known keys: the pin decides where the token and the code go. */
class PinningTest {
    private fun server(cert: HeldCertificate): MockWebServer =
        MockWebServer().apply {
            useHttps(HandshakeCertificates.Builder().heldCertificate(cert).build().sslSocketFactory())
            start()
        }

    private val me = """{"member":"Jacek","phone":"Pixel 8","hubVersion":"0.2.0","actions":3,"rooms":2}"""

    @Test fun sendsTheTokenOnlyToTheHubWithThePinnedKey() = runTest {
        val hubKey = HeldCertificate.Builder().commonName("SynBee").build()
        val otherKey = HeldCertificate.Builder().commonName("Someone else").build()
        val impostor = server(otherKey).apply { enqueue(MockResponse.Builder().body(me).build()) }
        val hub = server(hubKey).apply { enqueue(MockResponse.Builder().body(me).build()) }
        val client = HubClient(spkiFingerprint(hubKey.certificate), { listOf("127.0.0.1:${impostor.port}", "127.0.0.1:${hub.port}") }, { "sb1_token" })

        assertEquals("Jacek", client.me().member)
        assertEquals(0, impostor.requestCount)
        val request = hub.takeRequest()
        assertEquals("/api/app/v1/me", request.url.encodedPath)
        assertEquals("Bearer sb1_token", request.headers["Authorization"])
        impostor.close()
        hub.close()
    }

    @Test fun neverSendsThePairingCodeToAnotherKey() = runTest {
        val impostor = server(HeldCertificate.Builder().commonName("Someone else").build())
        val client = HubClient("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", { listOf("127.0.0.1:${impostor.port}") }, { null })
        try {
            client.pair("K7M4Q-X92PA", "Pixel 8")
            fail("expected failure")
        } catch (e: HubUnreachableException) {
            assertEquals("The hub's identity doesn't match the pairing code", e.message)
        }
        assertEquals(0, impostor.requestCount)
        assertNull(client.lastGood)
        impostor.close()
    }

    @Test fun reportsARevokedPhone() = runTest {
        val hubKey = HeldCertificate.Builder().commonName("SynBee").build()
        val hub = server(hubKey).apply { enqueue(MockResponse.Builder().code(401).body("""{"error":"Pair this phone again"}""").build()) }
        val client = HubClient(spkiFingerprint(hubKey.certificate), { listOf("127.0.0.1:${hub.port}") }, { "sb1_token" })
        try {
            client.me()
            fail("expected failure")
        } catch (e: UnauthorizedException) {
            assertEquals("Pair this phone again", e.message)
        }
        hub.close()
    }

    @Test fun listsAndRunsActions() = runTest {
        val hubKey = HeldCertificate.Builder().commonName("SynBee").build()
        val hub = server(hubKey).apply {
            enqueue(MockResponse.Builder().body("""[{"id":7,"name":"Evening","icon":"nightlight","description":""}]""").build())
            enqueue(MockResponse.Builder().body("""{"ok":false,"failedStep":1,"error":"Step 1 failed: no answer"}""").build())
        }
        val client = HubClient(spkiFingerprint(hubKey.certificate), { listOf("127.0.0.1:${hub.port}") }, { "sb1_token" })
        assertEquals("Evening", client.actions().single().name)
        val r = client.run(7)
        assertEquals(false, r.ok)
        assertEquals("Step 1 failed: no answer", r.error)
        hub.takeRequest()
        val run = hub.takeRequest()
        assertEquals("POST", run.method)
        assertEquals("/api/app/v1/actions/7/run", run.url.encodedPath)
        hub.close()
    }

    @Test fun readsTheKeyForTypedPairingWithoutTrustingAnyOther() = runTest {
        val hubKey = HeldCertificate.Builder().commonName("SynBee").build()
        val hub = server(hubKey).apply { enqueue(MockResponse.Builder().body("""{"hub":"synbee","api":1,"version":"1.1.0"}""").build()) }
        assertEquals(spkiFingerprint(hubKey.certificate), HubClient.readFingerprint("127.0.0.1:${hub.port}"))
        // The key is read in a refused handshake; only the ping, pinned to that key, reaches the server.
        assertEquals(1, hub.requestCount)
        hub.close()
    }
}
