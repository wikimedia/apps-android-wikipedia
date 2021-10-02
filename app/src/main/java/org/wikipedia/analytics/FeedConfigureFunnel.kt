package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import java.util.*

class FeedConfigureFunnel(app: WikipediaApp, wiki: WikiSite?, private val source: Int) :
        TimedFunnel(app, SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL, wiki) {

    override fun preprocessSessionToken(eventData: JSONObject) {}

    fun done(orderedContentTypes: List<FeedContentType>) {
        val orderedList = orderedContentTypes.map { it.code() }
        val enabledMap = HashMap<String, List<Int>>()
        for (language in app.appLanguageState.appLanguageCodes) {
            enabledMap[language] = FeedContentType.values().map { if (it.isEnabled) 1 else 0 }
        }
        log(
                "source", source,
                "feed_views", Prefs.exploreFeedVisitCount,
                "enabled_list", JsonUtil.encodeToString(enabledMap),
                "order_list", JsonUtil.encodeToString(orderedList),
                "languages", JsonUtil.encodeToString(app.appLanguageState.appLanguageCodes)
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppFeedConfigure"
        private const val REV_ID = 20298570
    }
}
