package org.wikipedia.activitytab.timeline

import org.wikipedia.Constants
import org.wikipedia.activitytab.toLocalDate
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry.Companion.SOURCE_SEARCH
import org.wikipedia.settings.Prefs
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

class TimelineRepository(private val userName: String) {
    var langCode = Prefs.userContribFilterLangCode

    val wikiSite
        get(): WikiSite {
            return when (langCode) {
                Constants.WIKI_CODE_COMMONS -> WikiSite(Service.COMMONS_URL)
                Constants.WIKI_CODE_WIKIDATA -> WikiSite(Service.WIKIDATA_URL)
                else -> WikiSite.forLanguageCode(langCode)
            }
        }

    suspend fun getLocalTimelineItems(): List<TimelineItem> {
        val items = AppDatabase.instance.historyEntryWithImageDao().filterHistoryItemsWithoutTime("")
        return items.map {
            TimelineItem(
                id = it.id,
                title = it.apiTitle,
                description = it.title.description,
                thumbnailUrl = it.title.thumbUrl,
                timestamp = it.timestamp,
                wiki = it.lang,
                activitySource = when (it.source) {
                    SOURCE_SEARCH -> ActivitySource.SEARCH
                    else -> ActivitySource.LINK
                }
            )
        }
    }

    suspend fun getWikipediaContributions(pageSize: Int, continueToken: String?): ApiResult {
        println("orange --> getWikipediaContributions")
        val response = ServiceFactory.get(WikiSite(url = "https://test.wikipedia.org/"))
            .getUserContrib(userName, pageSize, null, null, continueToken, ucdir = "older")
        val contribs = response.query?.userContributions ?: emptyList()
        val nextToken = response.continuation?.ucContinuation
        val items = contribs.map {
            TimelineItem(
                id = it.revid,
                title = it.title,
                description = null,
                thumbnailUrl = null,
                timestamp = Date.from(it.parsedDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                activitySource = ActivitySource.EDIT
            )
        }

        return ApiResult(
            items,
            nextToken
        )
    }

    private fun mergerAndSortItems(
        localItems: List<TimelineItem>,
        apiItems: List<TimelineItem>
    ): List<TimelineItem> {
        return (localItems + apiItems)
            .sortedByDescending { it.timestamp }
            .distinctBy { it.id }
    }
}

fun formatDate(date: LocalDate): String {
    val now = LocalDate.now()
    val daysDiff = ChronoUnit.DAYS.between(date, now)

    return when {
        daysDiff < 7 -> {
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            dayOfWeek
        }

        date.year == now.year -> {
            DateTimeFormatter.ofPattern("MMMM d").format(date)
        }

        else -> {
            DateTimeFormatter.ofPattern("MMMM d, yyyy").format(date)
        }
    }
}

// Stable display items with consistent keys
sealed class StableDisplayItem(val key: String) {
    data class DateSeparator(
        val date: Date,
        private val dateKey: String = "date_${date.toLocalDate()}"
    ) : StableDisplayItem(dateKey)

    data class TimelineEntry(
        val item: TimelineItem,
        private val itemKey: String = "item_${item.id}_${item.activitySource?.name ?: "unknown"}"
    ) : StableDisplayItem(itemKey)
}

/**
 * Creates stable display items that minimize recomposition
 * Uses consistent keys for dates and items
 */
fun createStableDisplayItems(items: List<TimelineItem>): List<StableDisplayItem> {
    val displayItems = mutableListOf<StableDisplayItem>()
    var lastDate: LocalDate? = null

    items.forEach { item ->
        val itemDate = item.timestamp.toLocalDate()

        // Add date separator if needed
        if (lastDate != itemDate) {
            displayItems.add(StableDisplayItem.DateSeparator(item.timestamp))
            lastDate = itemDate
        }

        displayItems.add(StableDisplayItem.TimelineEntry(item))
    }

    return displayItems
}

data class TimelineState(
    val databaseItems: List<TimelineItem> = emptyList(),
    val apiItemsByPage: Map<Int, List<TimelineItem>> = emptyMap(),
    val isLoadingDatabase: Boolean = false,
    val isLoadingApi: Boolean = false,
    val apiPageCount: Int = 0,
    val hasMoreApiData: Boolean = true,
    val error: Throwable? = null
)

// Data Models
data class TimelineItem(
    val id: Long,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val timestamp: Date,
    val wiki: String = "en",
    val activitySource: ActivitySource?
)
data class ApiResult(
    val items: List<TimelineItem>,
    val nextToken: String?
)

data class TimelinePageResult(
    val items: List<TimelineItem>,
    val nextContinueToken: String?
)
enum class ActivitySource {
    EDIT, SEARCH, LINK
}
