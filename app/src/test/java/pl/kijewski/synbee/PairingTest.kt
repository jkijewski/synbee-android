package pl.kijewski.synbee

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.kijewski.synbee.data.isHomeAddress
import pl.kijewski.synbee.data.isValidCode
import pl.kijewski.synbee.data.orderAddresses
import pl.kijewski.synbee.data.parsePairingQr
import pl.kijewski.synbee.data.shortFingerprint

class PairingTest {
    private val fp = "sSPbNT+LQzorKzeL8kx89KUMFVxBQQdddVGCOn1Jlgk="

    @Test fun parsesTheHubQrFormat() {
        val qr = parsePairingQr("""{"v":1,"hub":"synbee","a":["192.168.1.20:8443","10.0.0.5:8443"],"fp":"$fp","c":"3CTCH-EE6MS"}""")
        assertEquals(listOf("192.168.1.20:8443", "10.0.0.5:8443"), qr.a)
        assertEquals("3CTCH-EE6MS", qr.c)
    }

    @Test fun keepsOnlyHomeAddresses() {
        val qr = parsePairingQr("""{"v":1,"hub":"synbee","a":["home.example.net:8443","192.168.1.20:8443"],"fp":"$fp","c":"3CTCH-EE6MS"}""")
        assertEquals(listOf("192.168.1.20:8443"), qr.a)
    }

    @Test fun rejectsOtherCodes() {
        assertThrows(IllegalArgumentException::class.java) { parsePairingQr("https://example.com") }
        assertThrows(IllegalArgumentException::class.java) { parsePairingQr("""{"v":1,"hub":"synbee","a":["8.8.8.8:8443"],"fp":"$fp","c":"3CTCH-EE6MS"}""") }
        assertThrows(IllegalArgumentException::class.java) { parsePairingQr("""{"v":2,"hub":"synbee","a":["192.168.1.20:8443"],"fp":"$fp","c":"3CTCH-EE6MS"}""") }
        assertThrows(IllegalArgumentException::class.java) { parsePairingQr("""{"v":1,"hub":"synbee","a":["192.168.1.20:8443"],"fp":"short","c":"3CTCH-EE6MS"}""") }
    }

    @Test fun checksTypedValues() {
        assertTrue(isHomeAddress("192.168.1.20:8443"))
        assertFalse(isHomeAddress("192.168.1.20"))
        assertFalse(isHomeAddress("example.com:8443"))
        assertTrue(isValidCode("k7m4q-x92pa"))
        assertTrue(isValidCode("K7M4QX92PA"))
        assertFalse(isValidCode("K7M"))
    }

    @Test fun showsTheSameShortKeyAsTheHubPage() {
        // The hub's web page computes the same 16 hex digits (web/src/api.ts, shortFingerprint).
        assertEquals("B123 DB35 3F8B 433A", shortFingerprint(fp))
    }

    @Test fun keepsTheOutsideAddressFromTheCode() {
        val qr = parsePairingQr("""{"v":1,"hub":"synbee","a":["192.168.1.20:8443"],"o":["myhome.synology.me:18443","nope"],"fp":"$fp","c":"3CTCH-EE6MS"}""")
        assertEquals(listOf("myhome.synology.me:18443"), qr.o)
    }

    @Test fun triesHomeFirstOnWifiAndOutsideFirstElsewhere() {
        val home = listOf("192.168.1.20:8443")
        val away = listOf("myhome.synology.me:18443")
        assertEquals(home + away, orderAddresses(home, away, onWifi = true))
        assertEquals(away + home, orderAddresses(home, away, onWifi = false))
    }
}
