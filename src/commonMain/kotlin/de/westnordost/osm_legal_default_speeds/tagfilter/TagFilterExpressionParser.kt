package de.westnordost.osm_legal_default_speeds.tagfilter

import de.westnordost.osm_legal_default_speeds.tagfilter.filters.*
import kotlin.math.min

/** Compiles a string in filter syntax into a TagFilterExpression. A string in filter syntax is
 *  something like this:
 *
 *  `(highway = residential or highway = tertiary) and !name`
 *
 *  [matches] of an [TagFilterExpression] parsed from the above string returns true for any
 *  residential or tertiary roads that have no name tagged.
 *
 *  ### Cheatsheet for element filter syntax:
 *  | expression                     | [matches] returns `true` if element…                                          |
 *  | :----------------------------- | :---------------------------------------------------------------------------- |
 *  | `shop`                         | has a tag with key `shop`                                                     |
 *  | `!shop`                        | doesn't have a tag with key `shop`                                            |
 *  | `shop = car`                   | has a tag with key `shop` whose value is `car`                                |
 *  | `shop != car`                  | doesn't have a tag with key `shop` whose value is `car`                       |
 *  | `~shop|craft`                  | has a tag whose key matches the regex `shop|craft`                            |
 *  | `!~shop|craft`                 | doesn't have a tag whose key matches the regex `shop|craft`                   |
 *  | `shop ~ car|boat`              | has a tag whose key is `shop` and whose value matches the regex `car|boat`    |
 *  | `shop !~ car|boat`             | doesn't have a tag whose key is `shop` and value matches the regex `car|boat` |
 *  | `~shop|craft ~ car|boat`       | has a tag whose key matches `shop|craft` and value `car|boat` (both regexes)  |
 *  | `foo < 3.3`                    | has a tag with key `foo` whose value is smaller than 2.5<br/>`<`,`<=`,`>=`,`>` work likewise |
 *  | `foo < 3.3ft`                  | same as above but value is smaller than 3.3 feet (~1 meter)<br/>This works for other units as well (mph, st, lbs, yds...) |
 *  | `foo < 3'4"`                   | same as above but value is smaller than 3 feet, 4 inches (~1 meter)           |
 *  | `foo < 2012-10-01`             | same as above but value is a date older than Oct 1st 2012                     |
 *  | `foo < today -1.5 years`       | same as above but value is a date older than 1.5 years<br/>In place of `years`, `months`, `weeks` or `days` work |
 *  | `shop newer today -99 days`    | has a tag with key `shop` which has been modified in the last 99 days.<br/>Absolute dates work too. |
 *  | `shop older today -1 months`   | has a tag with key `shop` which hasn't been changed for more than a month.<br/>Absolute dates work too. |
 *  | `shop and name`                | has both a tag with key `shop` and one with key `name`                        |
 *  | `shop or craft`                | has either a tag with key `shop` or one with key `craft`                      |
 *  | `shop and (ref or name)`       | has a tag with key `shop` and either a tag with key `ref` or `name`           |
 *
 *  ### Equivalent expressions
 *  | expression                     | equivalent expression                                    |
 *  | :----------------------------- | :------------------------------------------------------- |
 *  | `shop and shop = boat`         | `shop = boat`                                            |
 *  | `!shop or shop != boat`        | `shop != boat`                                           |
 *  | `shop = car or shop = boat`    | `shop ~ car|boat`                                        |
 *  | `craft or shop and name`       | `craft or (shop and name)` (`and` has higher precedence) |
 *  | `!(amenity and craft)`         | **<error>** (negation of expression not supported)       |
 *  */

private const val OR = "or"
private const val AND = "and"

private const val EQUALS = "="
private const val NOT_EQUALS = "!="
private const val LIKE = "~"
private const val PLACEHOLDER_START = "{"
private const val PLACEHOLDER_END = "}"
private const val NOT = "!"
private const val NOT_LIKE = "!~"
private const val GREATER_THAN = ">"
private const val LESS_THAN = "<"
private const val GREATER_OR_EQUAL_THAN = ">="
private const val LESS_OR_EQUAL_THAN = "<="

