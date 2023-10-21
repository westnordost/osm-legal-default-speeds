package de.westnordost.osm_legal_default_speeds.tagfilter

import kotlin.test.*

internal class TagFilterExpressionParserTest {

    @Test fun fail_if_no_space_after_or_before_and_or() {
        shouldFail("shop andfail")
        shouldFail("'shop'and fail")
    }

    @Test fun fail_on_unknown_like_operator() {
        shouldFail("~speed > 3")
    }

    @Test fun fail_on_no_number_for_comparison() {
        shouldFail("speed > walk")
    }

    @Test fun fail_if_tag_key_is_like_reserved_word() {
        shouldFail("or = yes")
        shouldFail("and = yes")
    }

    @Test fun tag_key_like_reserved_word_in_quotation_marks_is_ok() {
        val tags = mapOf("with" to "with")
        matches(tags, "'with'")
        matches(tags, "'with'='with'")
    }

    @Test fun quotes_are_optional() {
        val tags = mapOf("shop" to "yes")
        matches(tags, "shop")
        matches(tags, "'shop'")
        matches(tags, "\"shop\"")
    }

    @Test fun quoting_empty_string() {
        matches(mapOf("shop" to ""), "shop = ''")
    }

    @Test fun escaping_quotes() {
        matches(mapOf("shop\"" to "yes"), "\"shop\\\"\"")
        matches(mapOf("shop'" to "yes"), "'shop\\\''")
        matches(mapOf("shop" to "yes\""), "shop = \"yes\\\"\"")
        matches(mapOf("shop" to "yes'"), "shop = 'yes\\\''")
        matches(mapOf("sh'op" to "yes'"), "sh\\'op = yes\\'")
    }

    @Test fun unquoted_tag_may_start_with_reserved_word() {
        matches(mapOf("withdrawn" to "with"), "withdrawn = with")
        matches(mapOf("orchard" to "or"), "orchard = or")
        matches(mapOf("android" to "and"), "android = and")
    }

    @Test fun tag_key_with_quotation_marks_is_ok() {
        matches(
            mapOf("highway = residential or bla" to "yes"),
            "\"highway = residential or bla\""
        )
    }

    @Test fun tag_value_with_quotation_marks_is_ok() {
        matches(
            mapOf("highway" to "residential or bla"),
            "highway = \"residential or bla\""
        )
    }

    @Test fun fail_if_tag_key_quotation_marks_not_closed() {
        shouldFail("nodes with \"highway = residential or bla")
    }

    @Test fun fail_if_tag_value_quotation_marks_not_closed() {
        shouldFail("nodes with highway = \"residential or bla")
    }

    @Test fun whitespaces_around_tag_key_do_not_matter() {
        val tags = mapOf("shop" to "yes")

        matches(tags, "shop")
        matches(tags, " \t\n\t\n shop \t\n\t\n ")
        matches(tags, " \t\n\t\n ( \t\n\t\n shop \t\n\t\n ) \t\n\t\n ")
    }

    @Test fun whitespaces_around_tag_value_do_not_matter() {
        val tags = mapOf("shop" to "yes")

        matches(tags, "shop=yes")
        matches(tags, "shop \t\n\t\n = \t\n\t\n yes \t\n\t\n ")
        matches(tags, " \t\n\t\n ( \t\n\t\n shop \t\n\t\n = \t\n\t\n yes \t\n\t\n ) \t\n\t\n ")
    }

    @Test fun whitespaces_in_tag_do_matter() {
        val tags = mapOf(" \t\n\t\n shop \t\n\t\n " to " \t\n\t\n yes \t\n\t\n ")
        matches(tags, "\" \t\n\t\n shop \t\n\t\n \" = \" \t\n\t\n yes \t\n\t\n \"")
    }

    @Test fun fail_on_dangling_operator() {
        shouldFail("nodes with highway=")
    }

    @Test fun fail_on_dangling_boolean_operator() {
        shouldFail("nodes with highway and")
        shouldFail("nodes with highway or ")
    }

    @Test fun fail_on_dangling_quote() {
        shouldFail("shop = yes '")
        shouldFail("shop = yes \"")
    }

    @Test fun fail_on_dangling_prefix_operator() {
        shouldFail("shop = yes and !")
        shouldFail("shop = yes and ~")
    }

