package org.wikipedia.util;

import java.util.Arrays;

import static org.apache.commons.lang3.ArrayUtils.INDEX_NOT_FOUND;
import static org.apache.commons.lang3.ArrayUtils.indexOf;
import static org.apache.commons.lang3.ArrayUtils.removeAll;

// Some OEMs appear to have added a pre-v3.5, possibly re-v3.4, version of ArrayUtils to the system
// path which does not contain removeAllOccurences(). This class is a partial copy of Apache
// Commons' ArrayUtils
// https://rink.hockeyapp.net/manage/apps/226650/app_versions/79/crash_reasons/156658637
public final class ArrayUtils {
    /**
     * Removes the occurrences of the specified element from the specified array.
     *
     * <p>
     * All subsequent elements are shifted to the left (subtracts one from their indices).
     * If the array doesn't contains such an element, no elements are removed from the array.
     * <code>null</code> will be returned if the input array is <code>null</code>.
     * </p>
     *
     * @param <T> the type of object in the array
     * @param element the element to remove
     * @param array the input array
     *
     * @return A new array containing the existing elements except the occurrences of the specified element.
     * @since 3.5
     */
    public static <T> T[] removeAllOccurences(final T[] array, final T element) {
        int index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) {
            return org.apache.commons.lang3.ArrayUtils.clone(array);
        }

        int[] indices = new int[array.length - index];
        indices[0] = index;
        int count = 1;

        while ((index = indexOf(array, element, indices[count - 1] + 1)) != INDEX_NOT_FOUND) {
            indices[count++] = index;
        }

        return removeAll(array, Arrays.copyOf(indices, count));
    }

    private ArrayUtils() { }
}
