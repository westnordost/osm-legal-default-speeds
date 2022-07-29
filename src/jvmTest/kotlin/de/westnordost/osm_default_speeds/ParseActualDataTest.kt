package de.westnordost.osm_default_speeds

import de.westnordost.osm_default_speeds.tagfilter.ParseException
import de.westnordost.osm_default_speeds.tagfilter.TagFilterExpression
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import java.net.URL
import kotlin.test.*


class ParseActualDataTest {
    @Test fun parse_actual_data() {
        // just parse, it should just not throw an exception
        val url = URL("https://raw.githubusercontent.com/westnordost/osm-default-speeds/master/output/default_speeds.json")
        val data = Json {ignoreUnknownKeys = true}.decodeFromStream<SpeedLimitsJson>(url.openStream())
        for ((name, roadType) in data.roadTypes.entries) {
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

@Serializable
internal data class SpeedLimitsJson(
    val meta: Map<String, String>,
    @SerialName("road_types")
    val roadTypes: Map<String, RoadTypeFilterJson>,
    @SerialName("speed_limits")
    val speedLimits: Map<String, List<RoadTypeJson>>,
    val warnings: List<String>
)

@Serializable
internal data class RoadTypeFilterJson(
    override val filter: String? = null,
    @SerialName("fuzzy_filter")
    override val fuzzyFilter: String? = null
) : RoadTypeFilter

@Serializable
internal data class RoadTypeJson(
    override val name: String? = null,
    override val tags: Map<String, String>
) : RoadType
