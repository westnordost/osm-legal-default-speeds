package de.westnordost.osm_legal_default_speeds.tagfilter

internal class TestBooleanExpressionValue(private val value: String) : Matcher<String> {
    override fun matches(obj: String) = obj == value
    override fun toString() = value
}
