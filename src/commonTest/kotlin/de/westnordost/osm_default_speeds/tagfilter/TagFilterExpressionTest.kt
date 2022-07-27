package de.westnordost.osm_default_speeds.tagfilter

import de.westnordost.osm_default_speeds.tagfilter.filters.HasKey
import de.westnordost.osm_default_speeds.tagfilter.filters.NotHasKey
import kotlin.test.*

class TagFilterExpressionTest {

    @Test fun matches_filter() {
        val expr1 = Leaf(HasKey("bla"))

        assertTrue(expr1.matches(mapOf("bla" to "1")))
        assertFalse(expr1.matches(mapOf("foo" to "1")))
        assertFalse(expr1.matches(mapOf()))

        // to test mayEvaluateToTrueWithNoTags
        val expr2 = Leaf(NotHasKey("bla"))
        assertTrue(expr2.matches(mapOf()))
        assertFalse(expr2.matches(mapOf("bla" to "1")))
        assertTrue(expr2.matches(mapOf("foo" to "1")))
    }
}
