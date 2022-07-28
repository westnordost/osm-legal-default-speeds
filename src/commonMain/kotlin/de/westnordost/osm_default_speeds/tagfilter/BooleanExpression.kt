package de.westnordost.osm_default_speeds.tagfilter

internal abstract class BooleanExpression<I : Matcher<T>, T> {
    var parent: Chain<I, T>? = null
        internal set

    abstract fun matches(obj: T, evaluate: (name: String) -> Boolean): Boolean
}

internal abstract class Chain<I : Matcher<T>, T> : BooleanExpression<I, T>() {
    protected val nodes = ArrayList<BooleanExpression<I, T>>()

    val children: List<BooleanExpression<I, T>> get() = nodes.toList()

    fun addChild(child: BooleanExpression<I, T>) {
        child.parent = this
        nodes.add(child)
    }

    fun removeChild(child: BooleanExpression<I, T>) {
        nodes.remove(child)
        child.parent = null
    }

    fun replaceChild(replace: BooleanExpression<I, T>, with: BooleanExpression<I, T>) {
        val it = nodes.listIterator()
        while (it.hasNext()) {
            val child = it.next()
            if (child === replace) {
                replaceChildAt(it, with)
                return
            }
        }
    }

    private fun replaceChildAt(
        at: MutableListIterator<BooleanExpression<I, T>>,
        vararg with: BooleanExpression<I, T>
    ) {
        at.remove()
        for (w in with) {
            at.add(w)
            w.parent = this
        }
    }

    /** Removes unnecessary depth in the expression tree  */
    fun flatten() {
        removeEmptyNodes()
        mergeNodesWithSameOperator()
    }

    /** remove nodes from superfluous brackets  */
    private fun removeEmptyNodes() {
        val it = nodes.listIterator()
        while (it.hasNext()) {
            val child = it.next() as? Chain ?: continue
            if (child.nodes.size == 1) {
                replaceChildAt(it, child.nodes.first())
                it.previous() // = the just replaced node will be checked again
            } else {
                child.removeEmptyNodes()
            }
        }
    }

    /** merge children recursively which do have the same operator set (and, or)  */
    private fun mergeNodesWithSameOperator() {
        val it = nodes.listIterator()
        while (it.hasNext()) {
            val child = it.next() as? Chain ?: continue
            child.mergeNodesWithSameOperator()

            // merge two successive nodes of same type
            if (child::class == this::class) {
                replaceChildAt(it, *child.children.toTypedArray())
            }
        }
    }
}

internal class Placeholder<I : Matcher<T>, T>(val value: String) : BooleanExpression<I, T>() {
    override fun matches(obj: T, evaluate: (name: String) -> Boolean) = evaluate(value)
    override fun toString() = "{$value}"
}

internal class NotPlaceholder<I : Matcher<T>, T>(val value: String) : BooleanExpression<I, T>() {
    override fun matches(obj: T, evaluate: (name: String) -> Boolean) = !evaluate(value)
    override fun toString() = "!{$value}"
}

internal class Leaf<I : Matcher<T>, T>(val value: I) : BooleanExpression<I, T>() {
    override fun matches(obj: T, evaluate: (name: String) -> Boolean) = value.matches(obj)
    override fun toString() = value.toString()
}

internal class AllOf<I : Matcher<T>, T> : Chain<I, T>() {
    override fun matches(obj: T, evaluate: (name: String) -> Boolean) = nodes.all { it.matches(obj, evaluate) }
    override fun toString() = nodes.joinToString(" and ") { if (it is AnyOf) "($it)" else "$it" }
}

internal class AnyOf<I : Matcher<T>, T> : Chain<I, T>() {
    override fun matches(obj: T, evaluate: (name: String) -> Boolean) = nodes.any { it.matches(obj, evaluate) }
    override fun toString() = nodes.joinToString(" or ") { "$it" }
}

internal interface Matcher<in T> {
    fun matches(obj: T): Boolean
}
