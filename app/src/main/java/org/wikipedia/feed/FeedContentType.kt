package org.wikipedia.feed

import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.feed.accessibility.AccessibilityCardClient
import org.wikipedia.feed.aggregated.AggregatedFeedContentClient
import org.wikipedia.feed.becauseyouread.BecauseYouReadClient
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.mainpage.MainPageClient
import org.wikipedia.feed.random.RandomClient
import org.wikipedia.feed.suggestededits.SuggestedEditsFeedClient
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil

enum class FeedContentType(private val code: Int,
                           @StringRes val titleId: Int,
                           @StringRes val subtitleId: Int,
                           val isPerLanguage: Boolean,
                           var showInConfig: Boolean = true) : EnumCode {

    FEATURED_ARTICLE(6, R.string.view_featured_article_card_title, R.string.feed_item_type_featured_article, true) {
        override fun newClient(aggregatedClient: AggregatedFeedContentClient, age: Int): FeedClient? {
            return if (isEnabled) AggregatedFeedContentClient.FeaturedArticle(aggregatedClient) else null
        }
    },
    TOP_READ_ARTICLES(3, R.string.view_top_read_card_title, R.string.feed_item_type_trending, true) {
        override fun newClient(aggregatedClient: AggregatedFeedContentClient, age: Int): FeedClient? {
            return if (isEnabled) AggregatedFeedContentClient.TopReadArticles(aggregatedClient) else null
        }
    },
    FEATURED_IMAGE(7, R.string.view_featured_image_card_title, R.string.feed_item_type_featured_image, false) {
        override fun newClient(aggregatedClient: AggregatedFeedContentClient, age: Int): FeedClient? {
            return if (isEnabled) AggregatedFeedContentClient.FeaturedImage(aggregatedClient) else null
        }
    },
    BECAUSE_YOU_READ(8, R.string.view_because_you_read_card_title, R.string.feed_item_type_because_you_read, false) {
        override fun newClient(aggregatedClient: AggregatedFeedContentClient, age: Int): FeedClient? {
            return if (isEnabled) BecauseYouReadClient() else null
        }
    },
    NEWS(0, R.string.view_card_news_title, R.string.feed_item_type_news, true) {
        override fun newClient(aggregatedClient: AggregatedFeedContentClient, age: Int): FeedClient? {
            return if (isEnabled && age == 0) AggregatedFeedContentClient.InTheNews(aggregatedClient) else null
        }
    },
    ON_THIS_DAY(1, R.string.on_this_day_card_title, R.string.feed_item_type_on_this_day, true) {
        override fun newClient(aggregatedClient: AggregatedFeedContentClient, age: Int): FeedClient? {
            return if (isEnabled) AggregatedFeedContentClient.OnThisDayFeed(aggregatedClient) else null
        }
    },
    RANDOM(5, R.string.view_random_card_title, R.string.feed_item_type_randomizer, true) {
        override fun newClient(aggregatedClient: AggregatedFeedContentClient, age: Int): FeedClient? {
            return if (isEnabled) RandomClient() else null
        }
    },
    MAIN_PAGE(4, R.string.view_main_page_card_title, R.string.feed_item_type_main_page, true) {
        override fun newClient(aggregatedClient: AggregatedFeedContentClient, age: Int): FeedClient? {
            return if (isEnabled && age == 0) MainPageClient() else null
        }
    },
    SUGGESTED_EDITS(9, R.string.suggested_edits_feed_card_title, R.string.feed_item_type_suggested_edits, false) {
        override fun newClient(aggregatedClient: AggregatedFeedContentClient, age: Int): FeedClient? {
            return if (isEnabled && AccountUtil.isLoggedIn && WikipediaApp.instance.isOnline) SuggestedEditsFeedClient() else null
        }
    },
    ACCESSIBILITY(10, 0, 0, false, false) {
        override fun newClient(aggregatedClient: AggregatedFeedContentClient, age: Int): FeedClient? {
            return if (DeviceUtil.isAccessibilityEnabled) AccessibilityCardClient() else null
        }
    };

    var order = code
    var isEnabled = true
    val langCodesSupported = mutableListOf<String>()
    val langCodesDisabled = mutableListOf<String>()

    abstract fun newClient(aggregatedClient: AggregatedFeedContentClient, age: Int): FeedClient?

    override fun code(): Int {
        return code
    }

    companion object {
        private val MAP = EnumCodeMap(FeedContentType::class.java)

        fun of(code: Int): FeedContentType { return MAP[code] }

        @JvmStatic
        val aggregatedLanguages: List<String>
            get() {
                val appLangCodes = WikipediaApp.instance.appLanguageState.appLanguageCodes
                val list = mutableListOf<String>()
                values().filter { it.isEnabled }.forEach { type ->
                    list.addAll(appLangCodes.filter {
                        (type.langCodesSupported.isEmpty() || type.langCodesSupported.contains(it)) &&
                                !type.langCodesDisabled.contains(it) && !list.contains(it)
                    })
                }
                return list
            }

        fun saveState() {
            val enabledList = mutableListOf<Boolean>()
            val orderList = mutableListOf<Int>()
            val langSupportedMap = mutableMapOf<Int, List<String>>()
            val langDisabledMap = mutableMapOf<Int, List<String>>()
            values().forEach {
                enabledList.add(it.isEnabled)
                orderList.add(it.order)
                langSupportedMap[it.code] = it.langCodesSupported
                langDisabledMap[it.code] = it.langCodesDisabled
            }
            Prefs.feedCardsEnabled = enabledList
            Prefs.feedCardsOrder = orderList
            Prefs.setFeedCardsLangSupported(langSupportedMap)
            Prefs.setFeedCardsLangDisabled(langDisabledMap)
        }

        fun restoreState() {
            val enabledList = Prefs.feedCardsEnabled
            val orderList = Prefs.feedCardsOrder
            val langSupportedMap = Prefs.feedCardsLangSupported
            val langDisabledMap = Prefs.feedCardsLangDisabled
            values().forEachIndexed { i, type ->
                type.isEnabled = if (i < enabledList.size) enabledList[i] else true
                type.order = if (i < orderList.size) orderList[i] else i
                type.langCodesSupported.clear()
                langSupportedMap[type.code]?.let {
                    type.langCodesSupported.addAll(it)
                }
                type.langCodesDisabled.clear()
                langDisabledMap[type.code]?.let {
                    type.langCodesDisabled.addAll(it)
                }
            }
        }
    }
}
