package org.wikipedia.readinglist

import android.content.Context
import android.util.Base64
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.json.JsonUtil
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.StringUtil
import java.util.Date

class PreviewReadingListRepository(
    private val context: Context = WikipediaApp.instance,
    private val prefs: Prefs = Prefs
) {

    suspend fun getPreviewReadingList(): ReadingList? {
        val encodedJson = prefs.receiveReadingListsData
        if (encodedJson.isNullOrEmpty()) {
            return null
        }

        return fetchReadingListsFromServer(encodedJson)
    }

    private suspend fun fetchReadingListsFromServer(encodedJson: String): ReadingList? {
        val readingListData = getExportedReadingLists(encodedJson) ?: return null

        val listTitle = readingListData.name.orEmpty()
            .ifEmpty { context.getString(R.string.reading_lists_preview_header_title) }
        val listDescription = readingListData.description.orEmpty()
            .ifEmpty { DateUtil.getTimeAndDateString(context, Date()) }

        val readingLists = readingListData.list
        val listPages = readingLists
            .map { fetchPagesForReadingList(it) }
            .reduce { allPages, currentPayload -> allPages + currentPayload }
            .sortedBy { it.apiTitle }

        val readingList = ReadingList(listTitle, listDescription)
        readingList.pages.addAll(listPages)

        return readingList
    }

    private suspend fun fetchPagesForReadingList(
        list: Map.Entry<String, Collection<JsonPrimitive>>
    ): List<ReadingListPage> {
        val wikiSite = WikiSite.forLanguageCode(list.key)
        val chunks = list.value.chunked(ReadingListsShareHelper.API_MAX_SIZE)
        val pages = mutableListOf<MwQueryPage>()
        for (chunk in chunks) {
            pages += fetchPagesForChunk(wikiSite, chunk)
        }

        return pages.map { page ->
            ReadingListPage(
                wikiSite,
                page.namespace(),
                page.displayTitle(wikiSite.languageCode),
                StringUtil.addUnderscores(page.title),
                page.description,
                ImageUrlUtil.getUrlForPreferredSize(
                    original = page.thumbUrl().orEmpty(),
                    size = Service.PREFERRED_THUMB_SIZE
                ),
                lang = wikiSite.languageCode
            )
        }
    }

    private suspend fun fetchPagesForChunk(
        wikiSite: WikiSite,
        payload: List<JsonPrimitive>
    ): MutableList<MwQueryPage> {
        val pages = mutableListOf<MwQueryPage>()
        val listOfTitles = payload.filter { it.isString }.map { it.content }
        val listOfIds = payload.filter { !it.isString }.map { it.int }
        if (listOfIds.isNotEmpty()) {
            val pagesById = ServiceFactory.get(wikiSite)
                .getInfoByPageIdsOrTitles(
                    pageIds = listOfIds.joinToString(separator = "|")
                )
                .query?.pages.orEmpty()
            pages.addAll(pagesById)
        }
        if (listOfTitles.isNotEmpty()) {
            val pagesByTitles = ServiceFactory.get(wikiSite)
                .getInfoByPageIdsOrTitles(titles = listOfTitles.joinToString(separator = "|"))
                .query?.pages.orEmpty()
            pages.addAll(pagesByTitles)
        }

        return pages
    }

    private fun getExportedReadingLists(
        encodedJson: String
    ): ReadingListsShareHelper.ExportedReadingLists? {
        return JsonUtil.decodeFromString(String(Base64.decode(encodedJson, Base64.NO_WRAP)))
    }
}
