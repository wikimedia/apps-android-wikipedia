package org.wikipedia.activitytab

import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry.Companion.SOURCE_SEARCH
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.readinglist.db.ReadingListPageDao
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

interface TimelineSource {
    suspend fun fetch(pageSize: Int, cursor: Cursor?): Pair<List<TimelineItem>, Cursor?>
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

class HistoryEntrySource(
    private val dao: HistoryEntryWithImageDao
) : TimelineSource {

    override suspend fun fetch(pageSize: Int, cursor: Cursor?): Pair<List<TimelineItem>, Cursor?> {
        val offset = (cursor as? Cursor.HistoryEntryCursor)?.offset ?: 0
        val items = dao.getHistoryEntriesWithOffset(pageSize, offset).map { TimelineItem(
            id = it.id,
            title = it.apiTitle,
            description = it.description,
            thumbnailUrl = it.imageName,
            timestamp = it.timestamp,
            activitySource = when (it.source) {
                SOURCE_SEARCH -> ActivitySource.SEARCH
                else -> ActivitySource.LINK
            }
        ) }
        val nextCursor = if (items.size < pageSize) null else Cursor.HistoryEntryCursor(offset + items.size)
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
                title = it.title,
                description = null,
                thumbnailUrl = null,
                timestamp = Date.from(it.parsedDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                activitySource = ActivitySource.EDIT
            )
        } ?: emptyList()

        val nextCursor = response.continuation?.ucContinuation?.let { Cursor.ApiCursor(it) }
        return items to nextCursor
    }
}

class ReadingListSource(
    val dao: ReadingListPageDao
) : TimelineSource {
    override suspend fun fetch(
        pageSize: Int,
        cursor: Cursor?
    ): Pair<List<TimelineItem>, Cursor?> {
        val offset = (cursor as? Cursor.ReadingListCursor)?.offset ?: 0
        val items = dao.getPagesWithLimitOffset(pageSize, offset).map { TimelineItem(
            id = it.id,
            title = it.apiTitle,
            description = it.description,
            thumbnailUrl = it.thumbUrl,
            timestamp = Date(it.mtime),
            wiki = it.wiki.languageCode,
            activitySource = ActivitySource.EDIT
        ) }
        val nextCursor = if (items.size < pageSize) null else Cursor.HistoryEntryCursor(offset + items.size)
        return items to nextCursor
    }
}

// Data Models
enum class ActivitySource {
    EDIT, SEARCH, LINK
}

data class TimelineItem(
    val id: Long,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val timestamp: Date,
    val wiki: String = "en",
    val activitySource: ActivitySource?
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
