package de.westnordost.osm_default_speeds.tagfilter.filters

import kotlin.test.*

class NotHasTagValueLikeTest {

    @Test fun matches_not_like_dot() {
        val f = NotHasTagValueLike("highway", ".*")

        assertFalse(f.matches(mapOf("highway" to "anything")))
        assertTrue(f.matches(mapOf()))
    }

    @Test fun matches_not_like_or() {
        val f = NotHasTagValueLike("noname", "yes")

        assertFalse(f.matches(mapOf("noname" to "yes")))
        assertTrue(f.matches(mapOf("noname" to "no")))
        assertTrue(f.matches(mapOf()))
    }

    @Test fun toStringMethod() {
        assertEquals("highway !~ .*", NotHasTagValueLike("highway", ".*").toString())
    }
}