private val RESERVED_WORDS = arrayOf(OR, AND)
private val QUOTATION_MARKS = charArrayOf('"', '\'')
private val KEY_VALUE_OPERATORS = setOf(EQUALS, NOT_EQUALS, LIKE, NOT_LIKE)
private val COMPARISON_OPERATORS = setOf(
    GREATER_THAN, GREATER_OR_EQUAL_THAN,
    LESS_THAN, LESS_OR_EQUAL_THAN
)
// must be in that order because if ">=" would be after ">", parser would match ">" also when encountering ">="
private val OPERATORS = linkedSetOf(
    GREATER_OR_EQUAL_THAN,
    LESS_OR_EQUAL_THAN,
    GREATER_THAN,
    LESS_THAN,
    NOT_EQUALS,
    EQUALS,
    NOT_LIKE,
    LIKE
)

private val ESCAPED_QUOTE_REGEX = Regex("\\\\(['\"])")
private val WHITESPACE_REGEX = Regex("\\s")
private val WHITESPACES_REGEX = Regex("\\s*")

internal fun StringWithCursor.parseTags(): BooleanExpression<TagFilter, Map<String, String>> {
    val builder = BooleanExpressionBuilder<TagFilter, Map<String, String>>()
    var first = true

    do {
        // if it has no bracket, there must be at least one whitespace
        if (!parseBracketsAndSpaces('(', builder) && !first) {
            throw ParseException("Expected a whitespace or bracket before the tag", cursorPos)
        }
        first = false

        if (nextIsAndAdvance(NOT + PLACEHOLDER_START)) {
            builder.addNotPlaceholder(parsePlaceholder())
        } else if (nextIsAndAdvance(PLACEHOLDER_START)) {
            builder.addPlaceholder(parsePlaceholder())
        } else {
            builder.addValue(parseTag())
        }

        val separated = parseBracketsAndSpaces(')', builder)

        if (isAtEnd()) break

        // same as with the opening bracket, only that if the string is over, its okay
        if (!separated) {
            throw ParseException("Expected a whitespace or bracket after the tag", cursorPos)
        }

        if (nextIsAndAdvance(OR)) {
            builder.addOr()
        } else if (nextIsAndAdvance(AND)) {
            builder.addAnd()
        } else {
            throw ParseException("Expected end of string, '$AND' or '$OR'", cursorPos)
        }
    } while (true)

    try {
        return builder.build()!!
    } catch (e: IllegalStateException) {
        throw ParseException(e.message, cursorPos)
    }
}

private fun StringWithCursor.parseBracketsAndSpaces(bracket: Char, expr: BooleanExpressionBuilder<*, *>): Boolean {
    val initialCursorPos = cursorPos
    do {
        val loopStartCursorPos = cursorPos
        expectAnyNumberOfSpaces()
        if (nextIsAndAdvance(bracket)) {
            try {
                if (bracket == '(')      expr.addOpenBracket()
                else if (bracket == ')') expr.addCloseBracket()
            } catch (e: IllegalStateException) {
                throw ParseException(e.message, cursorPos)
            }
        }
    } while (loopStartCursorPos < cursorPos)
    expectAnyNumberOfSpaces()
    return initialCursorPos < cursorPos
}

private fun StringWithCursor.parseTag(): TagFilter {
    if (nextIsAndAdvance(NOT)) {
        if (nextIsAndAdvance(LIKE)) {
            expectAnyNumberOfSpaces()
            return NotHasKeyLike(parseKey())
        } else {
            expectAnyNumberOfSpaces()
            return NotHasKey(parseKey())
        }
    }

    if (nextIsAndAdvance(LIKE)) {
        expectAnyNumberOfSpaces()
        val key = parseKey()
        val operator = parseOperatorWithSurroundingSpaces()
        if (operator == null) {
            return HasKeyLike(key)
        } else if (LIKE == operator) {
            return HasTagLike(key, parseQuotableWord())
        }
        throw ParseException("Unexpected operator '$operator': The key prefix operator '$LIKE' must be used together with the binary operator '$LIKE'", cursorPos)
    }

    val key = parseKey()
    val operator = parseOperatorWithSurroundingSpaces() ?: return HasKey(key)

    if (operator in KEY_VALUE_OPERATORS) {
        val value = parseQuotableWord()
        when (operator) {
            EQUALS       -> return HasTag(key, value)
            NOT_EQUALS   -> return NotHasTag(key, value)
            LIKE         -> return HasTagValueLike(key, value)
            NOT_LIKE     -> return NotHasTagValueLike(key, value)
        }
    }

    if (operator in COMPARISON_OPERATORS) {
        val value = parseWord().withOptionalUnitToDoubleOrNull()?.toFloat()
            ?: throw ParseException("Expected a number (e.g. 3.5) or a number with a known unit (e.g. 3.5st)", cursorPos)
        when (operator) {
            GREATER_THAN          -> return HasTagGreaterThan(key, value)
            GREATER_OR_EQUAL_THAN -> return HasTagGreaterOrEqualThan(key, value)
            LESS_THAN             -> return HasTagLessThan(key, value)
            LESS_OR_EQUAL_THAN    -> return HasTagLessOrEqualThan(key, value)
        }
        throw ParseException("Expected a number (e.g. 3.5) or a number with a known unit (e.g. 3.5st)", cursorPos)
    }
    throw ParseException("Unknown operator '$operator'", cursorPos)
}

