package de.westnordost.osm_default_speeds

import de.westnordost.osm_default_speeds.tagfilter.TagFilterExpression
import de.westnordost.osm_default_speeds.DefaultSpeeds.Result.Certitude.*
import de.westnordost.osm_default_speeds.tagfilter.withOptionalUnitToDoubleOrNull

interface RoadType {
    val name: String?
    val tags: Map<String, String>
}

interface RoadTypeFilter {
    val filter: String?
    val fuzzyFilter: String?
    val relationFilter: String?
}

/** Class with which to look up the default speed limits per country as specified in the
 *  given data (usually default_speed_limits.json) */
class DefaultSpeeds(
    roadTypes: Map<String, RoadTypeFilter>,
    private val speedLimits: Map<String, List<RoadType>>
) {

    private val roadTypeFilters: Map<String, RoadTypeTagFilterExpressions> =
        roadTypes.mapValues { (_, roadTypeFilter) ->
            /* let's parse the filters defined in strings right in the constructor, so it doesn't
               need to be done again and again (and if there is a syntax error, it becomes apparent
               immediately) */
            RoadTypeTagFilterExpressions(
                roadTypeFilter.filter?.let { TagFilterExpression(it) },
                roadTypeFilter.fuzzyFilter?.let { TagFilterExpression(it) },
                roadTypeFilter.relationFilter?.let { TagFilterExpression(it) }
            )
        }

    /** The result of looking for the speed limits via [getSpeedLimits]. It includes the road type
     *  name, the `maxspeed` tags that road type is assumed to have implicitly and a [Certitude].
     *  */
    data class Result(
        /** The road type name as it appears in the default speed limits wiki page or `null` if it
         *  is the default (fallback) rule */
        val roadTypeName: String?,
        /** The [tags] include only the tags that should be *added* to the tags specified in
         *  [getSpeedLimits]. In particular, if a `maxspeed` is already specified in the input tags,
         *  there will not be a `maxspeed` in these [tags], the same with any subtags. Also, if the
         *  maximum speed is lower than a maximum speed for a vehicle type (or conditional speed),
         *  that speed is also left out. */
        val tags: Map<String, String>,
        val certitude: Certitude
    ) {
        /** Indicates how sure the result can be assumed to be */
        enum class Certitude {
            /** It is an exact match with the road type. I.e., the tag filter for the road type
             *  matched. */
            Exact,
            /** It can be assumed with reasonable certainty that the match is of the given road
             *  type. I.e., the fuzzy tag filter for the road type matched. */
            Fuzzy,
            /** No road type matched, falling back to the default speed limit for "other roads". No
             *  tag filter matched. */
            Fallback
        }
    }

    /**
     * Given a country/subdivision and a set of tags on the road (segment), will return a set of
     * additional `maxspeed` tags the road can be assumed to have based on other properties of the
     * road or (optionally) of which relations it is a member of. Returns `null` if nothing was
     * found.
     *
     * @param countryCode ISO 3166-1 alpha-2 code optionally concatenated with a ISO 3166-2 code,
     *        e.g. "DE", "US" or "BE-VLG"
     *
     * @param tags OpenStreetMap tags of the road (segment) in question
     *
     * @param relationsTags the OpenStreetMap tags of all relations the road (segment) in question
     *        is a member of. Optional, but may lead to more precise results, especially in the
     *        United States.
     *
     * @param replacerFn Optional. You can replace the result of any number of placeholders in a
     *        tag filter here (e.g. for name = "urban"), for example if you have another data source
     *        to acquire whether a road is in a built-up area or not. For those you do not want to
     *        replace, simply pass on the result of evaluate as result
     */
    fun getSpeedLimits(
        countryCode: String,
        tags: Map<String, String>,
        relationsTags: List<Map<String, String>> = emptyList(),
        replacerFn: (name: String, evaluate: () -> Boolean) -> Boolean = { _, ev -> ev() }
    ): Result? {
        val roadTypes = speedLimits[countryCode]
            ?: speedLimits[countryCode.substringBefore('-')]
            ?: return null

        // 1. Try to match non-fuzzy first, if nothing found, then fuzzy
        for (fuzzy in listOf(false, true)) {
            val roadType = findRoadType(roadTypes, tags, relationsTags, fuzzy, replacerFn)
            if (roadType != null) {
                val certitude = if (!fuzzy) Exact else Fuzzy
                return Result(roadType.name, createResultTags(tags, roadType.tags), certitude)
            }
        }

        // 2. Otherwise, match the default (if it exists)
        val fallbackRoadType = roadTypes.find { it.name == null } ?: return null
        return Result(fallbackRoadType.name, createResultTags(tags, fallbackRoadType.tags), Fallback)
    }

    private fun findRoadType(
        roadTypes: List<RoadType>,
        tags: Map<String, String>,
        relationsTags: List<Map<String, String>>,
        fuzzy: Boolean,
        replacerFn: (name: String, evaluate: () -> Boolean) -> Boolean
    ): RoadType? {
        // a. First try to match the road that is defined the furthest to the bottom
        for (roadType in roadTypes.asReversed()) {
            val name = roadType.name ?: break
            if (filtersMatchReplace(name, tags, relationsTags, fuzzy, replacerFn)) return roadType
        }

        // b. If nothing matched, match the road that is defined furthest to the top
        for (roadType in roadTypes) {
            val name = roadType.name ?: break
            if (filtersMatchReplace(name, tags, relationsTags, fuzzy, replacerFn)) return roadType
        }
        return null
    }

    private fun filtersMatchReplace(
        name: String, tags: Map<String, String>, relationsTags: List<Map<String, String>>,
        fuzzy: Boolean, replacerFn: (name: String, evaluate: () -> Boolean) -> Boolean
    ): Boolean =
        replacerFn(name) { filtersMatch(name, tags, relationsTags, fuzzy, replacerFn) }

    private fun filtersMatch(
        name: String, tags: Map<String, String>, relationsTags: List<Map<String, String>>,
        fuzzy: Boolean, replacerFn: (name: String, evaluate: () -> Boolean) -> Boolean
    ): Boolean {
        val f = roadTypeFilters[name] ?: return false
        val fn: (String) -> Boolean = { filtersMatchReplace(it, tags, relationsTags, fuzzy, replacerFn) }

        return relationsTags.any { f.relationFilter?.matches(it, fn) == true }
            || f.filter?.matches(tags, fn) == true
            || fuzzy && f.fuzzyFilter?.matches(tags, fn) == true
    }
}

