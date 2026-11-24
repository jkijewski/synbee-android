package pl.kijewski.synbee.data

import kotlinx.serialization.Serializable

// The app API's answers (v1), as the hub's OpenAPI document describes them: openapi/app-api.json in the SynBee
// repository, generated from the hub's route schemas. Fields the app doesn't know are ignored, so a newer hub that
// adds fields keeps working; a breaking change gets a new version prefix.

/** Where the hub can be reached, and whether this phone may use it away from home (the owner allows it per phone). */
@Serializable data class Connection(
    val home: List<String> = emptyList(),
    val outside: List<String> = emptyList(),
    val outsideAllowed: Boolean = false,
    val hubAllowsOutside: Boolean = false,
)
@Serializable data class Ping(val hub: String, val api: Int, val version: String)
@Serializable data class Paired(val token: String, val member: String, val phone: String)
/** An action on the home screen; `state` is active, partly or inactive (null when unknown), `status` the tile's line. */
@Serializable data class HubAction(
    val id: Int,
    val name: String,
    val icon: String = "",
    val description: String = "",
    val rooms: List<Int> = emptyList(),
    val state: String? = null,
    /** Whether what the action switches is on, partly on or off; null when unknown. */
    val power: String? = null,
    val status: String = "",
)
@Serializable data class Reading(val property: String, val label: String, val text: String, val attention: String? = null)
/** A device in words, for the details sheet and the room page. */
@Serializable data class DeviceView(
    val ieee: String,
    val name: String,
    val room: String? = null,
    val online: Boolean? = null,
    val summary: String = "",
    val readings: List<Reading> = emptyList(),
    val battery: Double? = null,
    val lastSeen: String? = null,
)
/** What an action touches: a device, or a light group with its devices. */
@Serializable data class Target(val kind: String, val device: DeviceView? = null, val name: String? = null, val summary: String = "", val devices: List<DeviceView> = emptyList())
@Serializable data class ActionDetails(val id: Int, val name: String, val state: String? = null, val status: String = "", val targets: List<Target>)
@Serializable data class RoomPage(val id: Int, val name: String, val icon: String = "", val color: String = "#7aa7e0", val devices: List<DeviceView>)
@Serializable data class HubRoom(val id: Int, val name: String, val icon: String = "", val color: String = "#7aa7e0")
/** The app's home screen: the member's rooms, in the owner's order and colours, and their actions. */
@Serializable data class Home(val rooms: List<HubRoom>, val actions: List<HubAction>, val tiles: List<StatusTile> = emptyList())
/** A read-only status tile: a device the owner picked, a sensor group or a check. `level`: fine, info, warning or alarm. */
@Serializable data class StatusTile(
    val id: String,
    val kind: String,
    val room: Int? = null,
    val name: String,
    val icon: String = "",
    val text: String = "",
    val level: String = "info",
    /** The explanation shown on a press, line by line. */
    val why: List<String> = emptyList(),
    /** A colour hint as #rrggbb, e.g. a temperature from blue to red. */
    val color: String? = null,
)
/** Alerts paused by someone, until when (ms since 1970). */
@Serializable data class AlertPause(val until: Long, val by: String)
@Serializable data class PauseResult(val paused: AlertPause? = null)
/** `notRun` is set when the action's conditions weren't met, so nothing ran: what wasn't true. */
@Serializable data class RunResult(val ok: Boolean, val failedStep: Int? = null, val error: String? = null, val notRun: String? = null)
@Serializable data class Me(
    val member: String,
    val phone: String,
    val hubVersion: String,
    val actions: Int,
    val rooms: Int,
    /** Whether this member may pause alerts (the owner allows it on the hub). */
    val mayPauseAlerts: Boolean = false,
    val alertsPaused: AlertPause? = null,
)
