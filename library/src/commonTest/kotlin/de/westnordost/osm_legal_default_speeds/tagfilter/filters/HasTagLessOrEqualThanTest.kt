package de.westnordost.osm_legal_default_speeds.tagfilter.filters

import kotlin.test.*

internal class HasTagLessOrEqualThanTest {
    val c = HasTagLessOrEqualThan("width", 3.5f)

    @Test fun matches() {
        assertFalse(c.matches(mapOf()))
        assertFalse(c.matches(mapOf("width" to "broad")))
        assertFalse(c.matches(mapOf("width" to "3.6")))
        assertTrue(c.matches(mapOf("width" to "3.5")))
        assertTrue(c.matches(mapOf("width" to "3.4")))
    }

    @Test fun toStringMethod() {
        assertEquals("width <= 3.5", c.toString())
    }

    @Test fun relevantKey() {
        assertEquals(RelevantKeyString("width"), c.relevantKey)
    }
}
