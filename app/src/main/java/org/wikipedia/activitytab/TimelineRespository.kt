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

    suspend fun getHistoryItemsOlderThan(
        startDate: Date,
        pageSize: Int,
        offset: Int
    ): List<TimelineItem> {
        val items = AppDatabase.instance.historyEntryWithImageDao()
            .getHistoryEntriesOlderThan(startDate, pageSize, offset)
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

    suspend fun getHistoryItemsInRange(startTime: Date, endTime: Date): List<TimelineItem> {
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
        val response = ServiceFactory.get(wikiSite)
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

    suspend fun getTimelinePage(
        pageSize: Int,
        continueToken: String?,
        dbOnly: Boolean,
        dbOffset: Int = 0,
        startDate: Date? = null
    ): TimelinePageResult {
        return if (dbOnly) {
            val localItems = getHistoryItemsOlderThan(startDate!!, 2, dbOffset)
            TimelinePageResult(
                localItems,
                nextContinueToken = if (localItems.isEmpty()) null else "db-key",
                lastDbDate = localItems.lastOrNull()?.timestamp
            )
        } else {
            val apiResult = try {
                getWikipediaContributions(pageSize, continueToken)
            } catch (e: Exception) {
                ApiResult(emptyList(), null)
            }

            val timeRange = extractTimeRange(apiResult.items)
            val historyItems = if (timeRange != null) {
                val startDate = if (continueToken == null) Date() else timeRange.first
                val endDate = timeRange.second
                val (from, to) = if (startDate.before(endDate)) {
                    startDate to endDate
                } else {
                    endDate to startDate
                }
                getHistoryItemsInRange(from, to)
            } else
                emptyList()

            val mergedItems = mergerAndSortItems(historyItems, apiResult.items)
            TimelinePageResult(
                items = mergedItems,
                nextContinueToken = apiResult.nextToken,
                isApiExhausted = apiResult.nextToken == null,
                lastDbDate = historyItems.lastOrNull()?.timestamp
            )
        }
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
    val nextContinueToken: String?,
    val isApiExhausted: Boolean = false,
    val lastDbDate: Date? = null
)

data class TimelinePageKey(
    val continueToken: String?,
    val dbOnly: Boolean = false,
    val dbOffset: Int = 0,
    val startDate: Date? = null
)

enum class ActivitySource {
    EDIT, SEARCH, LINK
}
