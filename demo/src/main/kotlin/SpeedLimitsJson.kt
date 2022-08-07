import de.westnordost.osm_legal_default_speeds.RoadType
import de.westnordost.osm_legal_default_speeds.RoadTypeFilter
import kotlinx.serialization.Serializable

@Serializable internal data class SpeedLimitsJson(
    val meta: Map<String, String>,
    val roadTypesByName: Map<String, RoadTypeFilterJson>,
    val speedLimitsByCountryCode: Map<String, List<RoadTypeJson>>,
    val warnings: List<String>
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
