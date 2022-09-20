package org.wikipedia.readinglist

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L

object ReadingListsImportHelper {

    fun importReadingLists(activity: AppCompatActivity, encodedUrl: String): ReadingList {

        val decodedString = decodeReadingListUrl(encodedUrl)
        val readingListInfoArray = decodedString.split("|").toMutableList()
        val listTitle = UriUtil.decodeURL(readingListInfoArray.removeFirst())
        val listDescription = UriUtil.decodeURL(readingListInfoArray.removeFirst())
        val listPages = mutableListOf<ReadingListPage>()
        val pageIdsMap = mutableMapOf<String, MutableList<String>>()

        readingListInfoArray.forEach {
            // lang:pageid
            val langPageId = it.split("|")
            pageIdsMap.getOrPut(langPageId[0]) { mutableListOf() }.add(langPageId[1])
        }

        activity.lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            FeedbackUtil.showError(activity, throwable)
        }) {
            // Request API by languages
            pageIdsMap.forEach {
                val wikiSite = WikiSite.forLanguageCode(it.key)
                val response = ServiceFactory.get(wikiSite).getPageTitlesByPageId(it.value.joinToString { "|" })

                response.query?.pages?.forEach { page ->
                    val readingListPage = ReadingListPage(wikiSite, page.namespace(),page.displayTitle(wikiSite.languageCode),
                        page.title, page.description, page.thumbUrl())
                    listPages.add(readingListPage)
                }
            }
        }

        return ReadingList(listTitle, listDescription).apply {
            pages.addAll(listPages)
        }
    }

    private fun decodeReadingListUrl(encodedUrl: String): String {
        // TODO: will be finalized later
        return encodedUrl
    }
}
