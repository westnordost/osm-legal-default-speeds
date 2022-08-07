package de.westnordost.osm_legal_default_speeds

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlin.test.*

class ParseActualDataTest {
    @OptIn(ExperimentalSerializationApi::class)
    @Test fun parse_actual_data() {
        // just parse, it should just not throw an exception
        val url = javaClass.getResource("/legal_default_speeds.json")!!
        val json = Json { ignoreUnknownKeys = true }
        val data: SpeedLimitsJson = json.decodeFromStream(url.openStream())
        LegalDefaultSpeeds(data.roadTypesByName, data.speedLimitsByCountryCode)
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
