package com.rec.gpiv.model

/** Selection order is click order; removing an item closes the numbering gap. */
object OrderedSelection {
    fun <T> toggle(selection: List<T>, item: T): List<T> =
        if (item in selection) selection.filterNot { it == item } else selection + item

    fun <T, K> entries(items: List<T>, selection: List<K>, key: (T) -> K): List<T> {
        val byKey = items.associateBy(key)
        return selection.mapNotNull { byKey[it] }
    }
}
