package org.wikipedia.analytics.eventplatform

import android.view.View
import androidx.core.view.allViews
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.feed.view.ListCardItemView
import org.wikipedia.feed.view.ListCardView
import org.wikipedia.onboarding.InitialOnboardingActivity
import org.wikipedia.onboarding.InitialOnboardingFragment.OnboardingPage

object BreadCrumbViewUtil {
    fun getLogNameForView(view: View?): String {
        var logString = ""

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
                return view.context?.getString(R.string.breadcrumb_view_click, view.context.getString(R.string.breadcrumb_view_with_position, currentParent.javaClass.simpleName, position)).orEmpty()
            }
            logString =
                view.context?.getString(R.string.breadcrumb_view_with_position, getLogNameForView(view.parent as RecyclerView), position).orEmpty()
            return logString
        }
        logString =
            if (view?.id == null || view.id == View.NO_ID) "no-id" else view.context?.getString(R.string.breadcrumb_view_click, view.resources.getResourceEntryName(view.id)).orEmpty()
        return logString
    }

    fun getReadableScreenName(activity: BaseActivity, view: View?): String {
        if (activity is InitialOnboardingActivity) {
            return getInitialOnboardingScreenNames(activity.javaClass.simpleName, view)
        }
        return activity.javaClass.simpleName
    }

    fun getReadableScreenName(activity: BaseActivity, isRtl: Boolean): String {
        val visibleFragment: Fragment?
        if (activity is InitialOnboardingActivity) {
            visibleFragment = getVisibleFragment(activity)
            visibleFragment?.view?.rootView?.allViews?.forEach {
                if (it is ViewPager2) {
                    return activity.javaClass.simpleName + OnboardingPage.of(if (isRtl) {
                        if (it.currentItem >= 1) it.currentItem - 1 else it.currentItem
                    } else {
                        if (it.currentItem < it.size - 1) it.currentItem + 1 else it.currentItem
                    })
                }
            }
        }

        return activity.javaClass.simpleName
    }

    private fun getInitialOnboardingScreenNames(activity: String, view: View?): String {
        var currentParent = view?.parent
        while (currentParent !is ViewPager2) {
            if (currentParent != null) {
                currentParent = currentParent.parent
            } else {
                return activity.javaClass.simpleName
            }
        }
        return activity + OnboardingPage.of(currentParent.currentItem)
    }

    private fun getVisibleFragment(activity: InitialOnboardingActivity): Fragment? {
        val fragmentManager = activity.supportFragmentManager
        val fragments: List<Fragment> = fragmentManager.getFragments()
        for (fragment in fragments) {
            if (fragment.isVisible) return fragment
        }
        return null
    }
}
