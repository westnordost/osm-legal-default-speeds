package de.westnordost.osm_legal_default_speeds.tagfilter.filters

import kotlin.test.*

internal class HasTagTest {
    val c = HasTag("highway", "residential")

    @Test fun matches() {
        assertTrue(c.matches(mapOf("highway" to "residential")))
        assertFalse(c.matches(mapOf("highway" to "residental")))
        assertFalse(c.matches(mapOf("hipway" to "residential")))
        assertFalse(c.matches(mapOf()))
    }

    @Test fun toStringMethod() {
        assertEquals("highway = residential", c.toString())
    }

    @Test fun relevantKey() {
        assertEquals(RelevantKeyString("highway"), c.relevantKey)
    }
}
