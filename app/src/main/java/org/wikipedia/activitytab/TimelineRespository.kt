package org.wikipedia.activitytab

import org.wikipedia.Constants
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry.Companion.SOURCE_EXTERNAL_LINK
import org.wikipedia.history.HistoryEntry.Companion.SOURCE_INTERNAL_LINK
import org.wikipedia.history.HistoryEntry.Companion.SOURCE_LANGUAGE_LINK
import org.wikipedia.history.HistoryEntry.Companion.SOURCE_SEARCH
import org.wikipedia.settings.Prefs
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.collections.first

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

    suspend fun getLocalTimelineItemsInRange(startTime: Date, endTime: Date): List<TimelineItem> {
        val items = AppDatabase.instance.historyEntryWithImageDao()
            .getHistoryEntriesInRange(startTime, endTime)
        return items.map {
            TimelineItem(
                id = it.id,
                title = it.apiTitle,
                description = it.description,
                thumbnailUrl = it.imageName,
                timestamp = it.timestamp,
                wiki = it.lang,
                activitySource = when (it.source) {
                    SOURCE_SEARCH -> ActivitySource.SEARCH
                    SOURCE_INTERNAL_LINK, SOURCE_EXTERNAL_LINK, SOURCE_LANGUAGE_LINK -> ActivitySource.LINK
                    else -> null
                }
            )
        }
    }

    suspend fun getWikipediaContributions(pageSize: Int, continueToken: String?): ApiResult {
        println("orange --> getWikipediaContributions")
        val response = ServiceFactory.get(WikiSite(url = "https://test.wikipedia.org/"))
            .getUserContrib(userName, 2, null, null, continueToken, ucdir = "older")
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

    suspend fun getTimelinePage(pageSize: Int, continueToken: String?): TimelinePageResult {
        val apiResult = try {
            getWikipediaContributions(pageSize, continueToken)
        } catch (e: Exception) {
            ApiResult(emptyList(), null)
        }

        val timeRange = extractTimeRange(apiResult.items)
        val localItems = if (timeRange != null) {
            val startDate = if (continueToken == null) Date() else timeRange.first
            val endDate = timeRange.second
            val (from, to) = if (startDate.before(endDate)) {
                startDate to endDate
            } else {
                endDate to startDate
            }
            println("orange --> local db from: $from to: $to")
            getLocalTimelineItemsInRange(from, to)
        } else
            emptyList()

        val mergedItems = mergerAndSortItems(localItems, apiResult.items)
        return TimelinePageResult(
            items = mergedItems,
            nextContinueToken = apiResult.nextToken
        )
    }

    private fun extractTimeRange(apiItems: List<TimelineItem>): Pair<Date, Date>? {
        return Pair(apiItems.first().timestamp, apiItems.last().timestamp)
    }
}

// Data class for timeline display items (includes separators)
sealed class TimelineDisplayItem {
    data class DateSeparator(val date: Date) : TimelineDisplayItem()
    data class TimelineEntry(val item: TimelineItem) : TimelineDisplayItem()
}

// Extension function to group timeline items by date
fun List<TimelineItem>.groupByDateWithSeparators(): List<TimelineDisplayItem> {
    if (isEmpty()) return emptyList()

    val groupedByDate = groupBy { it.timestamp.toLocalDate() }
        .toSortedMap(compareByDescending { it })

    return buildList {
        groupedByDate.forEach { (localDate, items) ->
            val dateForSeparator = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

            add(TimelineDisplayItem.DateSeparator(dateForSeparator))

            // Add items for this date, sorted by time (newest first)
            items.sortedByDescending { it.timestamp }
                .forEach { item ->
                    add(TimelineDisplayItem.TimelineEntry(item))
                }
        }
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

data class TimelineUiState(
    val items: List<TimelineItem> = emptyList(),
    val isLoadingMore: Boolean = false,
    val isInitialLoading: Boolean = true,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val hasMoreData: Boolean = true,
    val currentPage: Int = 0
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

data class TimelinePageKey(
    val continueToken: String?
)

enum class ActivitySource {
    EDIT, SEARCH, LINK
}
