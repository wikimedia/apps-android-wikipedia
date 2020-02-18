package org.wikipedia.navtab

import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.lang.ref.WeakReference

class NavTabFragmentPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private var fragmentMap = SparseArray<WeakReference<Fragment>>()

    override fun createFragment(position: Int): Fragment {
        val fragment = NavTab.of(position).newInstance()
        fragmentMap.put(position, WeakReference(fragment))
        return fragment
    }

    override fun getItemCount(): Int {
        return NavTab.size()
    }

    fun getFragmentAt(position: Int): Fragment? {
        return fragmentMap[position]?.get()
    }
}
