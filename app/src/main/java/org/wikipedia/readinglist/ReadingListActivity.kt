package org.wikipedia.readinglist

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.eventplatform.ReadingListsAnalyticsHelper
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.ResourceUtil

class ReadingListActivity : SingleFragmentActivity<ReadingListFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateStatusBarColor(false)
        title = getString(R.string.reading_list_activity_title, intent.getStringExtra(EXTRA_READING_LIST_TITLE))
    }

    public override fun createFragment(): ReadingListFragment {
        val isPreview = intent.getBooleanExtra(EXTRA_READING_LIST_PREVIEW, false)
        return if (isPreview) {
            ReadingListFragment.newInstance(true)
        } else {
            ReadingListFragment.newInstance(intent.getLongExtra(EXTRA_READING_LIST_ID, 0))
        }
    }

    fun updateStatusBarColor(inActionMode: Boolean) {
        setStatusBarColor(if (!inActionMode) Color.TRANSPARENT else ResourceUtil.getThemedColor(this, R.attr.paper_color))
    }

    fun updateNavigationBarColor() {
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (intent.getBooleanExtra(EXTRA_READING_LIST_PREVIEW, false)) {
            ReadingListsAnalyticsHelper.logReceiveCancel(this, fragment.readingList)
        }
    }

    companion object {
        private const val EXTRA_READING_LIST_TITLE = "readingListTitle"
        const val EXTRA_READING_LIST_ID = "readingListId"
        const val EXTRA_READING_LIST_PREVIEW = "previewReadingList"

        fun newIntent(context: Context, list: ReadingList): Intent {
            return Intent(context, ReadingListActivity::class.java)
                    .putExtra(EXTRA_READING_LIST_TITLE, list.title)
                    .putExtra(EXTRA_READING_LIST_ID, list.id)
        }

        fun newIntent(context: Context, preview: Boolean): Intent {
            return Intent(context, ReadingListActivity::class.java)
                .putExtra(EXTRA_READING_LIST_PREVIEW, preview)
        }
    }
}
