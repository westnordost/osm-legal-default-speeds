import org.w3c.dom.Location

external fun decodeURIComponent(encodedURI: String): String
external fun encodeURIComponent(uriComponent: String): String

var Location.hashParams: Map<String, String>
get() = hash.substringAfter('#')
        .split('&')
        .mapNotNull {
            val splits = it.split('=', limit = 2)
            val key = decodeURIComponent(splits[0])
            if (key.isEmpty()) return@mapNotNull null
            val value = if (splits.size > 1) decodeURIComponent(splits[1]) else ""
            key to value
        }.toMap()
set(value) {
    hash = value.entries.mapNotNull {
        val key = encodeURIComponent(it.key)
        val value = encodeURIComponent(it.value)
        if (key.isEmpty()) return@mapNotNull null
        if (value.isNotEmpty()) "$key=$value" else key
    }.joinToString("&", "#")
}