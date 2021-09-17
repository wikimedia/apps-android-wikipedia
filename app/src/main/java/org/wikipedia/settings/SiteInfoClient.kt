package org.wikipedia.settings

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.util.log.L

object SiteInfoClient {
    private val SITE_INFO_MAP = mutableMapOf<String, SiteInfo?>()

    @JvmStatic
    fun getMainPageForLang(lang: String): String {
        getSiteInfoForLang(lang)?.let {
            if (!it.mainpage.isNullOrEmpty()) {
                return it.mainpage
            }
        }
        return MainPageNameData.valueFor(lang)
    }

    @JvmStatic
    val maxPagesPerReadingList: Int
        get() {
            val info = getSiteInfoForLang(WikipediaApp.instance.wikiSite.languageCode)
            return if (info?.readingListsConfig != null && info.readingListsConfig.maxEntriesPerList > 0) {
                info.readingListsConfig.maxEntriesPerList
            } else Constants.MAX_READING_LIST_ARTICLE_LIMIT
        }

    private fun getSiteInfoForLang(lang: String): SiteInfo? {
        return if (SITE_INFO_MAP.containsKey(lang)) {
            SITE_INFO_MAP[lang]
        } else null
    }

    @JvmStatic
    fun updateFor(wiki: WikiSite) {
        if (SITE_INFO_MAP.containsKey(wiki.languageCode)) {
            return
        }
        ServiceFactory.get(wiki).siteInfo
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response -> SITE_INFO_MAP[wiki.languageCode] = response.query?.siteInfo }) { caught -> L.d(caught) }
    }
}
