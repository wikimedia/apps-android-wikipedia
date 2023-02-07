package org.wikipedia.readinglist

import android.content.Context
import android.util.Base64
import org.wikipedia.R
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil
import java.util.*

object ReadingListsImportHelper {

    suspend fun importReadingLists(context: Context, encodedJson: String): ReadingList {
        val readingListData = getExportedReadingLists(encodedJson)
        val listTitle = readingListData?.name.orEmpty().ifEmpty { context.getString(R.string.reading_list_name_sample) }
        val listDescription = readingListData?.description.orEmpty().ifEmpty { DateUtil.getTimeAndDateString(context, Date()) }
        val listPages = mutableListOf<ReadingListPage>()

        // Request API by languages
        readingListData?.list?.forEach {
            val wikiSite = WikiSite.forLanguageCode(it.key)
            val response = ServiceFactory.get(wikiSite).getPageTitlesByPageId(it.value.joinToString(separator = "|"))

            response.query?.pages?.forEach { page ->
                val readingListPage = ReadingListPage(
                    wikiSite,
                    page.namespace(),
                    page.displayTitle(wikiSite.languageCode),
                    StringUtil.addUnderscores(page.title),
                    page.description,
                    page.thumbUrl(),
                    lang = wikiSite.languageCode
                )
                listPages.add(readingListPage)
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
