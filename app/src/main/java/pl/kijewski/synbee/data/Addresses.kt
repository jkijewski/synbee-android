package pl.kijewski.synbee.data

/** True for hosts on a home network: private IPv4 ranges, IPv6 unique-local and link-local. */
fun isLocalHost(host: String): Boolean {
    val h = host.removePrefix("[").removeSuffix("]").lowercase()
    val parts = h.split('.')
    if (parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }) {
        val (a, b) = parts[0].toInt() to parts[1].toInt()
        return a == 10 || (a == 172 && b in 16..31) || (a == 192 && b == 168) || (a == 169 && b == 254)
    }
    return h.startsWith("fc") || h.startsWith("fd") || h.startsWith("fe8") || h.startsWith("fe9") || h.startsWith("fea") || h.startsWith("feb")
}

fun hostOf(address: String): String = address.substringBeforeLast(':')

/** The order to try addresses in: home first on Wi-Fi, outside first elsewhere, each list in its own order. */
fun orderAddresses(home: List<String>, outside: List<String>, onWifi: Boolean): List<String> =
    (if (onWifi) home + outside else outside + home).distinct()
