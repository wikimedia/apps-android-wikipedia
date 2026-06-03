package org.wikipedia.feed.onthisday

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.settings.homefeed.CommunityModuleType
import org.wikipedia.util.DateUtil
import java.util.Calendar
import java.util.concurrent.TimeUnit

class OnThisDayCard(val events: List<OnThisDay.Event>, val age: Int, wiki: WikiSite) : WikiSiteCard(wiki) {
    private val date: Calendar = DateUtil.getDefaultDateFor(age)

    override fun moduleKey(): String {
        return CommunityModuleType.ON_THIS_DAY.name
    }

    override fun dismissHashCode(): Int {
        return TimeUnit.MILLISECONDS.toDays(date.time.time).toInt() + wikiSite().hashCode() + moduleKey().hashCode()
    }
}
