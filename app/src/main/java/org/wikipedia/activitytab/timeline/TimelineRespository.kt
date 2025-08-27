package org.wikipedia.activitytab.timeline

import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.HistoryEntry.Companion.SOURCE_SEARCH
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.db.ReadingListPageDao
import org.wikipedia.util.DateUtil
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

interface TimelineSource {
    suspend fun fetch(pageSize: Int, cursor: Cursor?): Pair<List<TimelineItem>, Cursor?>
}

fun formatDate(date: Date): String {
    val localDate = date.toLocalDate()
    val now = LocalDate.now()
    val daysDiff = ChronoUnit.DAYS.between(localDate, now)

    return when {
        daysDiff < 7 -> {
            val dayOfWeek = localDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            dayOfWeek
        }

        localDate.year == now.year -> {
            DateUtil.getMonthOnlyDateString(localDate)
        }

        else -> {
            DateUtil.getShortDateString(localDate)
        }
    }
}

class HistoryEntrySource(
    private val dao: HistoryEntryWithImageDao
) : TimelineSource {

    override suspend fun fetch(pageSize: Int, cursor: Cursor?): Pair<List<TimelineItem>, Cursor?> {
        val offset = (cursor as? Cursor.HistoryEntryCursor)?.offset ?: 0
        val items = dao.getHistoryEntriesWithOffset(pageSize, offset).map {
            TimelineItem(
                id = it.id,
                pageId = 0,
                authority = it.authority,
                apiTitle = it.apiTitle,
                displayTitle = it.displayTitle,
                description = it.description,
                thumbnailUrl = it.imageName,
                timestamp = it.timestamp,
                namespace = it.namespace,
                lang = it.lang,
                source = it.source,
                activitySource = when (it.source) {
                    SOURCE_SEARCH -> ActivitySource.SEARCH
                    else -> ActivitySource.LINK
                }
            )
        }
        val nextCursor =
            if (items.size < pageSize) null else Cursor.HistoryEntryCursor(offset + items.size)
        return items to nextCursor
    }
}

class ApiTimelineSource(
    private val wikiSite: WikiSite,
    private val userName: String
) : TimelineSource {

    override suspend fun fetch(pageSize: Int, cursor: Cursor?): Pair<List<TimelineItem>, Cursor?> {
        val token = (cursor as? Cursor.ApiCursor)?.token
        val response = ServiceFactory.get(wikiSite)
            .getUserContrib(userName, pageSize, null, null, token, ucdir = "older")

        val items = response.query?.userContributions?.map {
            TimelineItem(
                id = it.revid,
                pageId = it.pageid,
                displayTitle = it.title,
                description = null,
                thumbnailUrl = null,
                timestamp = Date.from(it.parsedDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                activitySource = ActivitySource.EDIT,
                source = -1
            )
        } ?: emptyList()

        val nextCursor = response.continuation?.ucContinuation?.let { Cursor.ApiCursor(it) }
        return items to nextCursor
    }
}

class ReadingListSource(
    val dao: ReadingListPageDao
) : TimelineSource {
    override suspend fun fetch(pageSize: Int, cursor: Cursor?): Pair<List<TimelineItem>, Cursor?> {
        val offset = (cursor as? Cursor.ReadingListCursor)?.offset ?: 0
        val items = dao.getPagesWithLimitOffset(pageSize, offset).map {
            TimelineItem(
                id = it.mtime + it.atime,
                pageId = 0,
                apiTitle = it.apiTitle,
                displayTitle = it.displayTitle,
                description = it.description,
                thumbnailUrl = it.thumbUrl,
                timestamp = Date(it.mtime),
                wiki = it.wiki,
                activitySource = ActivitySource.BOOKMARKED,
                source = -1
            )
        }
        val nextCursor =
            if (items.size < pageSize) null else Cursor.HistoryEntryCursor(offset + items.size)
        return items to nextCursor
    }
}

// Data Models
enum class ActivitySource {
    EDIT, SEARCH, LINK, BOOKMARKED
}

data class TimelineItem(
    val id: Long,
    val pageId: Int,
    val description: String?,
    val thumbnailUrl: String?,
    val timestamp: Date,
    val source: Int,
    val activitySource: ActivitySource?,
    var authority: String = "",
    var lang: String = "",
    var apiTitle: String = "",
    var displayTitle: String = "",
    var namespace: String = "",
    val wiki: WikiSite? = null
)

sealed class Cursor {
    data class ApiCursor(val token: String?) : Cursor()
    data class HistoryEntryCursor(val offset: Int) : Cursor()
    data class ReadingListCursor(val offset: Int) : Cursor()
}

data class TimelinePageKey(
    val cursors: Map<String, Cursor> = emptyMap()
)

// Extension functions
fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

fun Date.isToday(): Boolean {
    return this.toLocalDate() == LocalDate.now()
}

fun Date.isYesterday(): Boolean {
    return this.toLocalDate() == LocalDate.now().minusDays(1)
}

fun toHistoryEntry(timelineItem: TimelineItem): HistoryEntry {
    val entry = HistoryEntry(
        authority = timelineItem.authority,
        lang = timelineItem.lang,
        apiTitle = timelineItem.apiTitle,
        displayTitle = timelineItem.displayTitle,
        id = timelineItem.id,
        namespace = timelineItem.namespace,
        timestamp = timelineItem.timestamp,
        source = timelineItem.source
    )
    entry.title.thumbUrl = timelineItem.thumbnailUrl
    entry.title.description = timelineItem.description

    return entry
}

fun toPageTitle(timelineItem: TimelineItem): PageTitle {
    val wiki = timelineItem.wiki
    wiki?.languageCode = timelineItem.lang
    return PageTitle(
        timelineItem.apiTitle,
        wiki!!,
        timelineItem.thumbnailUrl,
        timelineItem.description,
        timelineItem.displayTitle
    )
}
