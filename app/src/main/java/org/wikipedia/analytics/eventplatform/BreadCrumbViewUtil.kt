package org.wikipedia.analytics.eventplatform

import android.content.res.Resources
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
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

    fun getLogNameForView(view: View?): String {
        if (view?.parent != null && view.parent is RecyclerView) {
            val position = (view.parent as RecyclerView).getChildViewHolder(view).layoutPosition + 1
            if (view is ListCardItemView) {
                var currentParent = view.parent
                while (currentParent !is ListCardView<*>) {
                    if (currentParent.parent != null) {
                        currentParent = currentParent.parent
                    } else {
                        return view.context?.getString(R.string.breadcrumb_view_with_position, getLogNameForView(view.parent as RecyclerView), position).orEmpty()
                    }
                }
                return view.context?.getString(R.string.breadcrumb_view_with_position, currentParent.javaClass.simpleName, position).orEmpty()
            }
            return view.context?.getString(R.string.breadcrumb_view_with_position, getLogNameForView(view.parent as RecyclerView), position).orEmpty()
        }
        return view?.let { if (it.id == View.NO_ID) "no-id" else getViewResourceName(it) }.orEmpty()
    }

    private fun getViewResourceName(view: View): String {
        return try {
            if (view is SwitchCompat) {
                return view.context.getString(R.string.breadcrumb_switch_view_click, view.resources.getResourceEntryName(view.id), if (!view.isChecked) view.context.getString(R.string.breadcrumb_switch_view_state_on) else view.context.getString(R.string.breadcrumb_switch_view_state_off))
            }
            view.resources.getResourceEntryName(view.id)
        } catch (e: Resources.NotFoundException) {
            "unnamed view"
        } catch (e: Exception) {
            ""
        }
    }

    fun getReadableScreenName(activity: BaseActivity): String {
        return activity.getString(R.string.breadcrumb_screen_fragment_name, activity.javaClass.simpleName, getFragmentName(activity))
    }

    private fun getFragmentName(activity: BaseActivity): String {
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

    private fun getVisibleFragment(activity: BaseActivity): Fragment? {
        val fragmentManager = activity.supportFragmentManager

        if (activity is SingleFragmentActivity<*>) {
            return fragmentManager.findFragmentById(R.id.fragment_container)
        } else {
            val fragments: List<Fragment> = fragmentManager.fragments
            for (fragment in fragments) {
                if (fragment.isVisible) return fragment
            }
        }
        return null
    }
}
