package de.westnordost.osm_default_speeds.tagfilter

class TestBooleanExpressionValue(private val value: String) : Matcher<String> {
    override fun matches(obj: String) = obj == value
    override fun toString() = value
}
