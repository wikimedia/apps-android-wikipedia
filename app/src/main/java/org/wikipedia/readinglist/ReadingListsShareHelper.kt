package org.wikipedia.readinglist

import android.content.Intent
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.ReadingListsAnalyticsHelper
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

object ReadingListsShareHelper {

    const val API_MAX_SIZE = 50
    const val PROVENANCE_PARAM = "rlsa1"

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
                wikiPageTitlesMap[wikiLang].orEmpty().chunked(API_MAX_SIZE).forEach { list ->
                    ServiceFactory.get(WikiSite.forLanguageCode(wikiLang)).getPageIds(list.joinToString("|")).query?.pages!!.forEach { page ->
                        wikiPageIdsMap.getOrPut(wikiLang) { mutableMapOf() }[StringUtil.addUnderscores(page.title)] = page.pageId
                    }
                }
            }

            val param = readingListToUrlParam(readingList, wikiPageIdsMap)
            val url = WikipediaApp.instance.wikiSite.url() + "/wiki/Special:ReadingLists?limport=$param&wprov=$PROVENANCE_PARAM"

            val finalUrl = if (Prefs.useUrlShortenerForSharing) ServiceFactory.get(WikipediaApp.instance.wikiSite).shortenUrl(url).shortenUrl?.shortUrl.orEmpty() else url

            ReadingListsAnalyticsHelper.logShareList(activity, readingList)

            val intent = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_SUBJECT, readingList.title)
                    .putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.reading_list_share_message_v2) + " " + finalUrl)
                    .setType("text/plain")
            activity.startActivity(intent)
        }
    }

    private fun readingListToUrlParam(readingList: ReadingList, pageIdMap: Map<String, Map<String, Int>>): String {
        val projectUrlMap = mutableMapOf<String, Collection<JsonPrimitive>>()
        pageIdMap.keys.forEach { key -> projectUrlMap[key] = pageIdMap[key]!!.values.map { JsonPrimitive(it) } }

        // TODO: for now we're not transmitting the free-form Name and Description of a reading list.
        val exportedReadingLists = ExportedReadingLists(projectUrlMap) // , readingList.title, readingList.description)
        return Base64.encodeToString(JsonUtil.encodeToString(exportedReadingLists)!!.toByteArray(), Base64.NO_WRAP)
    }

    @Suppress("unused")
    @Serializable
    class ExportedReadingLists(
        val list: Map<String, Collection<JsonPrimitive>>,
        val name: String? = null,
        val description: String? = null
    )
}
