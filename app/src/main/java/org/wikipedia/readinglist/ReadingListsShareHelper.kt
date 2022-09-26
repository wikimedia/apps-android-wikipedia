package org.wikipedia.readinglist

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L

object ReadingListsShareHelper {

    fun shareReadingList(activity: AppCompatActivity, readingList: ReadingList?) {
        if (readingList == null) {
            return
        }
        activity.lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            FeedbackUtil.showError(activity, throwable)
        }) {
            val wikiPageTitlesMap = mutableMapOf<String, MutableList<String>>()

            readingList.pages.forEach {
                wikiPageTitlesMap.getOrPut(it.lang) { mutableListOf() }.add(it.apiTitle)
            }

            val wikiPageIdsMap = mutableMapOf<String, MutableMap<String, Int>>()

            wikiPageTitlesMap.keys.forEach { wikiLang ->
                val titleList = wikiPageTitlesMap[wikiLang].orEmpty()
                val pages = ServiceFactory.get(WikiSite.forLanguageCode(wikiLang)).getPageIds(titleList.joinToString("|")).query?.pages!!
                pages.forEach { page ->
                    wikiPageIdsMap.getOrPut(wikiLang) { mutableMapOf() }[StringUtil.addUnderscores(page.title)] = page.pageId
                }
            }

            val param = readingListToUrlParam(readingList, wikiPageIdsMap)
            val url = "https://${WikipediaApp.instance.wikiSite.dbName()}.wikipedia.org/wiki/Special:ReadingLists?list=$param"

            val shortUrl = ServiceFactory.get(WikipediaApp.instance.wikiSite).shortenUrl(url).shortenUrl?.shortUrl.orEmpty()

            val intent = Intent(Intent.ACTION_SEND)
                    // .putExtra(Intent.EXTRA_SUBJECT, "Reading list: " + readingList.title)
                    .putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.reading_list_share_message, readingList.title) + ":" + shortUrl)
                    .setType("text/plain")
            activity.startActivity(intent)
        }
    }

    private fun readingListToUrlParam(readingList: ReadingList, pageIdMap: Map<String, Map<String, Int>>): String {
        val str = StringBuilder()
        str.append(UriUtil.encodeURL(readingList.title))
        str.append("|")
        str.append(UriUtil.encodeURL(readingList.description.orEmpty()))

        val totalPageIdList = pageIdMap.values.flatMap { it.values }.toMutableList()

        readingList.pages.forEach { page ->
            pageIdMap[page.lang]?.get(page.apiTitle)?.let {
                str.append("|")
                str.append(page.lang)
                str.append(":")
                str.append(it)
                totalPageIdList.remove(it)
            }
        }
        totalPageIdList.forEach { id ->
            var lang: String? = null
            pageIdMap.keys.forEach { key ->
                if (pageIdMap[key]?.containsValue(id) == true) {
                    lang = key
                }
            }
            if (lang != null) {
                str.append("|")
                str.append(lang)
                str.append(":")
                str.append(id)
            }
        }
        return str.toString()
    }
}
