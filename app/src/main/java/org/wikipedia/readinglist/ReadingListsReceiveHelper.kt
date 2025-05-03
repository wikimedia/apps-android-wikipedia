package org.wikipedia.readinglist

import android.util.Base64
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.Namespace
import org.wikipedia.readinglist.ReadingListsShareHelper.ExportedReadingListPage
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.StringUtil

object ReadingListsReceiveHelper {

    suspend fun receiveReadingLists(emptyTitle: String, emptyDescription: String, json: String, encoded: Boolean): ReadingList {
        val readingListData = getExportedReadingLists(json, encoded)
        val listTitle = readingListData?.name.orEmpty().ifEmpty { emptyTitle }
        val listDescription = readingListData?.description.orEmpty().ifEmpty { emptyDescription }
        val listPages = mutableListOf<ReadingListPage>()

        // Request API by languages
        readingListData?.list?.forEach { map ->
            val wikiSite = WikiSite.forLanguageCode(map.key)
            map.value.chunked(ReadingListsShareHelper.API_MAX_SIZE).forEach { list ->

                val listOfTitles = list.filter { it is JsonPrimitive && it.isString }.map { (it as JsonPrimitive).content }
                val listOfIds = list.filter { it is JsonPrimitive && !it.isString }.map { (it as JsonPrimitive).int }
                val listOfPages = list.filter { it is JsonObject }.map { JsonUtil.json.decodeFromJsonElement<ExportedReadingListPage>(it as JsonObject) }

                listOfPages.forEach {
                    val readingListPage = ReadingListPage(
                        wikiSite,
                        Namespace.of(it.ns),
                        it.title,
                        StringUtil.addUnderscores(it.title),
                        it.description,
                        ImageUrlUtil.getUrlForPreferredSize(it.thumbUrl.orEmpty(), Service.PREFERRED_THUMB_SIZE),
                        lang = wikiSite.languageCode
                    )
                    listPages.add(readingListPage)
                }

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

    private fun getExportedReadingLists(json: String, encoded: Boolean): ReadingListsShareHelper.ExportedReadingList? {
        return JsonUtil.decodeFromString(if (encoded) String(Base64.decode(json, Base64.NO_WRAP)) else json)
    }
}
