package de.westnordost.osm_default_speeds.tagfilter.filters

import de.westnordost.osm_default_speeds.tagfilter.Matcher
import de.westnordost.osm_default_speeds.tagfilter.withOptionalUnitToDoubleOrNull

typealias Tags = Map<String, String>

sealed interface TagFilter : Matcher<Tags> {
    abstract override fun toString(): String
}

class HasKey(val key: String) : TagFilter {
    override fun toString() = key
    override fun matches(obj: Tags) = obj.containsKey(key)
}

class NotHasKey(val key: String) : TagFilter {
    override fun toString() = "!$key"
    override fun matches(obj: Tags) = !obj.containsKey(key)
}

class HasTag(val key: String, val value: String) : TagFilter {
    override fun toString() = "$key = $value"
    override fun matches(obj: Tags) = obj[key] == value
}

class NotHasTag(val key: String, val value: String) : TagFilter {
    override fun toString() = "$key != $value"
    override fun matches(obj: Tags) = obj[key] != value
}

class HasKeyLike(val key: String) : TagFilter {
    private val regex = RegexOrSet.from(key)

    override fun toString() = "~$key"
    override fun matches(obj: Tags) = obj.keys.any { regex.matches(it) }
}

class NotHasKeyLike(val key: String) : TagFilter {
    private val regex = RegexOrSet.from(key)

    override fun toString() = "!~$key"
    override fun matches(obj: Tags) = obj.keys.none { regex.matches(it) }
}

class HasTagValueLike(val key: String, val value: String) : TagFilter {
    private val regex = RegexOrSet.from(value)

    override fun toString() = "$key ~ $value"
    override fun matches(obj: Tags) = obj[key]?.let { regex.matches(it) } ?: false
}

class NotHasTagValueLike(val key: String, val value: String) : TagFilter {
    private val regex = RegexOrSet.from(value)

    override fun toString() = "$key !~ $value"
    override fun matches(obj: Tags) = obj[key]?.let { !regex.matches(it) } ?: true
}

class HasTagLike(val key: String, val value: String) : TagFilter {
    private val keyRegex = RegexOrSet.from(key)
    private val valueRegex = RegexOrSet.from(value)

    override fun toString() = "~$key ~ $value"
    override fun matches(obj: Tags) =
        obj.entries.any { keyRegex.matches(it.key) && valueRegex.matches(it.value) }
}

class HasTagLessThan(key: String, value: Float) : CompareTagValue(key, value) {
    override fun toString() = "$key < $value"
    override fun compareTo(tagValue: Float) = tagValue < value
}
class HasTagGreaterThan(key: String, value: Float) : CompareTagValue(key, value) {
    override fun toString() = "$key > $value"
    override fun compareTo(tagValue: Float) = tagValue > value
}
class HasTagLessOrEqualThan(key: String, value: Float) : CompareTagValue(key, value) {
    override fun toString() = "$key <= $value"
    override fun compareTo(tagValue: Float) = tagValue <= value
}
class HasTagGreaterOrEqualThan(key: String, value: Float) : CompareTagValue(key, value) {
    override fun toString() = "$key >= $value"
    override fun compareTo(tagValue: Float) = tagValue >= value
}

abstract class CompareTagValue(val key: String, val value: Float) : TagFilter {
    abstract fun compareTo(tagValue: Float): Boolean
    override fun matches(obj: Tags): Boolean {
        val tagValue = obj[key]?.withOptionalUnitToDoubleOrNull()?.toFloat() ?: return false
        return compareTo(tagValue)
    }
}
