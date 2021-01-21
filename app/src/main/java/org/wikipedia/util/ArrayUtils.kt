package org.wikipedia.util

import org.apache.commons.lang3.ArrayUtils
import java.util.*

// Some OEMs appear to have added a pre-v3.5, possibly re-v3.4, version of ArrayUtils to the system
// path which does not contain removeAllOccurrences(). This class is a partial copy of Apache
// Commons' ArrayUtils
// https://rink.hockeyapp.net/manage/apps/226650/app_versions/79/crash_reasons/156658637
object ArrayUtils {
    /**
     * Removes the occurrences of the specified element from the specified array.
     *
     *
     *
     * All subsequent elements are shifted to the left (subtracts one from their indices).
     * If the array doesn't contains such an element, no elements are removed from the array.
     * `null` will be returned if the input array is `null`.
     *
     *
     * @param <T> the type of object in the array
     * @param element the element to remove
     * @param array the input array
     *
     * @return A new array containing the existing elements except the occurrences of the specified element.
     * @since 3.5
    </T> */
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
        return ArrayUtils.removeAll(array, *Arrays.copyOf(indices, count))
    }
}
