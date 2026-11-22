package pl.kijewski.synbee

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.kijewski.synbee.data.DemoHub

class DemoHubTest {
    @Test fun `answers like a hub and switches its sample lights`() = runTest {
        val hub = DemoHub()
        val home = hub.home()
        assertEquals(listOf("Living room", "Bedroom", "Kitchen"), home.rooms.map { it.name })
        assertTrue(home.actions.all { it.rooms.size == 1 && it.rooms[0] in home.rooms.map { r -> r.id } })
        val on = home.actions.first { it.name == "Living room on" }
        assertEquals("1 of 2 on", on.status)
        assertEquals("partly", on.power)
        assertTrue(hub.run(on.id).ok)
        val after = hub.home().actions.first { it.id == on.id }
        assertEquals("active", after.state)
        assertEquals("on", after.power)
        // Off actions are never coloured, and show active once everything is off.
        hub.run(home.actions.first { it.name == "Living room off" }.id)
        val off = hub.home().actions.first { it.name == "Living room off" }
        assertEquals("active", off.state)
        assertEquals(null, off.power)
        assertEquals(3, hub.room(1).devices.size) // two lamps and the climate sensor
        assertEquals(2, hub.details(on.id).targets.size)
    }
}
