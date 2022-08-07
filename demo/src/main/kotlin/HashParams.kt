import org.w3c.dom.Location

external fun decodeURIComponent(encodedURI: String): String
external fun encodeURIComponent(uriComponent: String): String

var Location.hashParams: Map<String, String>
get() = hash.substringAfter('#')
        .split('&')
        .associate {
            val splits = it.split('=', limit = 2)
            val key = decodeURIComponent(splits[0])
            val value = if (splits.size > 1) decodeURIComponent(splits[1]) else ""
            key to value
        }
set(value) {
    hash = value.entries.joinToString(separator = "&", prefix = "#") { (key, value) ->
        encodeURIComponent(key) + "=" + encodeURIComponent(value)
    }
}