package org.wikipedia.analytics.eventplatform

import android.app.Activity
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.feed.view.ListCardItemView
import org.wikipedia.feed.view.ListCardView
import org.wikipedia.main.MainActivity
import org.wikipedia.main.MainFragment
import org.wikipedia.navtab.NavTab
import org.wikipedia.onboarding.InitialOnboardingActivity
import org.wikipedia.onboarding.InitialOnboardingFragment
import org.wikipedia.onboarding.InitialOnboardingFragment.OnboardingPage

object BreadCrumbViewUtil {

    fun getReadableNameForView(view: View): String {
        if (view.parent != null && view.parent is RecyclerView) {
            val position =
                    (view.parent as RecyclerView).getChildViewHolder(view).layoutPosition + 1
            if (view is ListCardItemView) {
                var currentParent = view.parent
                while (currentParent !is ListCardView<*>) {
                    if (currentParent.parent != null) {
                        currentParent = currentParent.parent
                    } else {
                        // ListItemView is not in a CardView
                        return view.context.getString(R.string.breadcrumb_view_with_position, getReadableNameForView(view.parent as RecyclerView), position)
                    }
                }
                return view.context.getString(R.string.breadcrumb_view_with_position, currentParent.javaClass.simpleName, position)
            }
            // Returning only recyclerview name and click position for non-cardView recyclerViews
            return view.context.getString(R.string.breadcrumb_view_with_position, getReadableNameForView(view.parent as RecyclerView), position)
        }
        return if (view.id == View.NO_ID) view.context.getString(R.string.breadcrumb_view_unnamed) else getViewResourceName(view)
    }

    private fun getViewResourceName(view: View): String {
        return try {
            if (view is SwitchCompat) {
                return view.context.getString(R.string.breadcrumb_switch_view_click, view.resources.getResourceEntryName(view.id), if (!view.isChecked) view.context.getString(R.string.breadcrumb_switch_view_state_on) else view.context.getString(R.string.breadcrumb_switch_view_state_off))
            }
            if (view.id == R.id.footerActionButton) {
                return (view as MaterialButton).text.toString()
            }
            view.resources.getResourceEntryName(view.id)
        } catch (e: Exception) {
            view.context.getString(R.string.breadcrumb_view_unnamed)
        }
    }

    fun getReadableScreenName(activity: Activity): String {
        return activity.getString(R.string.breadcrumb_screen_fragment_name, activity.javaClass.simpleName, getFragmentName(activity))
    }

    private fun getFragmentName(activity: Activity): String {
        val fragment = getVisibleFragment(activity)

        return when {
            fragment != null && activity is InitialOnboardingActivity -> {
                getInitialOnboardingScreenName(fragment)
            }
            fragment != null && activity is MainActivity -> {
                getMainFragmentTabName(fragment)
            }
            fragment != null -> {
                return fragment.javaClass.simpleName
            }
            else -> return ""
        }
    }

    private fun getMainFragmentTabName(fragment: Fragment): String {
        if (fragment is MainFragment) {
            val mainFragmentPager: ViewPager2? = fragment.view?.findViewById(R.id.main_view_pager)
            return if (mainFragmentPager == null) {
                fragment.javaClass.simpleName
            } else {
                NavTab.of(mainFragmentPager.currentItem).name
            }
        }
        return ""
    }

    private fun getInitialOnboardingScreenName(fragment: Fragment): String {
        if (fragment is InitialOnboardingFragment) {
            val onboardingFragmentPager: ViewPager2? =
                fragment.view?.findViewById(R.id.fragment_pager)
            return if (onboardingFragmentPager == null) {
                fragment.javaClass.simpleName
            } else {
                OnboardingPage.of(onboardingFragmentPager.currentItem).name
            }
        }
        return ""
    }

    private fun getVisibleFragment(activity: Activity): Fragment? {
        if (activity is SingleFragmentActivity<*>) {
            return activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
        } else if (activity is FragmentActivity) {
            val fragments: List<Fragment> = activity.supportFragmentManager.fragments
            for (fragment in fragments) {
                if (fragment.isVisible) return fragment
            }
        }
        return null
    }
}
