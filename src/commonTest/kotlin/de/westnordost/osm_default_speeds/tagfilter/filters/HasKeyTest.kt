package de.westnordost.osm_default_speeds.tagfilter.filters

import kotlin.test.*

internal class HasKeyTest {
    private val key = HasKey("name")

    @Test fun matches() {
        assertTrue(key.matches(mapOf("name" to "yes")))
        assertTrue(key.matches(mapOf("name" to "no")))
        assertFalse(key.matches(mapOf("neme" to "no")))
        assertFalse(key.matches(mapOf()))
    }

    @Test fun toStringMethod() {
        assertEquals("name", key.toString())
    }
}
