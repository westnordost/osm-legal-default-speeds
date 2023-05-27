package de.westnordost.osm_legal_default_speeds.tagfilter.filters

import de.westnordost.osm_legal_default_speeds.tagfilter.Matcher
import de.westnordost.osm_legal_default_speeds.tagfilter.withOptionalUnitToDoubleOrNull

internal sealed interface TagFilter : Matcher<Map<String, String>> {
    abstract override fun toString(): String

    val relevantKey: RelevantKey
}

internal class HasKey(val key: String) : TagFilter {
    override fun toString() = key
    override fun matches(obj: Map<String, String>) = obj.containsKey(key)
    override val relevantKey get() = RelevantKeyString(key)
}

internal class NotHasKey(val key: String) : TagFilter {
    override fun toString() = "!$key"
    override fun matches(obj: Map<String, String>) = !obj.containsKey(key)
    override val relevantKey get() = RelevantKeyString(key)
}

internal class HasTag(val key: String, val value: String) : TagFilter {
    override fun toString() = "$key = $value"
    override fun matches(obj: Map<String, String>) = obj[key] == value
    override val relevantKey get() = RelevantKeyString(key)
}

internal class NotHasTag(val key: String, val value: String) : TagFilter {
    override fun toString() = "$key != $value"
    override fun matches(obj: Map<String, String>) = obj[key] != value
    override val relevantKey get() = RelevantKeyString(key)
}

internal class HasKeyLike(val key: String) : TagFilter {
    private val regex = RegexOrSet.from(key)

    override fun toString() = "~$key"
    override fun matches(obj: Map<String, String>) = obj.keys.any { regex.matches(it) }
    override val relevantKey get() = RelevantKeyRegex(regex)
}

internal class NotHasKeyLike(val key: String) : TagFilter {
    private val regex = RegexOrSet.from(key)

    override fun toString() = "!~$key"
    override fun matches(obj: Map<String, String>) = obj.keys.none { regex.matches(it) }
    override val relevantKey get() = RelevantKeyRegex(regex)
}

internal class HasTagValueLike(val key: String, val value: String) : TagFilter {
    private val regex = RegexOrSet.from(value)

    override fun toString() = "$key ~ $value"
    override fun matches(obj: Map<String, String>) = obj[key]?.let { regex.matches(it) } ?: false
    override val relevantKey get() = RelevantKeyString(key)
}

internal class NotHasTagValueLike(val key: String, val value: String) : TagFilter {
    private val regex = RegexOrSet.from(value)

    override fun toString() = "$key !~ $value"
    override fun matches(obj: Map<String, String>) = obj[key]?.let { !regex.matches(it) } ?: true
    override val relevantKey get() = RelevantKeyString(key)
}

internal class HasTagLike(val key: String, val value: String) : TagFilter {
    private val keyRegex = RegexOrSet.from(key)
    private val valueRegex = RegexOrSet.from(value)

    override fun toString() = "~$key ~ $value"
    override fun matches(obj: Map<String, String>) =
        obj.entries.any { keyRegex.matches(it.key) && valueRegex.matches(it.value) }
    override val relevantKey get() = RelevantKeyRegex(keyRegex)
}

internal class HasTagLessThan(key: String, value: Float) : CompareTagValue(key, value) {
    override fun toString() = "$key < $value"
    override fun compareTo(tagValue: Float) = tagValue < value
}
internal class HasTagGreaterThan(key: String, value: Float) : CompareTagValue(key, value) {
    override fun toString() = "$key > $value"
    override fun compareTo(tagValue: Float) = tagValue > value
}
internal class HasTagLessOrEqualThan(key: String, value: Float) : CompareTagValue(key, value) {
    override fun toString() = "$key <= $value"
    override fun compareTo(tagValue: Float) = tagValue <= value
}
internal class HasTagGreaterOrEqualThan(key: String, value: Float) : CompareTagValue(key, value) {
    override fun toString() = "$key >= $value"
    override fun compareTo(tagValue: Float) = tagValue >= value
}

internal abstract class CompareTagValue(val key: String, val value: Float) : TagFilter {
    abstract fun compareTo(tagValue: Float): Boolean
    override fun matches(obj: Map<String, String>): Boolean {
        val tagValue = obj[key]?.withOptionalUnitToDoubleOrNull()?.toFloat() ?: return false
        return compareTo(tagValue)
    }
    override val relevantKey get() = RelevantKeyString(key)
}