    @Test fun fail_if_bracket_not_closed() {
        shouldFail("nodes with (highway")
        shouldFail("nodes with (highway = service and (service = alley)")
    }

    @Test fun fail_if_too_many_brackets_closed() {
        shouldFail("nodes with highway)")
        shouldFail("nodes with (highway = service))")
    }

    @Test fun whitespaces_do_not_matter_for_brackets() {
        val tags = mapOf("shop" to "yes", "fee" to "yes")
        matches(tags,"shop and((fee=yes))")
        matches(tags,"shop and \t\n\t\n ( \t\n\t\n ( \n\t\n\t fee=yes \n\t\n\t ))")
    }

    @Test fun fail_on_unknown_thing_after_tag() {
        shouldFail("nodes with highway what is this")
    }

    @Test fun fail_if_neither_a_number_nor_a_date_is_used_for_comparison() {
        shouldFail("nodes with width > x")
        shouldFail("nodes with width >=x ")
        shouldFail("nodes with width < x")
        shouldFail("nodes with width <=x")
    }

    @Test fun quotes_for_comparisons_are_not_allowed() {
        shouldFail("nodes with width >  '3'")
        shouldFail("nodes with width >= '3'")
        shouldFail("nodes with width < '3'")
        shouldFail("nodes with width <= '3'")
    }

    @Test fun tag_negation_not_combinable_with_operator() {
        shouldFail("nodes with !highway=residential")
        shouldFail("nodes with !highway!=residential")
        shouldFail("nodes with !highway~residential")
        shouldFail("nodes with !highway!~residential")
    }

    @Test fun empty_key_and_value() {
        matches(mapOf("" to ""), "'' = ''")
    }

    @Test fun not_key_operator_is_parsed_correctly() {
        matches(mapOf(), "!shop")
        matches(mapOf(), "!  shop")
        notMatches(mapOf("shop" to "yes"), "!shop")
    }

    @Test fun not_key_like_operator_is_parsed_correctly() {
        matches(mapOf(), "!~...")
        matches(mapOf(), "!~  ...")
        notMatches(mapOf("abc" to "yes"), "!~...")
    }

    @Test fun key_like_operator_is_parsed_correctly() {
        matches(mapOf("abc" to "yes"), "~...")
        matches(mapOf("abc" to "yes"), "~   ...")
        notMatches(mapOf("ab" to "yes"), "~   ...")
    }

    @Test fun tag_like_operator_is_parsed_correctly() {
        matches(mapOf("abc" to "yes"), "~...~...")
        matches(mapOf("abc" to "yes"), "~  ...  ~  ...")
        notMatches(mapOf("abc" to "ye"), "~  ...  ~  ...")
        notMatches(mapOf("ab" to "yes"), "~  ...  ~  ...")
    }

    @Test fun key_operator_is_parsed_correctly() {
        matches(mapOf("shop" to "yes"), "shop")
        notMatches(mapOf("snop" to "yes"), "shop")
    }

    @Test fun has_tag_operator_is_parsed_correctly() {
        matches(mapOf("lit" to "yes"), "lit = yes")
        matches(mapOf("lit" to "yes"), "lit=yes")
        matches(mapOf("lit" to "yes"), "lit   =   yes")
        notMatches(mapOf("lit" to "yesnt"), "lit = yes")
    }

    @Test fun not_has_tag_operator_is_parsed_correctly() {
        matches(mapOf("lit" to "no"), "lit != yes")
        matches(mapOf("lit" to "no"), "lit!=yes")
        matches(mapOf("lit" to "no"), "lit   !=   yes")
        notMatches(mapOf("lit" to "yes"), "lit   !=   yes")
    }

    @Test fun has_tag_value_like_operator_is_parsed_correctly() {
        matches(mapOf("lit" to "yes"), "lit ~ ...")
        matches(mapOf("lit" to "yes"), "lit~...")
        matches(mapOf("lit" to "yes"), "lit   ~   ...")
        notMatches(mapOf("lit" to "ye"), "lit   ~   ...")
    }

    @Test fun not_has_tag_value_like_operator_is_parsed_correctly() {
        matches(mapOf("lit" to "ye"), "lit !~ ...")
        matches(mapOf("lit" to "ye"), "lit!~...")
        matches(mapOf("lit" to "ye"), "lit   !~   ...")
        notMatches(mapOf("lit" to "yes"), "lit   !~   ...")
    }