private fun createResultTags(tags: Map<String, String>, roadTypeTags: Map<String, String>): Map<String, String> {
    val result = roadTypeTags.toMutableMap()
    result.putAll(tags)
    result.limitSpeedsTo("maxspeed", result["maxspeed"])
    for (key in tags.keys) result.remove(key)
    return result
}

private fun MutableMap<String,String>.limitSpeedsTo(key: String, value: String?) {
    val v = value?.withOptionalUnitToDoubleOrNull()
    if (v != null) {
        // remove sub-entries
        entries.removeAll {
            if (!it.key.startsWith("$key:")) return@removeAll false
            val speed = it.value.withOptionalUnitToDoubleOrNull() ?: return@removeAll false
            speed >= v
        }
        // search & remove through conditionals string
        val conditionalKey = "$key:conditional"
        val conditionals = get(conditionalKey)?.split("; ")?.toMutableList()
        if (conditionals != null) {
            conditionals.removeAll {
                val speed = it.split(" @ ")[0].withOptionalUnitToDoubleOrNull() ?: return@removeAll false
                speed >= v
            }
            val newConditional = conditionals.joinToString("; ")
            if (newConditional.isEmpty()) {
                remove(conditionalKey)
            } else {
                put(conditionalKey, newConditional)
            }
        }
    }
    // recurse down
    val r = Regex("$key:[a-z_]+")
    val subkeys = keys.filter { r.matches(it) }
    for (subkey in subkeys) {
        limitSpeedsTo(subkey, this[subkey])
    }
}

private data class RoadTypeTagFilterExpressions(
    val filter: TagFilterExpression?,
    val fuzzyFilter: TagFilterExpression?,
    val relationFilter: TagFilterExpression?,
)

// TODO reverse search?!
