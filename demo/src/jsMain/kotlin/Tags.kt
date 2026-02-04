fun String.toTags(): Map<String, String> =
    split(Regex("[\n\r]+")).mapNotNull { it.toTag() }.toMap()

private fun String.toTag(): Pair<String, String>? {
    val equal = indexOf('=')
    if (equal == -1) return null
    val key = substring(0, equal).trim()
    val value = substring(equal+1).trim()
    if (key.isEmpty() || value.isEmpty()) return null
    return key to value
}

fun Map<String, String>.toTagString(): String =
    entries.joinToString("\n") { it.key + "=" + it.value }
