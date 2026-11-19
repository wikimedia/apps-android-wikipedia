package org.wikipedia.feed.topread

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.feed.model.TopReadCard
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.util.DateUtil

class TopReadArticlesActivity : SingleFragmentActivity<TopReadFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationCategory = intent.getSerializableExtra(Constants.INTENT_EXTRA_NOTIFICATION_CATEGORY) as NotificationCategory?
        title = if (notificationCategory == NotificationCategory.WEEKLY_MOST_READ) {
            getString(R.string.notifications_experiment_weekly_most_read_notification_title)
        } else {
            val card = intent.parcelableExtra<TopReadCard>(TOP_READ_CARD)
            val dateStr = card?.let { DateUtil.getShortDateString(it.articles.localDate) }
            getString(R.string.top_read_activity_title, dateStr.orEmpty())
        }
    }

    public override fun createFragment(): TopReadFragment {
        return TopReadFragment.newInstance(intent.parcelableExtra(TOP_READ_CARD)!!)
    }

    companion object {
        const val TOP_READ_CARD = "item"
        fun newIntent(context: Context, card: TopReadCard, invokeSource: InvokeSource? = null, notificationCategory: NotificationCategory? = null): Intent {
            return Intent(context, TopReadArticlesActivity::class.java)
                .putExtra(TOP_READ_CARD, card)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
                .putExtra(Constants.INTENT_EXTRA_NOTIFICATION_CATEGORY, notificationCategory)
        }
    }
}
