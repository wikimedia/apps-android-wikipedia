
package org.wikipedia.readinglist

import android.content.Context
import android.util.Base64
import kotlinx.serialization.json.int
import org.wikipedia.R
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.json.JsonUtil
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.StringUtil
import java.util.*

object ReadingListsReceiveHelper {

    suspend fun receiveReadingLists(context: Context, encodedJson: String): ReadingList {
        val readingListData = getExportedReadingLists(encodedJson)
        val listTitle = readingListData?.name.orEmpty().ifEmpty { context.getString(R.string.reading_lists_preview_header_title) }
        val listDescription = readingListData?.description.orEmpty().ifEmpty { DateUtil.getTimeAndDateString(context, Date()) }
        val listPages = mutableListOf<ReadingListPage>()

        // Request API by languages
        readingListData?.list?.forEach { map ->
            val wikiSite = WikiSite.forLanguageCode(map.key)
            map.value.chunked(ReadingListsShareHelper.API_MAX_SIZE).forEach { list ->
                val listOfTitles = list.filter { it.isString }.map { it.content }
                val listOfIds = list.filter { !it.isString }.map { it.int }

                val pages = mutableListOf<MwQueryPage>()
                if (listOfIds.isNotEmpty()) {
                    pages.addAll(ServiceFactory.get(wikiSite).getInfoByPageIdsOrTitles(pageIds = listOfIds.joinToString(separator = "|"))
                        .query?.pages.orEmpty())
                }
                if (listOfTitles.isNotEmpty()) {
                    pages.addAll(ServiceFactory.get(wikiSite).getInfoByPageIdsOrTitles(titles = listOfTitles.joinToString(separator = "|"))
                        .query?.pages.orEmpty())
                }
                pages.forEach {
                    val readingListPage = ReadingListPage(
                        wikiSite,
                        it.namespace(),
                        it.displayTitle(wikiSite.languageCode),
                        StringUtil.addUnderscores(it.title),
                        it.description,
                        ImageUrlUtil.getUrlForPreferredSize(it.thumbUrl().orEmpty(), Service.PREFERRED_THUMB_SIZE),
                        lang = wikiSite.languageCode
                    )
                    listPages.add(readingListPage)
                }
            }
        }

        listPages.sortBy { it.apiTitle }

        val readingList = ReadingList(listTitle, listDescription)
        readingList.pages.addAll(listPages)

        return readingList
    }

    private fun getExportedReadingLists(encodedJson: String): ReadingListsShareHelper.ExportedReadingLists? {
        return JsonUtil.decodeFromString(String(Base64.decode(encodedJson, Base64.NO_WRAP)))
    }
}
