package de.westnordost.osm_legal_default_speeds

import de.westnordost.osm_legal_default_speeds.tagfilter.ParseException
import de.westnordost.osm_legal_default_speeds.tagfilter.TagFilterExpression
import de.westnordost.osm_legal_default_speeds.tagfilter.withOptionalUnitToDoubleOrNull

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
class LegalDefaultSpeeds(
    roadTypesByName: Map<String, RoadTypeFilter>,
    private val speedLimitsByCountryCode: Map<String, List<RoadType>>
) {
    private val roadTypeFilters: Map<String, RoadTypeTagFilterExpressions> = roadTypesByName.mapValues { (roadName, roadTypeFilter) ->
        /* let's parse the filters defined in strings right in the constructor, so it doesn't
           need to be done again and again (and if there is a syntax error, it becomes apparent
           immediately) */
        val filter = try {
            roadTypeFilter.filter?.let { TagFilterExpression(it) }
        } catch (e: ParseException) {
            throw IllegalArgumentException("Invalid road type filter for \"$roadName\"", e)
        }

        val fuzzyFilter = try {
            roadTypeFilter.fuzzyFilter?.let { TagFilterExpression(it) }
        } catch (e: ParseException) {
            throw IllegalArgumentException("Invalid road type fuzzyFilter for \"$roadName\"", e)
        }

        val relationFilter = try {
            roadTypeFilter.relationFilter?.let { TagFilterExpression(it) }
        } catch (e: ParseException) {
            throw IllegalArgumentException("Invalid road type relationFilter for \"$roadName\"", e)
        }

        RoadTypeTagFilterExpressions(filter, fuzzyFilter, relationFilter)
    }

    init {
        checkForCircularPlaceholders()
    }

    private fun checkForCircularPlaceholders() {
        // map of e.g. "rural paved road" -> setOf("rural", "paved road") etc.
        val placeholdersByRoadName: Map<String, Set<String>> =
            roadTypeFilters.entries.associate { (roadName, roadTypeFilter) ->
                roadName to sequence {
                    roadTypeFilter.filter?.getPlaceholders()?.let { yieldAll(it) }
                    roadTypeFilter.fuzzyFilter?.getPlaceholders()?.let { yieldAll(it) }
                    roadTypeFilter.relationFilter?.getPlaceholders()?.let { yieldAll(it) }
                }.toSet()
            }

        for ((roadName, placeholders) in placeholdersByRoadName) {
            val collectedPlaceholders = placeholders.toMutableSet()

            var placeholdersToExpand = placeholders
            while (placeholdersToExpand.isNotEmpty()) {
                val expandedPlaceholders = HashSet<String>()
                for (placeholder in placeholdersToExpand) {
                    val referredPlaceholders = placeholdersByRoadName[placeholder] ?: emptyList()
                    expandedPlaceholders.addAll(referredPlaceholders)
                }
                expandedPlaceholders.removeAll(collectedPlaceholders)
                collectedPlaceholders.addAll(expandedPlaceholders)
                placeholdersToExpand = expandedPlaceholders
            }

            if (roadName in collectedPlaceholders) {
                throw IllegalArgumentException("A road type filter for \"$roadName\" contains circular placeholders")
            }
        }
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
    )

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
        val roadTypes = speedLimitsByCountryCode[countryCode]
            ?: speedLimitsByCountryCode[countryCode.substringBefore('-')]
            ?: return null

        // 1. Try to match tags first
        val exactRoadType = findRoadTypeByTags(roadTypes, tags, relationsTags, false, replacerFn)
        if (exactRoadType != null) {
            return Result(exactRoadType.name, createResultTags(tags, exactRoadType.tags), Certitude.Exact)
        }
        // 2. If a `maxspeed` is set, try to reverse-search by maxspeed
        val maxSpeedRoadType = findRoadTypeByMaxSpeed(roadTypes, tags)
        if (maxSpeedRoadType != null) {
            return Result(maxSpeedRoadType.name, createResultTags(tags, maxSpeedRoadType.tags), Certitude.FromMaxSpeed)
        }

        // 3. If still nothing is found, try to match fuzzy tags
        val fuzzyRoadType = findRoadTypeByTags(roadTypes, tags, relationsTags, true, replacerFn)
        if (fuzzyRoadType != null) {
            return Result(fuzzyRoadType.name, createResultTags(tags, fuzzyRoadType.tags), Certitude.Fuzzy)
        }

        // 4. Otherwise, match the default (if it exists)
        val fallbackRoadType = roadTypes.find { it.name == null }
        if (fallbackRoadType != null) {
            return Result(fallbackRoadType.name, createResultTags(tags, fallbackRoadType.tags), Certitude.Fallback)
        }
        return null
    }

    private fun findRoadTypeByTags(
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

    private fun findRoadTypeByMaxSpeed(roadTypes: List<RoadType>, tags: Map<String, String>): RoadType? {
        val maxspeed = tags["maxspeed"] ?: return null
        // a. First try to match the road that is defined the furthest to the bottom
        for (roadType in roadTypes.asReversed()) {
            roadType.name ?: break
            if (maxspeed == roadType.tags["maxspeed"]) return roadType
        }

        // b. If nothing matched, match the road that is defined furthest to the top
        for (roadType in roadTypes) {
            roadType.name ?: break
            if (maxspeed == roadType.tags["maxspeed"]) return roadType
        }
        return null
    }
}

