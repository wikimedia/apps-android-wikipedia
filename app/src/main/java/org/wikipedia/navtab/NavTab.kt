package org.wikipedia.navtab

import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.activitytab.ActivityTabFragment
import org.wikipedia.feed.HomeFragment
import org.wikipedia.history.HistoryFragment
import org.wikipedia.model.EnumCode
import org.wikipedia.readinglist.ReadingListsFragment

enum class NavTab(val text: Int, val icon: Int, val selectedIcon: Int = icon) : EnumCode {
    HOME(R.string.home, R.drawable.ic_home_filled_24dp) {
        override fun newInstance(): Fragment {
            return HomeFragment() // FeedFragment.newInstance()
        }
    },
    READING_LISTS(R.string.nav_item_saved, R.drawable.ic_bookmark_border_white_24dp, R.drawable.ic_bookmark_white_24dp) {
        override fun newInstance(): Fragment {
            return ReadingListsFragment.newInstance()
        }
    },
    SEARCH(R.string.nav_item_search, R.drawable.ic_search_white_24dp, R.drawable.search_bold) {
        override fun newInstance(): Fragment {
            return HistoryFragment.newInstance()
        }
    },
    EDITS(R.string.nav_item_activity, R.drawable.outline_activity_24, R.drawable.outline_activity_24) {
        override fun newInstance(): Fragment {
            return ActivityTabFragment.newInstance()
        }
    },
    MORE(R.string.nav_item_more, R.drawable.ic_menu_white_24dp) {
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
