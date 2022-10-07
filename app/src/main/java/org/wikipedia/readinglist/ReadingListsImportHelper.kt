package org.wikipedia.readinglist

import android.util.Base64
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage

object ReadingListsImportHelper {

    suspend fun importReadingLists(encodedJson: String): ReadingList {
        val readingListData = getExportedReadingLists(encodedJson)
        val listTitle = readingListData?.name.orEmpty()
        val listDescription = readingListData?.description.orEmpty()
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
                    page.title,
                    page.description,
                    page.thumbUrl()
                )
                listPages.add(readingListPage)
            }
        }

        val readingList = ReadingList(listTitle, listDescription)
        readingList.pages.addAll(listPages)

        return readingList
    }

    private fun getExportedReadingLists(encodedJson: String): ReadingListsShareHelper.ExportedReadingLists? {
        return JsonUtil.decodeFromString(String(Base64.decode(encodedJson, Base64.NO_WRAP)))
    }
}
