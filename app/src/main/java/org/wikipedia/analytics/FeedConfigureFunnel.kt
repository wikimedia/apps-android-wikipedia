package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs

class FeedConfigureFunnel(app: WikipediaApp, wiki: WikiSite?, private val source: Int) :
        TimedFunnel(app, SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL, wiki) {

    override fun preprocessSessionToken(eventData: JSONObject) {}

    fun done(orderedContentTypes: List<FeedContentType>) {
        val orderedList = orderedContentTypes.map { it.code() }
        val enabledList = FeedContentType.values().map { if (it.isEnabled) 1 else 0 }
        val enabledMap = app.language().appLanguageCodes.associateWith { enabledList }
        log(
                "source", source,
                "feed_views", Prefs.exploreFeedVisitCount,
                "enabled_list", JsonUtil.encodeToString(enabledMap),
                "order_list", JsonUtil.encodeToString(orderedList),
                "languages", JsonUtil.encodeToString(app.language().appLanguageCodes)
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppFeedConfigure"
        private const val REV_ID = 20298570
    }
}
