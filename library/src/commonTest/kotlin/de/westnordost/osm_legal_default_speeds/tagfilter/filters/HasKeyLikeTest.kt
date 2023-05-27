package de.westnordost.osm_legal_default_speeds.tagfilter.filters

import kotlin.test.*

internal class HasKeyLikeTest {
    private val key = HasKeyLike("n.[ms]e")

    @Test fun matches() {
        assertTrue(key.matches(mapOf("name" to "adsf")))
        assertTrue(key.matches(mapOf("nase" to "fefff")))
        assertTrue(key.matches(mapOf("neme" to "no")))
        assertFalse(key.matches(mapOf("a name yo" to "no")))
        assertTrue(key.matches(mapOf("n(se" to "no")))
        assertFalse(key.matches(mapOf()))
    }

    @Test fun toStringMethod() {
        assertEquals("~n.[ms]e", key.toString())
    }

    @Test fun relevantKey() {
        assertEquals(RelevantKeyRegex(RegexOrSet.from("n.[ms]e")), key.relevantKey)
    }
}
