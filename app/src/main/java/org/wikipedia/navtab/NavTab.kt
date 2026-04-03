package org.wikipedia.navtab

import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.activitytab.ActivityTabFragment
import org.wikipedia.feed.HomeFragment
import org.wikipedia.history.HistoryFragment
import org.wikipedia.model.EnumCode
import org.wikipedia.readinglist.ReadingListsFragment

enum class NavTab(val text: Int, val id: Int, val icon: Int) : EnumCode {

    HOME(R.string.home, R.id.nav_tab_home, R.drawable.ic_home_filled_24dp) {
        override fun newInstance(): Fragment {
            return HomeFragment() // FeedFragment.newInstance()
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
