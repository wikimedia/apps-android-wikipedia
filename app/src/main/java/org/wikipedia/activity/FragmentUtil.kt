package org.wikipedia.activity

import androidx.fragment.app.Fragment

object FragmentUtil {
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T> getCallback(fragment: Fragment, callback: Class<T>): T? {
        if (callback.isInstance(fragment.targetFragment)) {
            return fragment.targetFragment as T?
        }
        if (fragment.parentFragment != null) {
            if (callback.isInstance(fragment.parentFragment)) {
                return fragment.parentFragment as T?
            } else if (callback.isInstance(fragment.requireParentFragment().parentFragment)) {
                return fragment.requireParentFragment().parentFragment as T?
            }
        }
        return if (callback.isInstance(fragment.activity)) {
            fragment.activity as T?
        } else null
    }
}
