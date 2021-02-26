package org.wikipedia.navtab

import android.view.View
import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.feed.FeedFragment
import org.wikipedia.history.HistoryFragment
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap
import org.wikipedia.readinglist.ReadingListsFragment
import org.wikipedia.suggestededits.SuggestedEditsTasksFragment

enum class NavTab constructor(private val text: Int, private val id: Int, private val icon: Int) : EnumCode {
    EXPLORE(R.string.nav_item_feed, View.generateViewId(), R.drawable.ic_globe) {
        override fun newInstance(): Fragment {
            return FeedFragment.newInstance()
        }
    },
    READING_LISTS(R.string.nav_item_saved, View.generateViewId(), R.drawable.ic_bookmark_white_24dp) {
        override fun newInstance(): Fragment {
            return ReadingListsFragment.newInstance()
        }
    },
    SEARCH(R.string.nav_item_search, View.generateViewId(), R.drawable.ic_search_themed_24dp) {
        override fun newInstance(): Fragment {
            return HistoryFragment.newInstance()
        }
    },
    EDITS(R.string.nav_item_suggested_edits, View.generateViewId(), R.drawable.ic_mode_edit_themed_24dp) {
        override fun newInstance(): Fragment {
            return SuggestedEditsTasksFragment.newInstance()
        }
    };

    fun text(): Int {
        return text
    }

    fun icon(): Int {
        return icon
    }

    fun id(): Int {
        return id
    }

    abstract fun newInstance(): Fragment

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    companion object {

        private val MAP = EnumCodeMap(NavTab::class.java)

        @JvmStatic
        fun of(code: Int): NavTab {
            return MAP[code]
        }

        fun size(): Int {
            return MAP.size()
        }
    }
}
