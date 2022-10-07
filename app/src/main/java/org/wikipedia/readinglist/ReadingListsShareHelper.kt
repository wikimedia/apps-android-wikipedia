package org.wikipedia.readinglist

import android.content.Intent
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
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
            val url = WikipediaApp.instance.wikiSite.url() + "/wiki/Special:ReadingLists?limport=$param"

            val finalUrl = if (Prefs.useUrlShortenerForSharing) ServiceFactory.get(WikipediaApp.instance.wikiSite).shortenUrl(url).shortenUrl?.shortUrl.orEmpty() else url

            val intent = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_SUBJECT, readingList.title)
                    .putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.reading_list_share_message, readingList.title) + " " + finalUrl)
                    .setType("text/plain")
            activity.startActivity(intent)
        }
    }

    private fun readingListToUrlParam(readingList: ReadingList, pageIdMap: Map<String, Map<String, Int>>): String {
        val str = StringBuilder()
        str.append("{")

        // TODO: for now we're not transmitting the free-form Name and Description of a reading list.
        // str.append("\"name\":${UriUtil.encodeURL(readingList.title)},")
        // str.append("\"description\":${UriUtil.encodeURL(readingList.description.orEmpty())},")

        str.append("\"list\":{")
        var first = true
        pageIdMap.keys.forEach { lang ->
            if (!first) str.append(",")
            first = false
            str.append("\"$lang\":[")
            val pageIds = pageIdMap[lang].orEmpty().values
            str.append(pageIds.joinToString(","))
            str.append("]")
        }
        str.append("}") // list
        str.append("}") // root
        return Base64.encodeToString(str.toString().toByteArray(), Base64.NO_WRAP)
    }
}
