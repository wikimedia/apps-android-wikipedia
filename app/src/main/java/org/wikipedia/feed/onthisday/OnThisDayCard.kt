package org.wikipedia.feed.onthisday

import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.util.DateUtil
import org.wikipedia.util.L10nUtil
import java.util.*
import java.util.concurrent.TimeUnit

class OnThisDayCard(events: List<OnThisDay.Event>, wiki: WikiSite, val age: Int) : WikiSiteCard(wiki) {
    private val nextYear: Int
    private val date: Calendar = DateUtil.getDefaultDateFor(age)
    private val eventShownOnCard: OnThisDay.Event
    var callback: FeedAdapter.Callback? = null

    init {
        var randomIndex = 0
        if (events.size > 1) {
            randomIndex = Random().nextInt(events.size - 1)
        }
        eventShownOnCard = events[randomIndex]
        nextYear = if (randomIndex + 1 < events.size) events[randomIndex + 1].year else eventShownOnCard.year
    }

    override fun type(): CardType {
        return CardType.ON_THIS_DAY
    }

    override fun title(): String {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode, R.string.on_this_day_card_title)
    }

    override fun subtitle(): String {
        return DateUtil.getFeedCardShortDateString(date)
    }

    override fun dismissHashCode(): Int {
        return TimeUnit.MILLISECONDS.toDays(date.time.time).toInt() + wikiSite().hashCode()
    }

    fun footerActionText(): String {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode, R.string.more_events_text)
    }

    fun text(): CharSequence {
        return eventShownOnCard.text
    }

    fun year(): Int {
        return eventShownOnCard.year
    }

    fun date(): Calendar {
        return date
    }

    fun pages(): List<PageSummary>? {
        return eventShownOnCard.pages
    }
}
