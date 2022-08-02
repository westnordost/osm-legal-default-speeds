package de.westnordost.osm_legal_default_speeds

import de.westnordost.osm_legal_default_speeds.tagfilter.ParseException
import de.westnordost.osm_legal_default_speeds.tagfilter.TagFilterExpression
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlin.test.*


class ParseActualDataTest {
    @OptIn(ExperimentalSerializationApi::class)
    @Test fun parse_actual_data() {
        // just parse, it should just not throw an exception
        val url = javaClass.getResource("/default_speeds.json")!!
        val json = Json { ignoreUnknownKeys = true }
        val data: SpeedLimitsJson = json.decodeFromStream(url.openStream())
        for ((name, roadType) in data.roadTypesByName.entries) {
            roadType.filter?.let {
                try {
                    TagFilterExpression(it)
                } catch (e: ParseException) {
                    throw Exception("Error parsing $name - $it", e)
                }
            }
            roadType.fuzzyFilter?.let {
                try {
                    TagFilterExpression(it)
                } catch (e: ParseException) {
                    throw Exception("Error parsing $name - $it", e)
                }
            }
        }
    }
}

@Serializable internal data class SpeedLimitsJson(
    val roadTypesByName: Map<String, RoadTypeFilterJson>,
    val speedLimitsByCountryCode: Map<String, List<RoadTypeJson>>
)

@Serializable internal data class RoadTypeFilterJson(
    override val filter: String? = null,
    override val fuzzyFilter: String? = null,
    override val relationFilter: String? = null
) : RoadTypeFilter

@Serializable internal data class RoadTypeJson(
    override val name: String? = null,
    override val tags: Map<String, String>
) : RoadType
