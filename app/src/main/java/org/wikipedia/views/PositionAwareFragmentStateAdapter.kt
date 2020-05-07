package org.wikipedia.views

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter

abstract class PositionAwareFragmentStateAdapter: FragmentStateAdapter {
    private val fragmentManager: FragmentManager
    constructor(fragment: Fragment): super(fragment) { fragmentManager = fragment.childFragmentManager }
    constructor(activity: FragmentActivity): super(activity) { fragmentManager = activity.supportFragmentManager }

    fun getFragmentAt(position: Int): Fragment? {
        // HACK: this is internal implementation-specific logic that is likely to change in the future.
        // TODO: wait until FragmentStateAdapter supports indexing fragments natively.
        return fragmentManager.findFragmentByTag("f$position")
    }

    // TODO: remove until the TransactionTooLargeException when swiping too many pages is resolved.
    fun removeFragmentAt(position: Int) {
        val fragment = getFragmentAt(position)
        if (fragment != null) {
            fragmentManager.beginTransaction().remove(fragment).commit()
        }
    }
}