private fun createResultTags(tags: Map<String, String>, roadTypeTags: Map<String, String>): Map<String, String> {
    val result = roadTypeTags.toMutableMap()
    result.putAll(tags.filter { !it.isImplicitMaxSpeed })
    val maxspeed = result["maxspeed"]?.withOptionalUnitToDoubleOrNull()
    result.limitSpeedsTo("maxspeed", maxspeed)
    tags.entries.forEach { if (!it.isImplicitMaxSpeed) result.remove(it.key) }
    return result
}

// stuff like maxspeed=RO:urban from the input tags should not overwrite explicit speed limits from output tags
private val Map.Entry<String, String>.isImplicitMaxSpeed get() =
    key == "maxspeed" && value.withOptionalUnitToDoubleOrNull() == null

private fun MutableMap<String,String>.limitSpeedsTo(key: String, maxspeed: Double?) {
    if (maxspeed != null) {
        val iter = entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val idx1 = entry.key.indexOf("$key:")
            if (idx1 == 0) {
                val idx2 = entry.key.indexOf(":conditional")
                if (idx2 > 0) {
                    /* search & remove through conditionals strings. E.g. if maxspeed=60, turn
                       maxspeed:hgv:conditional=80 @ (trailer); 40 @ (weight>30t) into
                       maxspeed:hgv:conditional=40 @ (weight>30t) or delete if no conditionals are left
                       after removing those that are higher */
                    val conditionals = entry.value.split("; ").toMutableList()
                    conditionals.removeAll {
                        val speed = it.split(" @ ")[0].withOptionalUnitToDoubleOrNull() ?: return@removeAll false
                        speed >= maxspeed
                    }
                    val newConditional = conditionals.joinToString("; ")
                    if (newConditional.isEmpty()) {
                        iter.remove()
                    } else {
                        entry.setValue(newConditional)
                    }
                }

                /* remove higher speeds. E.g. if maxspeed=60, remove maxspeed:hgv=80 */
                val speed = entry.value.withOptionalUnitToDoubleOrNull()
                if (speed != null && speed >= maxspeed) {
                    iter.remove()
                }
            }
        }
    }
    /* recurse down. The same should be done for e.g. maxspeed:hgv:conditional if maxspeed:hgv
    *  already has a lower speed limit etc. but copy before as keys will be modified */
    var copiedKeys = keys.toMutableSet()
    for (subkey in copiedKeys) {
        val index = subkey.indexOf("$key:")
        if (index < 0) continue
        val subMaxspeed = this[subkey]?.withOptionalUnitToDoubleOrNull()
        limitSpeedsTo(subkey, listOfNotNull(maxspeed, subMaxspeed).minOrNull())
    }
}

/** Indicates how sure the result can be assumed to be */
enum class Certitude {
    /** It is an exact match with the road type. I.e., the tag filter for the road type matched. */
    Exact,
    /** The road type was inferred from the `maxspeed` given in the input */
    FromMaxSpeed,
    /** It can be assumed with reasonable certainty that the match is of the given road type.
     * I.e., the fuzzy tag filter for the road type matched. */
    Fuzzy,
    /** No road type matched, falling back to the default speed limit for "other roads". No tag
     * filter matched. */
    Fallback
}

private data class RoadTypeTagFilterExpressions(
    val filter: TagFilterExpression?,
    val fuzzyFilter: TagFilterExpression?,
    val relationFilter: TagFilterExpression?,
)
