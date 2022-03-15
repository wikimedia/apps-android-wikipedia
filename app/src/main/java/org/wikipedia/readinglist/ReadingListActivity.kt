package org.wikipedia.readinglist

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.ResourceUtil.getThemedColor

class ReadingListActivity : SingleFragmentActivity<ReadingListFragment>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        title = getString(R.string.reading_list_activity_title, intent.getStringExtra(EXTRA_READING_LIST_TITLE))
    }

    public override fun createFragment(): ReadingListFragment {
        return ReadingListFragment.newInstance(intent.getLongExtra(EXTRA_READING_LIST_ID, 0))
    }

    fun updateNavigationBarColor() {
        setNavigationBarColor(getThemedColor(this, R.attr.paper_color))
    }

    companion object {
        private const val EXTRA_READING_LIST_TITLE = "readingListTitle"
        const val EXTRA_READING_LIST_ID = "readingListId"
        fun newIntent(context: Context, list: ReadingList): Intent {
            return Intent(context, ReadingListActivity::class.java)
                    .putExtra(EXTRA_READING_LIST_TITLE, list.title)
                    .putExtra(EXTRA_READING_LIST_ID, list.id)
        }
    }
}
