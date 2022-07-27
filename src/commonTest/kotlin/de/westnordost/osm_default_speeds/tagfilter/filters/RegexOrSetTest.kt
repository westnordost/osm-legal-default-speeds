package de.westnordost.osm_default_speeds.tagfilter.filters

import kotlin.test.*

internal class RegexOrSetTest {
    @Test fun pipesMatch() {
        val r = RegexOrSet.from("a|b|c")
        assertTrue(r.matches("a"))
        assertTrue(r.matches("b"))
        assertTrue(r.matches("c"))
        assertFalse(r.matches("d"))
        assertFalse(r.matches("a|b"))
    }
}
