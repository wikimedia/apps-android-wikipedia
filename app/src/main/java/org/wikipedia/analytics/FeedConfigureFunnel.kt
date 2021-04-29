package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil.listToJSONString
import org.wikipedia.util.StringUtil.listToJsonArrayString
import org.wikipedia.util.StringUtil.stringToListMapToJSONString
import java.util.*

class FeedConfigureFunnel(app: WikipediaApp, wiki: WikiSite?, private val source: Int) :
        TimedFunnel(app, SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL, wiki) {
    override fun preprocessSessionToken(eventData: JSONObject) {}
    fun done(orderedContentTypes: List<FeedContentType>) {
        var enabledList: MutableList<Int>
        val orderedList: MutableList<Int> = ArrayList()
        val enabledMap: MutableMap<String, List<Int>> = HashMap()
        for (language in app.language().appLanguageCodes) {
            enabledList = ArrayList()
            for (type in FeedContentType.values()) {
                enabledList.add(if (type.isEnabled) 1 else 0)
            }
            enabledMap[language] = enabledList
        }
        for (type in orderedContentTypes) {
            orderedList.add(type.code())
        }
        log(
                "source", source,
                "feed_views", Prefs.getExploreFeedVisitCount(),
                "enabled_list", stringToListMapToJSONString(enabledMap),
                "order_list", listToJSONString(orderedList),
                "languages", listToJsonArrayString(app.language().appLanguageCodes)
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppFeedConfigure"
        private const val REV_ID = 20298570
    }
}
