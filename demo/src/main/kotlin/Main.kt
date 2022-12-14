import de.westnordost.osm_legal_default_speeds.Certitude
import de.westnordost.osm_legal_default_speeds.LegalDefaultSpeeds
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.w3c.dom.*
import org.w3c.dom.events.InputEvent
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
private var hashParams
    get() = window.location.hashParams
    set(value) { window.location.hashParams = value }

fun main() {
    resultTags.style.visibility = "none"
    tagsInput.value = hashParams["tags"] ?: ""

    window.onhashchange = ::applyFromHash
    countrySelect.oninput = ::applyFromInput
    tagsInput.oninput = ::applyFromInput

    scope.launch {
        val speedLimitsJson = fetchSpeedLimitsJson()

        speeds = LegalDefaultSpeeds(speedLimitsJson.roadTypesByName, speedLimitsJson.speedLimitsByCountryCode)

        initMetadataInfo(speedLimitsJson.meta)
        initializeCountrySelect(speedLimitsJson.speedLimitsByCountryCode.keys)
        applyFromHash(null)
    }
}

private fun applyFromHash(event: HashChangeEvent?) {
    countrySelect.oninput = null
    tagsInput.oninput = null

    tagsInput.value = hashParams["tags"] ?: ""
    countrySelect.value = hashParams["cc"] ?: "IT"

    countrySelect.oninput = ::applyFromInput
    tagsInput.oninput = ::applyFromInput

    updateOutput()
}

private fun applyFromInput(event: InputEvent?) {
    window.onhashchange = null

    hashParams = hashParams.toMutableMap().also {
        it["tags"] = tagsInput.value
        it["cc"] = countrySelect.value
    }

    window.onhashchange = ::applyFromHash

    updateOutput()
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
    tagsInput.rows = max(Regex("\n").findAll(tagsInput.value).count() + 1, 3)

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