    @Test fun tag_value_greater_than_operator_is_parsed_correctly() {
        matches(mapOf("width" to "5"), "width > 3")
        matches(mapOf("width" to "5"), "width>3.0")
        matches(mapOf("width" to "5"), "width   >   3")
        notMatches(mapOf("width" to "3"), "width   >   3")
        matches(mapOf("width" to "0.4"), "width>0.3")
        matches(mapOf("width" to ".4"), "width>.3")
        notMatches(mapOf("width" to ".3"), "width>.3")
    }

    @Test fun tag_value_greater_or_equal_than_operator_is_parsed_correctly() {
        matches(mapOf("width" to "3"), "width >= 3")
        matches(mapOf("width" to "3"), "width>=3.0")
        matches(mapOf("width" to "3"), "width   >=   3")
        notMatches(mapOf("width" to "2"), "width   >=   3")
        matches(mapOf("width" to "0.3"), "width>=0.3")
        matches(mapOf("width" to ".3"), "width>=.3")
        notMatches(mapOf("width" to ".2"), "width>=.3")
    }

    @Test fun tag_value_less_than_operator_is_parsed_correctly() {
        matches(mapOf("width" to "2"), "width < 3")
        matches(mapOf("width" to "2"), "width<3.0")
        matches(mapOf("width" to "2"), "width   <   3")
        notMatches(mapOf("width" to "3"), "width   <   3")
        matches(mapOf("width" to "0.2"), "width<0.3")
        matches(mapOf("width" to ".2"), "width<.3")
        notMatches(mapOf("width" to ".3"), "width<.3")
    }

    @Test fun tag_value_less_or_equal_than_operator_is_parsed_correctly() {
        matches(mapOf("width" to "3"), "width <= 3")
        matches(mapOf("width" to "3"), "width<=3.0")
        matches(mapOf("width" to "3"), "width   <=   3")
        notMatches(mapOf("width" to "4"), "width   <=   3")
        matches(mapOf("width" to "0.3"), "width<=0.3")
        matches(mapOf("width" to ".3"), "width<=.3")
        notMatches(mapOf("width" to ".4"), "width<=.3")
    }

    @Test fun comparisons_work_with_units() {
        matches(mapOf("maxspeed" to "30.1 mph"), "maxspeed > 30mph")
        matches(mapOf("maxspeed" to "48.3"), "maxspeed > 30mph")
        matches(mapOf("maxspeed" to "48.3 km/h"), "maxspeed > 30mph")

        notMatches(mapOf("maxspeed" to "30.0 mph"), "maxspeed > 30mph")
        notMatches(mapOf("maxspeed" to "48.2"), "maxspeed > 30mph")
        notMatches(mapOf("maxspeed" to "48.2 km/h"), "maxspeed > 30mph")
    }

    @Test fun comparisons_work_with_extra_special_units() {
        matches(mapOf("maxwidth" to "4 ft 7 in"), "maxwidth > 4'6\"")
        matches(mapOf("maxwidth" to "4'7\""), "maxwidth > 4'6\"")
        matches(mapOf("maxwidth" to "1.4 m"), "maxwidth > 4'6\"")
        matches(mapOf("maxwidth" to "1.4m"), "maxwidth > 4'6\"")
        matches(mapOf("maxwidth" to "1.4"), "maxwidth > 4'6\"")

        notMatches(mapOf("maxwidth" to "4'6\""), "maxwidth > 4'6\"")
        notMatches(mapOf("maxwidth" to "1.3"), "maxwidth > 4'6\"")
    }

    @Test fun and() {
        val expr = "a and b"
        matches(mapOfKeys("a", "b"), expr)
        notMatches(mapOfKeys("a"), expr)
        notMatches(mapOfKeys("b"), expr)
    }

    @Test fun two_and() {
        val expr = "a and b and c"
        matches(mapOfKeys("a", "b", "c"), expr)
        notMatches(mapOfKeys("a", "b"), expr)
        notMatches(mapOfKeys("a", "c"), expr)
        notMatches(mapOfKeys("b", "c"), expr)
    }

    @Test fun or() {
        val expr = "a or b"
        matches(mapOfKeys("b"), expr)
        matches(mapOfKeys("a"), expr)
        notMatches(mapOfKeys(), expr)
    }

