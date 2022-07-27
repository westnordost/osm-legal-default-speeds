package de.westnordost.osm_default_speeds.tagfilter

import de.westnordost.osm_default_speeds.tagfilter.filters.TagFilter

class TagFilterExpression internal constructor(
    private val filters: BooleanExpression<TagFilter, Map<String, String>>
) {
    constructor(filters: String) : this(StringWithCursor(filters).parseTags())

    fun matches(tags: Map<String, String>, evaluate: (name: String) -> Boolean): Boolean =
        filters.matches(tags, evaluate)
}
