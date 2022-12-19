package org.wikipedia.analytics.eventplatform

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.feed.view.ListCardItemView
import org.wikipedia.main.MainActivity
import org.wikipedia.main.MainFragment
import org.wikipedia.navtab.NavTab
import org.wikipedia.onboarding.InitialOnboardingActivity
import org.wikipedia.onboarding.InitialOnboardingFragment
import org.wikipedia.onboarding.InitialOnboardingFragment.OnboardingPage

object BreadCrumbViewUtil {
    private const val VIEW_UNNAMED = "unnamed"

    fun getReadableNameForView(view: View): String {
        if (view.parent is RecyclerView) {
            val position = (view.parent as RecyclerView).findContainingViewHolder(view)?.bindingAdapterPosition ?: 0
            if (view is ListCardItemView) {
              return BreadCrumbCustomLogHelper.getCustomNameForListItemView(view, position)
            }
            if (view.parent is RecyclerView && (view.parent as RecyclerView).id == R.id.languages_list_recycler) {
                return BreadCrumbCustomLogHelper.getCustomNameForLanguageListItem(view)
            }
            // Returning only recyclerview name and click position for non-cardView recyclerViews
            return getReadableNameForView(view.parent as RecyclerView) + "." + position
        }
        return if (view.id == View.NO_ID) {
            if (view is TextView) {
                view.text.toString()
            } else {
                VIEW_UNNAMED
            }
        } else {
            getViewResourceName(view)
        }
    }

    private fun getViewResourceName(view: View): String {
        return try {
            if (view.id == R.id.footerActionButton) {
                return (view as MaterialButton).text.toString()
            }
            view.resources.getResourceEntryName(view.id)
        } catch (e: Exception) {
            VIEW_UNNAMED
        }
    }

    fun getReadableScreenName(context: Context): String {
        val fragName = getCurrentFragmentName(context)
        return context.javaClass.simpleName + (if (fragName.isNotEmpty()) ".$fragName" else "")
    }

    private fun getCurrentFragmentName(context: Context): String {
        val fragment = getVisibleFragment(context)

        return when {
            fragment != null && context is InitialOnboardingActivity -> {
                getInitialOnboardingScreenName(fragment)
            }
            fragment != null && context is MainActivity -> {
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

    private fun getVisibleFragment(context: Context): Fragment? {
        if (context is SingleFragmentActivity<*>) {
            return context.supportFragmentManager.findFragmentById(R.id.fragment_container)
        } else if (context is FragmentActivity) {
            val fragments: List<Fragment> = context.supportFragmentManager.fragments
            for (fragment in fragments) {
                if (fragment.isVisible) return fragment
            }
        }
        return null
    }
}
