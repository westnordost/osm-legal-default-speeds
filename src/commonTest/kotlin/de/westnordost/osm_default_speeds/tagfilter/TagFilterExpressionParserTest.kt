package de.westnordost.osm_default_speeds.tagfilter

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
        matchesTags(tags, "'with'")
        matchesTags(tags, "'with'='with'")
    }

    @Test fun quotes_are_optional() {
        val tags = mapOf("shop" to "yes")
        matchesTags(tags, "shop")
        matchesTags(tags, "'shop'")
        matchesTags(tags, "\"shop\"")
    }

    @Test fun quoting_empty_string() {
        matchesTags(mapOf("shop" to ""), "shop = ''")
    }

    @Test fun escaping_quotes() {
        matchesTags(mapOf("shop\"" to "yes"), "\"shop\\\"\"")
        matchesTags(mapOf("shop'" to "yes"), "'shop\\\''")
        matchesTags(mapOf("shop" to "yes\""), "shop = \"yes\\\"\"")
        matchesTags(mapOf("shop" to "yes'"), "shop = 'yes\\\''")
        matchesTags(mapOf("sh'op" to "yes'"), "sh\\'op = yes\\'")
    }

    @Test fun unquoted_tag_may_start_with_reserved_word() {
        matchesTags(mapOf("withdrawn" to "with"), "withdrawn = with")
        matchesTags(mapOf("orchard" to "or"), "orchard = or")
        matchesTags(mapOf("android" to "and"), "android = and")
    }

    @Test fun tag_key_with_quotation_marks_is_ok() {
        matchesTags(
            mapOf("highway = residential or bla" to "yes"),
            "\"highway = residential or bla\""
        )
    }

    @Test fun tag_value_with_quotation_marks_is_ok() {
        matchesTags(
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

        matchesTags(tags, "shop")
        matchesTags(tags, " \t\n\t\n shop \t\n\t\n ")
        matchesTags(tags, " \t\n\t\n ( \t\n\t\n shop \t\n\t\n ) \t\n\t\n ")
    }

    @Test fun whitespaces_around_tag_value_do_not_matter() {
        val tags = mapOf("shop" to "yes")

        matchesTags(tags, "shop=yes")
        matchesTags(tags, "shop \t\n\t\n = \t\n\t\n yes \t\n\t\n ")
        matchesTags(tags, " \t\n\t\n ( \t\n\t\n shop \t\n\t\n = \t\n\t\n yes \t\n\t\n ) \t\n\t\n ")
    }

    @Test fun whitespaces_in_tag_do_matter() {
        val tags = mapOf(" \t\n\t\n shop \t\n\t\n " to " \t\n\t\n yes \t\n\t\n ")
        matchesTags(tags, "\" \t\n\t\n shop \t\n\t\n \" = \" \t\n\t\n yes \t\n\t\n \"")
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
        matchesTags(tags,"shop and((fee=yes))")
        matchesTags(tags,"shop and \t\n\t\n ( \t\n\t\n ( \n\t\n\t fee=yes \n\t\n\t ))")
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
        matchesTags(mapOf("" to ""), "'' = ''")
    }

    @Test fun not_key_operator_is_parsed_correctly() {
        matchesTags(mapOf(), "!shop")
        matchesTags(mapOf(), "!  shop")
        notMatchesTags(mapOf("shop" to "yes"), "!shop")
    }

    @Test fun not_key_like_operator_is_parsed_correctly() {
        matchesTags(mapOf(), "!~...")
        matchesTags(mapOf(), "!~  ...")
        notMatchesTags(mapOf("abc" to "yes"), "!~...")
    }

    @Test fun key_like_operator_is_parsed_correctly() {
        matchesTags(mapOf("abc" to "yes"), "~...")
        matchesTags(mapOf("abc" to "yes"), "~   ...")
        notMatchesTags(mapOf("ab" to "yes"), "~   ...")
    }

    @Test fun tag_like_operator_is_parsed_correctly() {
        matchesTags(mapOf("abc" to "yes"), "~...~...")
        matchesTags(mapOf("abc" to "yes"), "~  ...  ~  ...")
        notMatchesTags(mapOf("abc" to "ye"), "~  ...  ~  ...")
        notMatchesTags(mapOf("ab" to "yes"), "~  ...  ~  ...")
    }

    @Test fun key_operator_is_parsed_correctly() {
        matchesTags(mapOf("shop" to "yes"), "shop")
        notMatchesTags(mapOf("snop" to "yes"), "shop")
    }

    @Test fun has_tag_operator_is_parsed_correctly() {
        matchesTags(mapOf("lit" to "yes"), "lit = yes")
        matchesTags(mapOf("lit" to "yes"), "lit=yes")
        matchesTags(mapOf("lit" to "yes"), "lit   =   yes")
        notMatchesTags(mapOf("lit" to "yesnt"), "lit = yes")
    }

    @Test fun not_has_tag_operator_is_parsed_correctly() {
        matchesTags(mapOf("lit" to "no"), "lit != yes")
        matchesTags(mapOf("lit" to "no"), "lit!=yes")
        matchesTags(mapOf("lit" to "no"), "lit   !=   yes")
        notMatchesTags(mapOf("lit" to "yes"), "lit   !=   yes")
    }

    @Test fun has_tag_value_like_operator_is_parsed_correctly() {
        matchesTags(mapOf("lit" to "yes"), "lit ~ ...")
        matchesTags(mapOf("lit" to "yes"), "lit~...")
        matchesTags(mapOf("lit" to "yes"), "lit   ~   ...")
        notMatchesTags(mapOf("lit" to "ye"), "lit   ~   ...")
    }

    @Test fun not_has_tag_value_like_operator_is_parsed_correctly() {
        matchesTags(mapOf("lit" to "ye"), "lit !~ ...")
        matchesTags(mapOf("lit" to "ye"), "lit!~...")
        matchesTags(mapOf("lit" to "ye"), "lit   !~   ...")
        notMatchesTags(mapOf("lit" to "yes"), "lit   !~   ...")
    }

    @Test fun tag_value_greater_than_operator_is_parsed_correctly() {
        matchesTags(mapOf("width" to "5"), "width > 3")
        matchesTags(mapOf("width" to "5"), "width>3.0")
        matchesTags(mapOf("width" to "5"), "width   >   3")
        notMatchesTags(mapOf("width" to "3"), "width   >   3")
        matchesTags(mapOf("width" to "0.4"), "width>0.3")
        matchesTags(mapOf("width" to ".4"), "width>.3")
        notMatchesTags(mapOf("width" to ".3"), "width>.3")
    }

    @Test fun tag_value_greater_or_equal_than_operator_is_parsed_correctly() {
        matchesTags(mapOf("width" to "3"), "width >= 3")
        matchesTags(mapOf("width" to "3"), "width>=3.0")
        matchesTags(mapOf("width" to "3"), "width   >=   3")
        notMatchesTags(mapOf("width" to "2"), "width   >=   3")
        matchesTags(mapOf("width" to "0.3"), "width>=0.3")
        matchesTags(mapOf("width" to ".3"), "width>=.3")
        notMatchesTags(mapOf("width" to ".2"), "width>=.3")
    }

    @Test fun tag_value_less_than_operator_is_parsed_correctly() {
        matchesTags(mapOf("width" to "2"), "width < 3")
        matchesTags(mapOf("width" to "2"), "width<3.0")
        matchesTags(mapOf("width" to "2"), "width   <   3")
        notMatchesTags(mapOf("width" to "3"), "width   <   3")
        matchesTags(mapOf("width" to "0.2"), "width<0.3")
        matchesTags(mapOf("width" to ".2"), "width<.3")
        notMatchesTags(mapOf("width" to ".3"), "width<.3")
    }

    @Test fun tag_value_less_or_equal_than_operator_is_parsed_correctly() {
        matchesTags(mapOf("width" to "3"), "width <= 3")
        matchesTags(mapOf("width" to "3"), "width<=3.0")
        matchesTags(mapOf("width" to "3"), "width   <=   3")
        notMatchesTags(mapOf("width" to "4"), "width   <=   3")
        matchesTags(mapOf("width" to "0.3"), "width<=0.3")
        matchesTags(mapOf("width" to ".3"), "width<=.3")
        notMatchesTags(mapOf("width" to ".4"), "width<=.3")
    }

    @Test fun comparisons_work_with_units() {
        matchesTags(mapOf("maxspeed" to "30.1 mph"), "maxspeed > 30mph")
        matchesTags(mapOf("maxspeed" to "48.3"), "maxspeed > 30mph")
        matchesTags(mapOf("maxspeed" to "48.3 km/h"), "maxspeed > 30mph")

        notMatchesTags(mapOf("maxspeed" to "30.0 mph"), "maxspeed > 30mph")
        notMatchesTags(mapOf("maxspeed" to "48.2"), "maxspeed > 30mph")
        notMatchesTags(mapOf("maxspeed" to "48.2 km/h"), "maxspeed > 30mph")
    }

    @Test fun comparisons_work_with_extra_special_units() {
        matchesTags(mapOf("maxwidth" to "4 ft 7 in"), "maxwidth > 4'6\"")
        matchesTags(mapOf("maxwidth" to "4'7\""), "maxwidth > 4'6\"")
        matchesTags(mapOf("maxwidth" to "1.4 m"), "maxwidth > 4'6\"")
        matchesTags(mapOf("maxwidth" to "1.4m"), "maxwidth > 4'6\"")
        matchesTags(mapOf("maxwidth" to "1.4"), "maxwidth > 4'6\"")

        notMatchesTags(mapOf("maxwidth" to "4'6\""), "maxwidth > 4'6\"")
        notMatchesTags(mapOf("maxwidth" to "1.3"), "maxwidth > 4'6\"")
    }

    @Test fun and() {
        val expr = "a and b"
        matchesTags(mapOfKeys("a", "b"), expr)
        notMatchesTags(mapOfKeys("a"), expr)
        notMatchesTags(mapOfKeys("b"), expr)
    }

    @Test fun two_and() {
        val expr = "a and b and c"
        matchesTags(mapOfKeys("a", "b", "c"), expr)
        notMatchesTags(mapOfKeys("a", "b"), expr)
        notMatchesTags(mapOfKeys("a", "c"), expr)
        notMatchesTags(mapOfKeys("b", "c"), expr)
    }

    @Test fun or() {
        val expr = "a or b"
        matchesTags(mapOfKeys("b"), expr)
        matchesTags(mapOfKeys("a"), expr)
        notMatchesTags(mapOfKeys(), expr)
    }

    @Test fun two_or() {
        val expr = "a or b or c"
        matchesTags(mapOfKeys("c"), expr)
        matchesTags(mapOfKeys("b"), expr)
        matchesTags(mapOfKeys("a"), expr)
        notMatchesTags(mapOfKeys(), expr)
    }

    @Test fun or_as_first_child_in_and() {
        val expr = "(a or b) and c"
        matchesTags(mapOfKeys("c", "a"), expr)
        matchesTags(mapOfKeys("c", "b"), expr)
        notMatchesTags(mapOfKeys("b"), expr)
        notMatchesTags(mapOfKeys("a"), expr)
    }

    @Test fun or_as_last_child_in_and() {
        val expr = "c and (a or b)"
        matchesTags(mapOfKeys("c", "a"), expr)
        matchesTags(mapOfKeys("c", "b"), expr)
        notMatchesTags(mapOfKeys("b"), expr)
        notMatchesTags(mapOfKeys("a"), expr)
    }

    @Test fun or_in_the_middle_of_and() {
        val expr = "c and (a or b) and d"
        matchesTags(mapOfKeys("c", "d", "a"), expr)
        matchesTags(mapOfKeys("c", "d", "b"), expr)
        notMatchesTags(mapOfKeys("b"), expr)
        notMatchesTags(mapOfKeys("a"), expr)
    }

    private fun mapOfKeys(vararg key: String) =
        key.mapIndexed { i, s -> s to i.toString() }.toMap()

    @Test fun and_as_first_child_in_or() {
        val expr = "a and b or c"
        matchesTags(mapOfKeys("a", "b"), expr)
        matchesTags(mapOfKeys("c"), expr)
        notMatchesTags(mapOfKeys("a"), expr)
        notMatchesTags(mapOfKeys("b"), expr)
    }

    @Test fun and_as_last_child_in_or() {
        val expr = "c or a and b"
        matchesTags(mapOfKeys("a", "b"), expr)
        matchesTags(mapOfKeys("c"), expr)
        notMatchesTags(mapOfKeys("a"), expr)
        notMatchesTags(mapOfKeys("b"), expr)
    }

    @Test fun and_in_the_middle_of_or() {
        val expr = "c or a and b or d"
        matchesTags(mapOfKeys("a", "b"), expr)
        matchesTags(mapOfKeys("c"), expr)
        matchesTags(mapOfKeys("d"), expr)
        notMatchesTags(mapOfKeys("a"), expr)
        notMatchesTags(mapOfKeys("b"), expr)
    }

    @Test fun and_in_or_in_and() {
        val expr = "a and (b and c or d)"
        matchesTags(mapOfKeys("a", "d"), expr)
        matchesTags(mapOfKeys("a", "b", "c"), expr)
        notMatchesTags(mapOfKeys("a"), expr)
        notMatchesTags(mapOfKeys("b", "c"), expr)
        notMatchesTags(mapOfKeys("d"), expr)
    }

    @Test fun and_in_or_in_and_in_or() {
        val expr = "a or (b and (c or (d and e)))"
        matchesTags(mapOfKeys("a"), expr)
        matchesTags(mapOfKeys("b", "c"), expr)
        matchesTags(mapOfKeys("b", "d", "e"), expr)
        notMatchesTags(mapOfKeys(), expr)
        notMatchesTags(mapOfKeys("b"), expr)
        notMatchesTags(mapOfKeys("c"), expr)
        notMatchesTags(mapOfKeys("b", "d"), expr)
        notMatchesTags(mapOfKeys("b", "e"), expr)
    }

    @Test fun and_in_bracket_followed_by_another_and() {
        val expr = "(a or (b and c)) and d"
        matchesTags(mapOfKeys("a", "d"), expr)
        matchesTags(mapOfKeys("b", "c", "d"), expr)
        notMatchesTags(mapOfKeys("a"), expr)
        notMatchesTags(mapOfKeys("d"), expr)
        notMatchesTags(mapOfKeys("b", "c"), expr)
    }


    private fun parse(input: String) = input.toTagFilterExpression()

    private fun shouldFail(input: String) {
        assertFailsWith(ParseException::class) { parse(input) }
    }

    private fun matchesTags(tags: Map<String,String>, input: String) =
        assertTrue(parse(input).matches(tags))

    private fun notMatchesTags(tags: Map<String,String>, input: String) =
        assertFalse(parse(input).matches(tags))
}
