package org.wikipedia.util

import org.apache.commons.lang3.ArrayUtils

object ArrayUtils {
    @JvmStatic
    fun <T> removeAllOccurrences(array: Array<T>, element: T): Array<T> {
        var index = ArrayUtils.indexOf(array, element)
        if (index == ArrayUtils.INDEX_NOT_FOUND) {
            return ArrayUtils.clone(array)
        }
        val indices = IntArray(array.size - index)
        indices[0] = index
        var count = 1
        while (ArrayUtils.indexOf(array, element, indices[count - 1] + 1).also { index = it } != ArrayUtils.INDEX_NOT_FOUND) {
            indices[count++] = index
        }
        return ArrayUtils.removeAll(array, *indices.copyOf(count))
    }
}
