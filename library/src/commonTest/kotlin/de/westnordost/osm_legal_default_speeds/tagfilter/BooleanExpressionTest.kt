package de.westnordost.osm_legal_default_speeds.tagfilter

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

    @Test fun placeholder() {
        assertTrue(evalExpression("A") { it == "A" })
        assertFalse(evalExpression("A") { it == "B" })

        assertFalse(evalExpression("!A") { it == "A" })
        assertTrue(evalExpression("!A") { it == "B" })
    }

    @Test fun placeholder_or() {
        assertTrue(evalExpression("A+B") { it == "A" })
        assertTrue(evalExpression("A+B") { it == "B" })
        assertFalse(evalExpression("A+B") { it == "C" })
    }

    @Test fun placeholder_and() {
        assertTrue(evalExpression("A*B") { it == "A" || it == "B" })
        assertFalse(evalExpression("A*B") { it == "A" })
        assertFalse(evalExpression("A*B") { it == "B" })
    }

    @Test fun list_placeholders() {
        assertEquals(listOf("A","B"), getPlaceholders("A+B"))
        assertEquals(listOf("A","B","C"), getPlaceholders("A+B+C"))
        assertEquals(listOf("A","B","A","C"), getPlaceholders("A*(B+A)*C"))
    }

    @Test fun list_items() {
        assertEquals(listOf("1","2"), getItems("1+2"))
        assertEquals(listOf("1","2"), getItems("1*2"))
        assertEquals(listOf("1","2","3"), getItems("1*(2+3)"))
        assertEquals(listOf("1","2","3"), getItems("(1*2)+3"))
    }

    private fun getItems(input: String): List<String> {
        val expr = TestBooleanExpressionParser.parse(input) as Chain
        return expr.getItems().map { it.toString() }.toList()
    }

    private fun getPlaceholders(input: String): List<String> {
        val expr = TestBooleanExpressionParser.parse(input) as Chain
        return expr.getPlaceholders().toList()
    }

    private fun evalExpression(input: String, evaluate: (String) -> Boolean = { false }): Boolean {
        val expr = TestBooleanExpressionParser.parse(input)
        return expr!!.matches("1", evaluate)
    }
}
