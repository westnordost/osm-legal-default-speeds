package de.westnordost.osm_legal_default_speeds.tagfilter.filters

internal sealed interface RelevantKey

internal data class RelevantKeyString(val key: String) : RelevantKey

internal data class RelevantKeyRegex(val regex: RegexOrSet) : RelevantKey