private fun StringWithCursor.parseKey(): String {
    val reserved = nextIsReservedWord()
    if (reserved != null) {
        throw ParseException("A key cannot be named like the reserved word '$reserved', surround it with quotation marks", cursorPos)
    }

    val length = findKeyLength()
    if (length == 0) {
        throw ParseException("Missing key (dangling prefix operator)", cursorPos)
    }
    return advanceBy(length).stripAndUnescapeQuotes()
}

private fun StringWithCursor.parseOperatorWithSurroundingSpaces(): String? {
    val spaces = expectAnyNumberOfSpaces()
    val result = OPERATORS.firstOrNull { nextIsAndAdvance(it) }
    if (result == null) {
        retreatBy(spaces)
        return null
    }
    expectAnyNumberOfSpaces()
    return result
}

private fun StringWithCursor.parsePlaceholder(): String {
    val length = findNext(PLACEHOLDER_END)
    if (isAtEnd(length)) {
        throw ParseException("Missing closing bracket '}' for placeholder", cursorPos + length)
    }
    val result = advanceBy(length)
    advance() // consume "}"
    return result
}

private fun StringWithCursor.parseQuotableWord(): String {
    val length = findQuotableWordLength()
    if (length == 0) {
        throw ParseException("Missing value (dangling operator)", cursorPos)
    }
    return advanceBy(length).stripAndUnescapeQuotes()
}

private fun StringWithCursor.parseWord(): String {
    val length = findWordLength()
    if (length == 0) {
        throw ParseException("Missing value (dangling operator)", cursorPos)
    }
    return advanceBy(length)
}

private fun StringWithCursor.expectAnyNumberOfSpaces(): Int =
    nextMatchesAndAdvance(WHITESPACES_REGEX)?.value?.length ?: 0

private fun StringWithCursor.nextIsReservedWord(): String? {
    val wordLength = findWordLength()
    return RESERVED_WORDS.firstOrNull { nextIs(it) && wordLength == it.length }
}

private fun StringWithCursor.findKeyLength(): Int {
    var length = findQuotationLength()
    if (length != null) return length

    length = findWordLength()
    for (o in OPERATORS) {
        val opLen = findNext(o)
        if (opLen < length!!) length = opLen
    }
    return length!!
}

private fun StringWithCursor.findWordLength(): Int =
    min(findNext(WHITESPACE_REGEX), findNext(')'))

private fun StringWithCursor.findQuotableWordLength(): Int =
    findQuotationLength() ?: findWordLength()

private fun StringWithCursor.findQuotationLength(): Int? {
    for (quot in QUOTATION_MARKS) {
        if (nextIs(quot)) {
            var length = 0
            while (true) {
                length = findNext(quot, 1 + length)
                if (isAtEnd(length)) {
                    throw ParseException("Did not close quotation marks", cursorPos - 1)
                }
                // ignore escaped
                if (get(cursorPos + length - 1) == '\\') continue
                // +1 because we want to include the closing quotation mark
                return length + 1
            }
        }
    }
    return null
}

private fun String.stripAndUnescapeQuotes(): String {
    val trimmed = if (startsWith('\'') || startsWith('"')) substring(1, length - 1) else this
    val unescaped = trimmed.replace(ESCAPED_QUOTE_REGEX) { it.groupValues[1] }
    return unescaped
}

class ParseException(message: String?, val errorOffset: Int)
    : RuntimeException("At position $errorOffset: $message")
