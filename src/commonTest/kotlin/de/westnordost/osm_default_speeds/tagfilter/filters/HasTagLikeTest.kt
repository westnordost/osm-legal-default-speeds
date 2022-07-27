package de.westnordost.osm_default_speeds.tagfilter.filters

import kotlin.test.*

class HasTagLikeTest {

    @Test fun matches_regex_key_and_value() {
        val f = HasTagLike(".ame", "y.s")

        assertTrue(f.matches(mapOf("name" to "yes")))
        assertTrue(f.matches(mapOf("lame" to "yos")))
        assertFalse(f.matches(mapOf("lame" to "no")))
        assertFalse(f.matches(mapOf("good" to "yes")))
        assertFalse(f.matches(mapOf("neme" to "no")))
        assertFalse(f.matches(mapOf("names" to "yess"))) // only entire string is matched
        assertFalse(f.matches(mapOf()))
    }

    @Test fun matches_exact_value_of_tag_if_without_regexp() {
        val f = HasTagLike("shop", "cheese")

        assertTrue(f.matches(mapOf("shop" to "cheese")))
        assertFalse(f.matches(mapOf("shop" to "cheese_frog_swamp")))
    }

    @Test fun matches_any_exact_value_of_pipelid_list_and_otherwise_without_regexp() {
        val f = HasTagLike("shop", "cheese|greengrocer")

        assertTrue(f.matches(mapOf("shop" to "cheese")))
        assertTrue(f.matches(mapOf("shop" to "greengrocer")))
        assertFalse(f.matches(mapOf("shop" to "cheese_frog_swamp")))
        assertFalse(f.matches(mapOf("shop" to "cheese|greengrocer")))
    }

    @Test fun toStringMethod() {
        assertEquals("~.ame ~ y.s", HasTagLike(".ame", "y.s").toString())
    }
}
