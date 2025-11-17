package org.wikipedia.navtab

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.activitytab.ActivityTabFragment
import org.wikipedia.feed.FeedFragment
import org.wikipedia.history.HistoryFragment
import org.wikipedia.model.EnumCode
import org.wikipedia.readinglist.ReadingListsFragment

enum class NavTab(@StringRes val text: Int, val id: Int, @DrawableRes val icon: Int) : EnumCode {

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
    EDITS(R.string.nav_item_activity, R.id.nav_tab_edits, R.drawable.selector_nav_activity) {
        override fun newInstance(): Fragment {
            return ActivityTabFragment.newInstance()
        }
    },
    MORE(R.string.nav_item_more, R.id.nav_tab_more, R.drawable.ic_menu_white_24dp) {
        override fun newInstance(): Fragment {
            return Fragment()
        }
    };

    abstract fun newInstance(): Fragment

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    companion object {
        fun of(code: Int): NavTab {
            return entries[code]
        }
    }
}
