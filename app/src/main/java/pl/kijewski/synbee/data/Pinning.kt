package pl.kijewski.synbee.data

import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Base64
import javax.net.ssl.X509TrustManager

/** SHA-256 of the certificate's SubjectPublicKeyInfo, base64: the same value the hub shows. */
fun spkiFingerprint(cert: X509Certificate): String =
    Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(cert.publicKey.encoded))

/**
 * Trusts exactly one key. The hub's certificate is self-signed and reached by IP or any domain, so the
 * pinned key replaces both the CA chain and the hostname check.
 */
class PinnedKeyTrustManager(private val pin: String) : X509TrustManager {
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw CertificateException("No certificate")
        leaf.checkValidity()
        if (spkiFingerprint(leaf) != pin) throw PinMismatchException()
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) =
        throw CertificateException("Client certificates are not used")

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

class PinMismatchException : CertificateException("The hub's key doesn't match the paired key")

/** Only used once, before pairing by hand, to read and show the key fingerprint for the owner to confirm. */
class CaptureTrustManager : X509TrustManager {
    @Volatile var seen: X509Certificate? = null
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        seen = chain?.firstOrNull()
    }
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) =
        throw CertificateException("Client certificates are not used")
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
