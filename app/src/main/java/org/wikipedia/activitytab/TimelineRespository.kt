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

class TimelineRepository(private val userName: String) {
    var langCode = Prefs.userContribFilterLangCode

    val wikiSite get(): WikiSite {
        return when (langCode) {
            Constants.WIKI_CODE_COMMONS -> WikiSite(Service.COMMONS_URL)
            Constants.WIKI_CODE_WIKIDATA -> WikiSite(Service.WIKIDATA_URL)
            else -> WikiSite.forLanguageCode(langCode)
        }
    }

    suspend fun getLocalTimelineItems(page: Int, pageSize: Int): List<TimelineItem> {
        val items = AppDatabase.instance.historyEntryWithImageDao().filterHistoryItemsWithoutTime("")
        println("orange --> getLocalTimelineItems")
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
                    SOURCE_INTERNAL_LINK, SOURCE_EXTERNAL_LINK, SOURCE_LANGUAGE_LINK -> ActivitySource.LINK
                    else -> null
                }
            )
        }
    }

    suspend fun getWikipediaContributions(page: Int, pageSize: Int): List<TimelineItem> {
        val response = ServiceFactory.get(wikiSite).getUserContrib(userName, 500, null, null, null)
        val contribs = response.query?.userContributions!!
        println("orange --> getWikipediaContributions")
        return contribs.map {
            TimelineItem(
                id = it.revid,
                title = it.title,
                description = null,
                thumbnailUrl = null,
                timestamp = Date.from(it.parsedDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                activitySource = ActivitySource.EDIT
            )
        }
    }

    private fun mergeTimelines(
        localItems: List<TimelineItem>,
        apiItems: List<TimelineItem>
    ): List<TimelineItem> {
        return (localItems + apiItems)
            .sortedBy { it.timestamp }
            .distinctBy { it.id }
    }

    suspend fun getTimelinePage(page: Int, pageSize: Int): Pair<List<TimelineItem>, Boolean> {
        val localItems = getLocalTimelineItems(page, pageSize)
        return try {
            val apiItems = getWikipediaContributions(page, pageSize)
            val mergedItems = mergeTimelines(localItems, apiItems)
            val hasMoreData = localItems.size == pageSize || apiItems.isNotEmpty()

            Pair(mergedItems, hasMoreData)
        } catch (e: Exception) {
            // If API fails, return local data only
            val hasMoreData = localItems.size == pageSize
            Pair(localItems, hasMoreData)
        }
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

enum class ActivitySource {
    EDIT, SEARCH, LINK
}
