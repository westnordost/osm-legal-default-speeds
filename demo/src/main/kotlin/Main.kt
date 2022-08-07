import de.westnordost.osm_legal_default_speeds.Certitude
import de.westnordost.osm_legal_default_speeds.LegalDefaultSpeeds
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.w3c.dom.*
import org.w3c.dom.url.URLSearchParams
import kotlin.js.Date
import kotlin.math.max

private val countrySelect = document.getElementById("countrySelect") as HTMLSelectElement
private val tagsInput = document.getElementById("tagsInput") as HTMLTextAreaElement
private val roadType = document.getElementById("roadType") as HTMLDivElement
private val matchType = document.getElementById("matchType") as HTMLDivElement
private val resultTags = document.getElementById("resultTags") as HTMLTextAreaElement
private val wikiUrl = document.getElementById("wikiUrl") as HTMLAnchorElement
private val revisionText = document.getElementById("revisionText") as HTMLSpanElement

private val scope = MainScope()
private var speeds: LegalDefaultSpeeds? = null
private val hashParams = URLSearchParams(window.location.hash)

fun main() {

    resultTags.style.visibility = "none"

    countrySelect.oninput = {
        hashParams.set("cc", countrySelect.value)
        window.location.hash = hashParams.toString()
        updateOutput()
    }
    tagsInput.oninput = {
        hashParams.set("tags", tagsInput.value)
        window.location.hash = hashParams.toString()
        tagsInput.rows = max(Regex("\n").findAll(tagsInput.value).count() + 1, 3)
        updateOutput()
    }

    scope.launch {
        val speedLimitsJson = fetchSpeedLimitsJson()

        speeds = LegalDefaultSpeeds(speedLimitsJson.roadTypesByName, speedLimitsJson.speedLimitsByCountryCode)

        tagsInput.value = hashParams.get("tags") ?: ""
        initMetadataInfo(speedLimitsJson.meta)
        initializeCountrySelect(speedLimitsJson.speedLimitsByCountryCode.keys)

        updateOutput()
    }
}

private suspend fun fetchSpeedLimitsJson(): SpeedLimitsJson {
    val response = window.fetch("./legal_default_speeds.json").await()
    val text = response.text().await()
    return Json.decodeFromString(text)
}

private fun initMetadataInfo(meta: Map<String, String>) {
    wikiUrl.href = meta.getValue("source")
    val revisionId = meta.getValue("revisionId")
    val date = Date(meta.getValue("timestamp")).toDateString()
    revisionText.innerText = "(revision $revisionId, $date)"
}

private fun initializeCountrySelect(countryCodes: Collection<String>) {
    for (countryCode in countryCodes.sortedBy { getCountryName(it) }) {
        countrySelect.appendChild(createCountryOption(countryCode))
    }

    countrySelect.value = hashParams.get("cc") ?: "IT"
}

private fun createCountryOption(countryCode: String): HTMLOptionElement {
    val option = document.createElement("option") as HTMLOptionElement
    val flagAndName = getCountryFlagEmoji(countryCode.substring(0,2)) + " " + getCountryName(countryCode)
    val optionText = document.createTextNode(flagAndName)
    option.appendChild(optionText)
    option.value = countryCode
    return option
}

private val Certitude.description: String get() = when(this) {
    Certitude.Exact -> "The tag filter matched this road type."
    Certitude.FromMaxSpeed -> "The road type was inferred from the <code>maxspeed</code> given in the input"
    Certitude.Fuzzy -> "The <em>fuzzy</em> tag filter matched this road type."
    Certitude.Fallback -> "Nothing matched, falling back to &quot;other roads&quot;."
}

private fun updateOutput() {
    val speeds = speeds ?: return
    val inputTags = tagsInput.value.toTags()
    val inputCountry = countrySelect.value
    val result = speeds.getSpeedLimits(inputCountry, inputTags)

    if (result != null) {
        roadType.innerHTML = "<strong>Match</strong>: " + (result.roadTypeName ?: "None")
        resultTags.value = result.tags.toTagString()
        resultTags.rows = result.tags.size
        resultTags.style.visibility = "visible"
        matchType.innerHTML = "" + result.certitude.name + ": " + result.certitude.description
    } else {
        roadType.innerText = "Nothing found"
        resultTags.style.visibility = "none"
        matchType.innerText = ""
    }
}
