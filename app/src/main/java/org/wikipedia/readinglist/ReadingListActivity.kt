package org.wikipedia.readinglist

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.eventplatform.ReadingListsAnalyticsHelper
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RecommendedReadingListNotificationManager
import org.wikipedia.util.ResourceUtil

class ReadingListActivity : SingleFragmentActivity<ReadingListFragment>(), BaseActivity.Callback {

    private var readingListMode: ReadingListMode = ReadingListMode.DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateStatusBarColor(false)
        title = getString(R.string.reading_list_activity_title, intent.getStringExtra(EXTRA_READING_LIST_TITLE))
        callback = this
    }

    public override fun createFragment(): ReadingListFragment {
        readingListMode = (intent.getSerializableExtra(EXTRA_READING_LIST_MODE) as ReadingListMode?) ?: ReadingListMode.DEFAULT
        return if (readingListMode != ReadingListMode.DEFAULT) {
            ReadingListFragment.newInstance(readingListMode)
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
        if (readingListMode == ReadingListMode.DEFAULT) {
            ReadingListsAnalyticsHelper.logReceiveCancel(this, fragment.readingList)
        }
        if (!WikipediaApp.instance.haveMainActivity) {
            startActivity(MainActivity.newIntent(this)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.INTENT_RETURN_TO_MAIN, true)
                .putExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.READING_LISTS)
            )
            finish()
        }
    }

    override fun onPermissionResult(activity: BaseActivity, isGranted: Boolean) {
        if (isGranted) {
            RecommendedReadingListNotificationManager.scheduleRecommendedReadingListNotification(this)
            Prefs.isRecommendedReadingListNotificationEnabled = true
        } else {
            Prefs.isRecommendedReadingListNotificationEnabled = false
        }
        fragment.updateNotificationIcon()
    }

    companion object {
        private const val EXTRA_READING_LIST_TITLE = "readingListTitle"
        const val EXTRA_READING_LIST_ID = "readingListId"
        const val EXTRA_READING_LIST_MODE = "readingListMode"

        fun newIntent(context: Context, list: ReadingList): Intent {
            return Intent(context, ReadingListActivity::class.java)
                    .putExtra(EXTRA_READING_LIST_TITLE, list.title)
                    .putExtra(EXTRA_READING_LIST_ID, list.id)
        }

        fun newIntent(context: Context, readingListMode: ReadingListMode): Intent {
            return Intent(context, ReadingListActivity::class.java)
                .putExtra(EXTRA_READING_LIST_MODE, readingListMode)
        }
    }
}
