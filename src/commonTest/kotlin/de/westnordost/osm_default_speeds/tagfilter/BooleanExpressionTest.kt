package de.westnordost.osm_default_speeds.tagfilter

import kotlin.test.*

internal class BooleanExpressionTest {

    @Test fun match_leaf() {
        assertTrue(evalExpression("1"))
        assertFalse(evalExpression("0"))
    }

    @Test fun match_or() {
        assertTrue(evalExpression("1+1"))
        assertTrue(evalExpression("1+0"))
        assertTrue(evalExpression("0+1"))
        assertFalse(evalExpression("0+0"))

        assertTrue(evalExpression("0+0+1"))
    }

    @Test fun match_and() {
        assertTrue(evalExpression("1*1"))
        assertFalse(evalExpression("1*0"))
        assertFalse(evalExpression("0*1"))
        assertFalse(evalExpression("0*0"))

        assertTrue(evalExpression("1*1*1"))
        assertFalse(evalExpression("1*1*0"))
    }

    @Test fun match_and_in_or() {
        assertTrue(evalExpression("(1*0)+1"))
        assertFalse(evalExpression("(1*0)+0"))
        assertTrue(evalExpression("(1*1)+0"))
        assertTrue(evalExpression("(1*1)+1"))
    }

    @Test fun match_or_in_and() {
        assertTrue(evalExpression("(1+0)*1"))
        assertFalse(evalExpression("(1+0)*0"))
        assertFalse(evalExpression("(0+0)*0"))
        assertFalse(evalExpression("(0+0)*1"))
    }

    private fun evalExpression(input: String): Boolean {
        val expr = TestBooleanExpressionParser.parse(input)
        return expr!!.matches("1") { false }
    }
}
