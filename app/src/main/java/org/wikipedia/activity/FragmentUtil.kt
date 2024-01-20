package org.wikipedia.activity

import androidx.fragment.app.Fragment

@Suppress("UNCHECKED_CAST")
object FragmentUtil {
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

    fun <T : Fragment> getAncestor(fragment: Fragment, ancestor: Class<T>): T? {
        if (fragment.parentFragment == null) {
            return null
        }
        return if (ancestor.isInstance(fragment.parentFragment)) {
            fragment.parentFragment as T
        } else {
            getAncestor(fragment.parentFragment as Fragment, ancestor)
        }
    }
}
