package de.westnordost.osm_default_speeds

import de.westnordost.osm_default_speeds.tagfilter.ParseException
import de.westnordost.osm_default_speeds.DefaultSpeeds.Result.Certitude.*
import kotlin.test.*

internal class DefaultSpeedsTest {

    val za = DefaultSpeeds(
        mapOf(
            "living street" to filters("highway=living_street"), // only exact filter
            "alley" to filters("{urban} and alley=yes"), // with placeholder
            "urban" to filters("lit=yes", "highway=residential"), // both exact and fuzzy filter
            "rural" to filters(null, "sidewalk=no"), // only fuzzy filter
            "dual carriageway" to filters("dual_carriageway=yes"), // only fuzzy filter
            "motorway" to filters("highway=motorway"),
        ),
        mapOf(
            "ZA" to listOf(
                road("living street", mapOf("maxspeed" to "10")),
                road("alley", mapOf("maxspeed" to "5")),
                road("urban", mapOf("maxspeed" to "50")),
                road(null, mapOf("maxspeed" to "100")), // default rule
                road("rural", mapOf("maxspeed" to "100")),
                road("dual carriageway", mapOf("maxspeed" to "110")),
                road("motorway", mapOf("maxspeed" to "120")),
            )
        )
    )

    @Test fun fails_on_syntax_exception_in_filter() {
        assertFailsWith(ParseException::class) {
            DefaultSpeeds(
                mapOf("urban" to filters("and and")),
                mapOf("FR" to listOf(road("urban", mapOf("maxspeed" to "50"))))
            )
        }
    }

    @Test fun no_tags_match() {
        assertNull(DefaultSpeeds(
            mapOf("urban" to filters("lit=yes")),
            mapOf("SD" to listOf(road("urban", mapOf("maxspeed" to "60"))))
        ).getSpeedLimits("SD", mapOf("lit" to "no")))
    }

    @Test fun unknown_country() {
        assertNull(za.getSpeedLimits("GY", mapOf("lit" to "yes")))
    }

    @Test fun fallback_when_no_tags_match() {
        assertEquals(
            DefaultSpeeds.Result(null, mapOf("maxspeed" to "100"), Fallback),
            za.getSpeedLimits("ZA", mapOf("lit" to "no"))
        )
    }

    @Test fun simple_match() {
        assertEquals(
            DefaultSpeeds.Result("urban", mapOf("maxspeed" to "50"), Exact),
            za.getSpeedLimits("ZA", mapOf("lit" to "yes"))
        )
    }

    @Test fun fuzzy_match() {
        assertEquals(
            DefaultSpeeds.Result("urban", mapOf("maxspeed" to "50"), Fuzzy),
            za.getSpeedLimits("ZA", mapOf("highway" to "residential"))
        )
    }

    @Test fun fallback_to_country_if_subdivision_unknown() {
        assertEquals(
            DefaultSpeeds.Result("urban", mapOf("maxspeed" to "50"), Exact),
            za.getSpeedLimits("ZA-NC", mapOf("lit" to "yes"))
        )
    }

    @Test fun prefer_matches_further_down_the_list() {
        assertEquals(
            DefaultSpeeds.Result("motorway", mapOf("maxspeed" to "120"), Exact),
            za.getSpeedLimits("ZA", mapOf("highway" to "motorway", "lit" to "yes", "dual_carriageway" to "yes"))
        )
        assertEquals(
            DefaultSpeeds.Result("dual carriageway", mapOf("maxspeed" to "110"), Exact),
            za.getSpeedLimits("ZA", mapOf("lit" to "yes", "dual_carriageway" to "yes"))
        )
    }

    @Test fun prefer_matches_further_at_the_top_of_the_list_otherwise() {
        assertEquals(
            DefaultSpeeds.Result("living street", mapOf("maxspeed" to "10"), Exact),
            za.getSpeedLimits("ZA", mapOf("highway" to "living_street", "lit" to "yes"))
        )
    }

    @Test fun prefer_exact_over_fuzzy_rules() {
        assertEquals(
            DefaultSpeeds.Result("urban", mapOf("maxspeed" to "50"), Exact),
            za.getSpeedLimits("ZA", mapOf("lit" to "yes", "sidewalk" to "no"))
        )
    }

    @Test fun exact_match_with_placeholder() {
        assertEquals(
            DefaultSpeeds.Result("alley", mapOf("maxspeed" to "5"), Exact),
            za.getSpeedLimits("ZA", mapOf("lit" to "yes", "alley" to "yes"))
        )
    }

    @Test fun fuzzy_match_with_placeholder() {
        assertEquals(
            DefaultSpeeds.Result("alley", mapOf("maxspeed" to "5"), Fuzzy),
            za.getSpeedLimits("ZA", mapOf("highway" to "residential", "alley" to "yes"))
        )
    }

    // TODO tests for replace function...
}

internal data class RoadTypeFilterImpl(
    override val filter: String?,
    override val fuzzyFilter: String?
) : RoadTypeFilter

internal data class RoadTypeImpl(
    override val name: String?,
    override val tags: Map<String, String>
) : RoadType

internal fun road(name: String? = null, tags: Map<String, String> = mapOf()) =
    RoadTypeImpl(name, tags)

internal fun filters(filter: String? = null, fuzzyFilter: String? = null) =
    RoadTypeFilterImpl(filter, fuzzyFilter)