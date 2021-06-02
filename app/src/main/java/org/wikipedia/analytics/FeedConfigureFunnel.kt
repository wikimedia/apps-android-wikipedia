package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import java.util.*

class FeedConfigureFunnel(app: WikipediaApp, wiki: WikiSite?, private val source: Int) :
        TimedFunnel(app, SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL, wiki) {

    override fun preprocessSessionToken(eventData: JSONObject) {}

    fun done(orderedContentTypes: List<FeedContentType>) {
        val orderedList = orderedContentTypes.map { it.code() }
        val enabledMap = HashMap<String, List<Int>>()
        for (language in app.language().appLanguageCodes) {
            enabledMap[language] = FeedContentType.values().map { if (it.isEnabled) 1 else 0 }
        }
        log(
                "source", source,
                "feed_views", Prefs.getExploreFeedVisitCount(),
                "enabled_list", StringUtil.stringToListMapToJSONString(enabledMap),
                "order_list", StringUtil.listToJSONString(orderedList),
                "languages", StringUtil.listToJsonArrayString(app.language().appLanguageCodes)
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppFeedConfigure"
        private const val REV_ID = 20298570
    }
}
