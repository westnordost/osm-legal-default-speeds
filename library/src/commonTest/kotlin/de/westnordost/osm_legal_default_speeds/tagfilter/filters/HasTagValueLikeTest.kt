package de.westnordost.osm_legal_default_speeds.tagfilter.filters

import kotlin.test.*

internal class HasTagValueLikeTest {

    @Test fun matches_like_dot() {
        val f = HasTagValueLike("highway", ".esidential")

        assertTrue(f.matches(mapOf("highway" to "residential")))
        assertTrue(f.matches(mapOf("highway" to "wesidential")))
        assertFalse(f.matches(mapOf("highway" to "rresidential")))
        assertFalse(f.matches(mapOf()))
    }

    @Test fun matches_like_or() {
        val f = HasTagValueLike("highway", "residential|unclassified")

        assertTrue(f.matches(mapOf("highway" to "residential")))
        assertTrue(f.matches(mapOf("highway" to "unclassified")))
        assertFalse(f.matches(mapOf("highway" to "blub")))
        assertFalse(f.matches(mapOf()))
    }

    @Test fun matches_like_character_class() {
        val f = HasTagValueLike("maxspeed", "([1-9]|[1-2][0-9]|3[0-5]) mph")

        assertTrue(f.matches(mapOf("maxspeed" to "1 mph")))
        assertTrue(f.matches(mapOf("maxspeed" to "5 mph")))
        assertTrue(f.matches(mapOf("maxspeed" to "15 mph")))
        assertTrue(f.matches(mapOf("maxspeed" to "25 mph")))
        assertTrue(f.matches(mapOf("maxspeed" to "35 mph")))
        assertFalse(f.matches(mapOf("maxspeed" to "40 mph")))
        assertFalse(f.matches(mapOf("maxspeed" to "45 mph")))
        assertFalse(f.matches(mapOf("maxspeed" to "135 mph")))
        assertFalse(f.matches(mapOf()))
    }

    @Test fun toStringMethod() {
        assertEquals("highway ~ .esidential", HasTagValueLike("highway", ".esidential").toString())
    }

    @Test fun relevantKey() {
        val f = HasTagValueLike("highway", "residential|unclassified")
        assertEquals(RelevantKeyString("highway"), f.relevantKey)
    }
}
