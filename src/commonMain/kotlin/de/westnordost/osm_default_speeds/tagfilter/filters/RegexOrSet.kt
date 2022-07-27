package de.westnordost.osm_default_speeds.tagfilter.filters

/** Either works like a regex if there is a real regex in the string or otherwise as a set if the
 *  regex only consists of a string with pipes, e.g. bakery|pharmacy|clock */
internal sealed class RegexOrSet {
    abstract fun matches(string: String): Boolean

    companion object {
        private val anyRegexStuffExceptPipe = Regex("[.\\[\\]{}()<>*+-=!?^$]")

        fun from(string: String): RegexOrSet {
            return if (!string.contains(anyRegexStuffExceptPipe)) {
                SetRegex(string.split('|').toSet())
            } else {
                RealRegex(string.toRegex())
            }
        }
    }
}

internal class RealRegex(private val regex: Regex) : RegexOrSet() {
    override fun matches(string: String) = regex.matches(string)
}

internal class SetRegex(private val set: Set<String>) : RegexOrSet() {
    override fun matches(string: String) = set.contains(string)
}
