package org.wikipedia.navtab

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.feed.FeedFragment
import org.wikipedia.history.HistoryFragment
import org.wikipedia.model.EnumCode
import org.wikipedia.readinglist.ReadingListsFragment
import org.wikipedia.suggestededits.SuggestedEditsTasksFragment
import org.wikipedia.usercontrib.ContributionsDashboardHelper

enum class NavTab constructor(
    @StringRes val text: Int,
    val id: Int,
    @DrawableRes val icon: Int,
    ) : EnumCode {

    EXPLORE(R.string.feed, R.id.nav_tab_explore, R.drawable.selector_nav_explore) {
        override fun newInstance(): Fragment {
            return FeedFragment.newInstance()
        }
    },
    READING_LISTS(R.string.nav_item_saved, R.id.nav_tab_reading_lists, R.drawable.selector_nav_saved) {
        override fun newInstance(): Fragment {
            return ReadingListsFragment.newInstance()
        }
    },
    SEARCH(R.string.nav_item_search, R.id.nav_tab_search, R.drawable.selector_nav_search) {
        override fun newInstance(): Fragment {
            return HistoryFragment.newInstance()
        }
    },
    EDITS(R.string.nav_item_suggested_edits, R.id.nav_tab_edits, R.drawable.selector_nav_edits) {
        override fun newInstance(): Fragment {
            return SuggestedEditsTasksFragment.newInstance()
        }
    },
    CONTRIBUTE(R.string.nav_item_contribute, R.id.nav_tab_contribute, R.drawable.selector_nav_edits) {
        override fun newInstance(): Fragment {
            return SuggestedEditsTasksFragment.newInstance()
        }

        override fun ordinalIndex(): Int = 3
    },
    MORE(R.string.nav_item_more, R.id.nav_tab_more, R.drawable.ic_menu_white_24dp) {
        override fun newInstance(): Fragment {
            return Fragment()
        }

        override fun ordinalIndex(): Int = 4
    };

    open fun ordinalIndex(): Int = ordinal

    abstract fun newInstance(): Fragment

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinalIndex()
    }

    companion object {

        fun getTabs(): List<NavTab> {
            return listOf(
                EXPLORE,
                READING_LISTS,
                SEARCH,
                if (ContributionsDashboardHelper.contributionsDashboardEnabled) CONTRIBUTE else EDITS,
                MORE
            )
        }

        fun of(code: Int): NavTab {
            return getTabs()[code]
        }
    }

}
