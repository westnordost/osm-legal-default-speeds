package de.westnordost.osm_legal_default_speeds.tagfilter

internal abstract class BooleanExpression<I : Matcher<T>, T> {
    var parent: Chain<I, T>? = null
        internal set

    abstract fun matches(obj: T, evaluate: (name: String) -> Boolean): Boolean
}

internal abstract class Chain<I : Matcher<T>, T> : BooleanExpression<I, T>() {
    protected val nodes = ArrayList<BooleanExpression<I, T>>()

    val children: List<BooleanExpression<I, T>> get() = nodes.toList()

    open fun addChild(child: BooleanExpression<I, T>) {
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
            if (child.nodes.size == 1 && child !is Not<I, T>) {
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
            if (child is Not<I, T>) {
                continue
            }
            child.mergeNodesWithSameOperator()

            // merge two successive nodes of same type
            if (child::class == this::class) {
                replaceChildAt(it, *child.children.toTypedArray())
            }
        }
    }

    fun getPlaceholders(): Sequence<String> = sequence {
        for (node in nodes) {
            if (node is Chain) {
                yieldAll(node.getPlaceholders())
            } else if (node is Placeholder) {
                yield(node.value)
            }
        }
    }

    fun getItems(): Sequence<I> = sequence {
        for (node in nodes) {
            if (node is Chain) {
                yieldAll(node.getItems())
            } else if (node is Leaf) {
                yield(node.value)
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
    override fun matches(obj: T, evaluate: (name: String) -> Boolean) =
        nodes.all { it.matches(obj, evaluate) }
    override fun toString() =
        nodes.joinToString(" and ") { if (it is AnyOf) "($it)" else "$it" }
}

internal class AnyOf<I : Matcher<T>, T> : Chain<I, T>() {
    override fun matches(obj: T, evaluate: (name: String) -> Boolean) =
        nodes.any { it.matches(obj, evaluate) }
    override fun toString() =
        nodes.joinToString(" or ") { "$it" }
}

internal class Not<I : Matcher<T>, T> : Chain<I, T>() {
    override fun addChild(child: BooleanExpression<I, T>) {
        check(nodes.isEmpty()) { "Adding a second child to '!' (NOT) operator is not allowed" }
        super.addChild(child)
    }

    override fun matches(obj: T, evaluate: (name: String) -> Boolean) =
        !nodes.first().matches(obj, evaluate)

    override fun toString() = "!(${nodes.firstOrNull() ?: ""})"
}

internal interface Matcher<in T> {
    fun matches(obj: T): Boolean
}