    @Test fun two_or() {
        val expr = "a or b or c"
        matches(mapOfKeys("c"), expr)
        matches(mapOfKeys("b"), expr)
        matches(mapOfKeys("a"), expr)
        notMatches(mapOfKeys(), expr)
    }

    @Test fun or_as_first_child_in_and() {
        val expr = "(a or b) and c"
        matches(mapOfKeys("c", "a"), expr)
        matches(mapOfKeys("c", "b"), expr)
        notMatches(mapOfKeys("b"), expr)
        notMatches(mapOfKeys("a"), expr)
    }

    @Test fun or_as_last_child_in_and() {
        val expr = "c and (a or b)"
        matches(mapOfKeys("c", "a"), expr)
        matches(mapOfKeys("c", "b"), expr)
        notMatches(mapOfKeys("b"), expr)
        notMatches(mapOfKeys("a"), expr)
    }

    @Test fun or_in_the_middle_of_and() {
        val expr = "c and (a or b) and d"
        matches(mapOfKeys("c", "d", "a"), expr)
        matches(mapOfKeys("c", "d", "b"), expr)
        notMatches(mapOfKeys("b"), expr)
        notMatches(mapOfKeys("a"), expr)
    }

    @Test fun and_as_first_child_in_or() {
        val expr = "a and b or c"
        matches(mapOfKeys("a", "b"), expr)
        matches(mapOfKeys("c"), expr)
        notMatches(mapOfKeys("a"), expr)
        notMatches(mapOfKeys("b"), expr)
    }

    @Test fun and_as_last_child_in_or() {
        val expr = "c or a and b"
        matches(mapOfKeys("a", "b"), expr)
        matches(mapOfKeys("c"), expr)
        notMatches(mapOfKeys("a"), expr)
        notMatches(mapOfKeys("b"), expr)
    }

    @Test fun and_in_the_middle_of_or() {
        val expr = "c or a and b or d"
        matches(mapOfKeys("a", "b"), expr)
        matches(mapOfKeys("c"), expr)
        matches(mapOfKeys("d"), expr)
        notMatches(mapOfKeys("a"), expr)
        notMatches(mapOfKeys("b"), expr)
    }

    @Test fun and_in_or_in_and() {
        val expr = "a and (b and c or d)"
        matches(mapOfKeys("a", "d"), expr)
        matches(mapOfKeys("a", "b", "c"), expr)
        notMatches(mapOfKeys("a"), expr)
        notMatches(mapOfKeys("b", "c"), expr)
        notMatches(mapOfKeys("d"), expr)
    }

    @Test fun and_in_or_in_and_in_or() {
        val expr = "a or (b and (c or (d and e)))"
        matches(mapOfKeys("a"), expr)
        matches(mapOfKeys("b", "c"), expr)
        matches(mapOfKeys("b", "d", "e"), expr)
        notMatches(mapOfKeys(), expr)
        notMatches(mapOfKeys("b"), expr)
        notMatches(mapOfKeys("c"), expr)
        notMatches(mapOfKeys("b", "d"), expr)
        notMatches(mapOfKeys("b", "e"), expr)
    }

    @Test fun and_in_bracket_followed_by_another_and() {
        val expr = "(a or (b and c)) and d"
        matches(mapOfKeys("a", "d"), expr)
        matches(mapOfKeys("b", "c", "d"), expr)
        notMatches(mapOfKeys("a"), expr)
        notMatches(mapOfKeys("d"), expr)
        notMatches(mapOfKeys("b", "c"), expr)
    }

    @Test fun not_with_leaf() {
        val expr = "!(a)"
        matches(mapOfKeys("b"), expr)
        notMatches(mapOfKeys("a"), expr)
        notMatches(mapOfKeys("a", "b"), expr)
    }

    @Test fun not_without_braces() {
        val expr = "ways with !highway = residential or access = yes"
        shouldFail(expr)
    }

    @Test fun not_and_with_space() {
        val expr = "! (a and b)"
        matches(mapOfKeys("a"), expr)
        matches(mapOfKeys("b"), expr)
        matches(mapOfKeys("b", "c"), expr)
        matches(mapOfKeys("c"), expr)
        notMatches(mapOfKeys("a", "b", "c"), expr)
    }

