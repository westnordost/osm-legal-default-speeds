package de.westnordost.osm_default_speeds.tagfilter

internal object TestBooleanExpressionParser {
    fun parse(input: String): BooleanExpression<Matcher<String>, String>? {
        val builder = BooleanExpressionBuilder<Matcher<String>, String>()
        val reader = StringWithCursor(input)
        while (!reader.isAtEnd()) {
            val match = reader.nextMatchesAndAdvance(Regex("(!?)([A-Z])"))
            if (match != null) {
                val (not, placeholder) = match.destructured
                if (not == "!") builder.addNotPlaceholder(placeholder)
                else            builder.addPlaceholder(placeholder)
            } else {
                when {
                    reader.nextIsAndAdvance('*') -> builder.addAnd()
                    reader.nextIsAndAdvance('+') -> builder.addOr()
                    reader.nextIsAndAdvance('(') -> builder.addOpenBracket()
                    reader.nextIsAndAdvance(')') -> builder.addCloseBracket()

                    else -> builder.addValue(TestBooleanExpressionValue(reader.advance().toString()))
                }
            }
        }
        return builder.build()
    }
}
