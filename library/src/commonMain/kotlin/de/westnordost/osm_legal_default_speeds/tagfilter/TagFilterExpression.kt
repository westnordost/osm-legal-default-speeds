package de.westnordost.osm_legal_default_speeds.tagfilter

import de.westnordost.osm_legal_default_speeds.tagfilter.filters.RelevantKey
import de.westnordost.osm_legal_default_speeds.tagfilter.filters.TagFilter

class TagFilterExpression internal constructor(
    private val filters: BooleanExpression<TagFilter, Map<String, String>>
) {
    constructor(filters: String) : this(StringWithCursor(filters).parseTags())

    fun matches(tags: Map<String, String>, evaluate: (name: String) -> Boolean): Boolean =
        filters.matches(tags, evaluate)

    internal fun getPlaceholders(): Sequence<String> = when (filters) {
        is Chain -> filters.getPlaceholders()
        is Placeholder -> sequenceOf(filters.value)
        else -> sequenceOf()
    }

    internal fun getRelevantKeys(): Set<RelevantKey> = when (filters) {
        is Chain -> filters.getItems()
        is Leaf -> sequenceOf(filters.value)
        else -> sequenceOf()
    }.map { it.relevantKey }.toSet()
}
