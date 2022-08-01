package de.westnordost.osm_legal_default_speeds.tagfilter.filters

import kotlin.test.*

internal class NotHasTagTest {
    val c = NotHasTag("highway", "residential")

    @Test fun matches() {
        assertFalse(c.matches(mapOf("highway" to "residential")))
        assertTrue(c.matches(mapOf("highway" to "residental")))
        assertTrue(c.matches(mapOf("hipway" to "residential")))
        assertTrue(c.matches(mapOf()))
    }

    @Test fun toStringMethod() {
        assertEquals("highway != residential", c.toString())
    }
}
