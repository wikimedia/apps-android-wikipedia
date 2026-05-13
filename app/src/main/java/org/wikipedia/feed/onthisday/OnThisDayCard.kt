package org.wikipedia.feed.onthisday

import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.settings.homefeed.CommunityModuleType
import org.wikipedia.util.DateUtil
import org.wikipedia.util.L10nUtil
import java.util.Calendar
import java.util.concurrent.TimeUnit

class OnThisDayCard(val events: List<OnThisDay.Event>, val age: Int, wiki: WikiSite) : WikiSiteCard(wiki) {
    private val date: Calendar = DateUtil.getDefaultDateFor(age)
    var callback: FeedAdapter.Callback? = null

    override fun moduleKey(): String {
        return CommunityModuleType.ON_THIS_DAY.name
    }

    override fun type(): CardType {
        return CardType.ON_THIS_DAY
    }

    override fun title(): String {
        return L10nUtil.getString(wikiSite().languageCode, R.string.on_this_day_card_title)
    }

    override fun subtitle(): String {
        return DateUtil.getFeedCardShortDateString(date)
    }

    override fun dismissHashCode(): Int {
        return TimeUnit.MILLISECONDS.toDays(date.time.time).toInt() + wikiSite().hashCode()
    }

    fun footerActionText(): String {
        return L10nUtil.getString(wikiSite().languageCode, R.string.more_events_text)
    }

    fun text(): CharSequence {
        return events.first().text
    }

    fun year(): Int {
        return events.first().year
    }

    fun date(): Calendar {
        return date
    }

    fun pages(): List<PageSummary> {
        return events.first().pages
    }
}