    @Test fun not_and() {
        val expr = "!(a and b)"
        matches(mapOfKeys("a"), expr)
        matches(mapOfKeys("b"), expr)
        matches(mapOfKeys("b", "c"), expr)
        matches(mapOfKeys("c"), expr)
        notMatches(mapOfKeys("a", "b", "c"), expr)
    }

    @Test fun not_or() {
        val expr = "!(a or b)"
        matches(mapOfKeys("c"), expr)
        matches(mapOfKeys("c", "d", "e"), expr)
        notMatches(mapOfKeys("a"), expr)
        notMatches(mapOfKeys("b"), expr)
        notMatches(mapOfKeys("b", "c"), expr)
        notMatches(mapOfKeys("a", "c"), expr)
        notMatches(mapOfKeys("a", "b", "c"), expr)
    }

    @Test fun nested_not() {
        val expr = "!(!(a))" // equals the expression "a"
        matches(mapOfKeys("a"), expr)
        matches(mapOfKeys("a", "b"), expr)
        notMatches(mapOfKeys("b"), expr)
    }

    @Test fun nested_not_with_or() {
        val expr = "!(!(a and b) or c)" // equals a and b and !(c)
        matches(mapOfKeys("a", "b"), expr)
        matches(mapOfKeys("a", "b", "d"), expr)
        notMatches(mapOfKeys("a"), expr)
        notMatches(mapOfKeys("c"), expr)
        notMatches(mapOfKeys("b", "c"), expr)
        notMatches(mapOfKeys("a", "b", "c"), expr)
        notMatches(mapOfKeys("a", "b", "c", "d"), expr)
    }

    @Test fun nested_not_with_or_and_switched_operands() {
        val expr = "!(c or !(a and b))" // equals a and b and !(c)
        matches(mapOfKeys("a", "b"), expr)
        matches(mapOfKeys("a", "b", "d"), expr)
        notMatches(mapOfKeys("a"), expr)
        notMatches(mapOfKeys("c"), expr)
        notMatches(mapOfKeys("b", "c"), expr)
        notMatches(mapOfKeys("a", "b", "c"), expr)
        notMatches(mapOfKeys("a", "b", "c", "d"), expr)
    }

    @Test fun brackets_are_not_dissolved_illegally() {
        val expr = "a or (b or c) and !d"
        matches(mapOfKeys("a"), expr)
        matches(mapOfKeys("a", "d"), expr)
        matches(mapOfKeys("b"), expr)
        matches(mapOfKeys("c"), expr)
        notMatches(mapOfKeys("c", "d"), expr)
        notMatches(mapOfKeys("b", "d"), expr)
        matches(mapOfKeys("a", "c", "d"), expr)
    }

    @Test fun fail_on_placeholder_not_closed() {
        shouldFail("{my placeholder")
    }

    @Test fun placeholders() {
        matches(mapOfKeys(), "{placeholder}") { it == "placeholder" }
        notMatches(mapOfKeys(), "{placeholder}") { it == "placeholder2" }

        matches(mapOfKeys(), "{stuff $§%&\"'()or}") { it == "stuff $§%&\"'()or" }

        matches(mapOfKeys("a"), "{placeholder} and a") { it == "placeholder" }
        notMatches(mapOfKeys(), "{placeholder} and a") { it == "placeholder" }
    }

    @Test fun list_placeholders() {
        assertEquals(listOf(), TagFilterExpression("a").getPlaceholders().toList())
        assertEquals(listOf("a"), TagFilterExpression("{a}").getPlaceholders().toList())
        assertEquals(listOf("a", "b"), TagFilterExpression("{a} and {b}").getPlaceholders().toList())
    }
}

private fun mapOfKeys(vararg key: String) =
    key.mapIndexed { i, s -> s to i.toString() }.toMap()

private fun shouldFail(input: String) {
    assertFailsWith(ParseException::class) { TagFilterExpression(input) }
}

private fun matches(tags: Map<String,String>, input: String, evaluate: (String) -> Boolean = { false }) =
    assertTrue(TagFilterExpression(input).matches(tags, evaluate))

private fun notMatches(tags: Map<String,String>, input: String, evaluate: (String) -> Boolean = { false }) =
    assertFalse(TagFilterExpression(input).matches(tags, evaluate))