package de.westnordost.osm_legal_default_speeds.tagfilter.filters

import kotlin.test.*

internal class NotHasKeyLikeTest {

    @Test fun matches() {
        val key = NotHasKeyLike("n.[ms]e")

        assertFalse(key.matches(mapOf("name" to "adsf")))
        assertFalse(key.matches(mapOf("nase" to "fefff")))
        assertFalse(key.matches(mapOf("neme" to "no")))
        assertTrue(key.matches(mapOf("a name yo" to "no", "another name yo" to "no")))
        assertFalse(key.matches(mapOf("n(se" to "no")))
        assertTrue(key.matches(mapOf()))
    }

    @Test fun toStringMethod() {
        assertEquals("!~n.[ms]e", NotHasKeyLike("n.[ms]e").toString())
    }

    @Test fun relevantKey() {
        val key = NotHasKeyLike("n.[ms]e")
        assertEquals(RelevantKeyRegex(RegexOrSet.from("n.[ms]e")), key.relevantKey)
    }
}
