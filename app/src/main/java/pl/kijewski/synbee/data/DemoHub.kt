package pl.kijewski.synbee.data

/**
 * A sample home kept in memory, for trying the app without a hub (and for store review and screenshots). It answers
 * like a hub: rooms, actions with their state, devices in words; running an action switches its lights here only.
 * Nothing is sent anywhere.
 */
class DemoHub : Hub {
    override val lastGood: String? = null

    private class Light(val ieee: String, val name: String, val room: String, var on: Boolean, var brightness: Int = 80, var colour: String? = null)

    private val rooms = listOf(
        HubRoom(1, "Living room", "weekend", "#e8a33d"),
        HubRoom(2, "Bedroom", "bed", "#7aa7e0"),
        HubRoom(3, "Kitchen", "kitchen", "#7cbf7c"),
    )
    private val roomName = rooms.associate { it.id to it.name }

    private val lights = listOf(
        Light("0x0000000000000001", "Ceiling light", "Living room", on = true, brightness = 70),
        Light("0x0000000000000002", "Reading lamp", "Living room", on = false),
        Light("0x0000000000000003", "LED strip", "Bedroom", on = false, colour = "warm white"),
        Light("0x0000000000000004", "Bedside lamp", "Bedroom", on = true, brightness = 30),
        Light("0x0000000000000005", "Worktop lights", "Kitchen", on = false),
    ).associateBy { it.ieee }

    private val sensors = mapOf(
        "Living room" to listOf(
            DeviceView("0x0000000000000011", "Climate sensor", "Living room", true, "21.4 °C · 48 %",
                listOf(Reading("temperature", "Temperature", "21.4 °C"), Reading("humidity", "Humidity", "48 %")), 87.0, null),
        ),
        "Bedroom" to listOf(
            DeviceView("0x0000000000000012", "Window", "Bedroom", true, "Closed", listOf(Reading("contact", "Window", "Closed")), 64.0, null),
        ),
        "Kitchen" to listOf(
            DeviceView("0x0000000000000013", "Leak sensor", "Kitchen", true, "Dry", listOf(Reading("water_leak", "Water", "Dry")), 92.0, null),
        ),
    )

    private class DemoAction(val id: Int, val name: String, val icon: String, val description: String, val room: Int, val lights: List<String>, val on: Boolean, val brightness: Int? = null)

    private val actions = listOf(
        DemoAction(1, "Living room on", "lightbulb", "Ceiling light and reading lamp", 1, listOf("0x0000000000000001", "0x0000000000000002"), on = true),
        DemoAction(2, "Movie time", "movie", "Reading lamp only, dimmed", 1, listOf("0x0000000000000001", "0x0000000000000002"), on = true, brightness = 20),
        DemoAction(3, "Living room off", "light_off", "Everything off", 1, listOf("0x0000000000000001", "0x0000000000000002"), on = false),
        DemoAction(4, "Good night", "bedtime", "Bedroom lights off", 2, listOf("0x0000000000000003", "0x0000000000000004"), on = false),
        DemoAction(5, "Bedside lamp", "nightlight", "Bedside lamp on, low", 2, listOf("0x0000000000000004"), on = true, brightness = 30),
        DemoAction(6, "Kitchen lights", "countertops", "Worktop lights on", 3, listOf("0x0000000000000005"), on = true),
    )

    private fun device(l: Light) = DeviceView(
        ieee = l.ieee,
        name = l.name,
        room = l.room,
        online = true,
        summary = if (l.on) listOfNotNull("On", l.colour, "${l.brightness} %").joinToString(" · ") else "Off",
        readings = emptyList(),
    )

    private fun matches(a: DemoAction): Boolean = a.lights.map { lights.getValue(it) }.let { ls ->
        when {
            a.id == 2 -> !ls[0].on && ls[1].on && ls[1].brightness == 20
            a.on -> ls.all { it.on && (a.brightness == null || it.brightness == a.brightness) }
            else -> ls.none { it.on }
        }
    }

    private fun view(a: DemoAction): HubAction {
        val ls = a.lights.map { lights.getValue(it) }
        val onCount = ls.count { it.on }
        val power = if (!a.on) null else if (matches(a)) "on" else if (onCount > 0) "partly" else "off"
        val status = when {
            ls.size == 1 -> if (ls[0].on) "On" else "Off"
            onCount == 0 -> "Off"
            onCount == ls.size -> "On"
            else -> "$onCount of ${ls.size} on"
        }
        return HubAction(a.id, a.name, a.icon, a.description, listOf(a.room), if (matches(a)) "active" else "inactive", power, status)
    }

    private var paused: AlertPause? = null

    override suspend fun me() = Me("Demo", "This phone", "demo", actions.size, rooms.size, mayPauseAlerts = true, alertsPaused = paused?.takeIf { it.until > System.currentTimeMillis() })

    override suspend fun pauseAlerts(minutes: Int): PauseResult {
        paused = if (minutes == 0) null else AlertPause(System.currentTimeMillis() + minutes * 60_000L, "Demo (this phone)")
        return PauseResult(paused)
    }

    private fun tiles(): List<StatusTile> {
        val bedroomLit = lights.values.filter { it.room == "Bedroom" }.count { it.on }
        return listOf(
            StatusTile("device:climate", "device", 1, "Living room", "thermostat", "21.4 °C", "info", listOf("Temperature: 21.4 °C", "Humidity: 48 %", "Battery: 87 %"), color = "#4aba6a"),
            StatusTile("sensors:windows", "sensors", 2, "Windows", "sensor_door", "All closed", "fine", emptyList()),
            StatusTile(
                "check:night", "check", 2, "Ready for the night", if (bedroomLit == 0) "task_alt" else "report",
                if (bedroomLit == 0) "Ready: lights off, windows closed" else "Not yet", if (bedroomLit == 0) "fine" else "warning",
                if (bedroomLit == 0) listOf("Windows: All closed", "Bedroom lights are off") else listOf("$bedroomLit bedroom light(s) on"),
            ),
            StatusTile("device:leak", "device", 3, "Leak sensor", "water_damage", "Dry", "fine", listOf("Water: Dry", "Battery: 92 %")),
        )
    }

    override suspend fun connection() = Connection()

    override suspend fun home() = Home(rooms, actions.map(::view), tiles())

    override suspend fun details(id: Int): ActionDetails {
        val a = actions.firstOrNull { it.id == id } ?: throw HubRefusedException(404, "Not found")
        val v = view(a)
        return ActionDetails(a.id, a.name, v.state, v.status, a.lights.map { Target("device", device(lights.getValue(it))) })
    }

    override suspend fun room(id: Int): RoomPage {
        val r = rooms.firstOrNull { it.id == id } ?: throw HubRefusedException(404, "Not found")
        val name = roomName.getValue(id)
        return RoomPage(r.id, r.name, r.icon, r.color, lights.values.filter { it.room == name }.map(::device) + (sensors[name] ?: emptyList()))
    }

    override suspend fun run(id: Int): RunResult {
        val a = actions.firstOrNull { it.id == id } ?: throw HubRefusedException(404, "Not found")
        if (a.id == 2) {
            lights.getValue(a.lights[0]).on = false
            lights.getValue(a.lights[1]).apply { on = true; brightness = 20 }
        } else {
            for (ieee in a.lights) {
                val l = lights.getValue(ieee)
                l.on = a.on
                if (a.brightness != null) l.brightness = a.brightness else if (a.on) l.brightness = maxOf(l.brightness, 80)
            }
        }
        return RunResult(ok = true)
    }
}
