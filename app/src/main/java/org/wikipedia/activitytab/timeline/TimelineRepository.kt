package org.wikipedia.activitytab.timeline

import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.HistoryEntry.Companion.SOURCE_SEARCH
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.db.ReadingListPageDao
import java.time.ZoneId
import java.util.Date

interface TimelineSource {
    val id: String
    suspend fun fetch(pageSize: Int, cursor: Cursor?): Pair<List<TimelineItem>, Cursor?>
}

class HistoryEntryPagingSource(
    private val dao: HistoryEntryWithImageDao
) : TimelineSource {

    override val id: String = "history_entry"

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

class UserContribPagingSource(
    private val wikiSite: WikiSite,
    private val userName: String
) : TimelineSource {

    override val id: String = "user_contrib"

    override suspend fun fetch(pageSize: Int, cursor: Cursor?): Pair<List<TimelineItem>, Cursor?> {
        val token = (cursor as? Cursor.UserContribCursor)?.token
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

        val nextCursor = response.continuation?.ucContinuation?.let { Cursor.UserContribCursor(it) }
        return items to nextCursor
    }
}

class ReadingListPagingSource(
    val dao: ReadingListPageDao
) : TimelineSource {

    override val id: String = "reading_list"

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
) {
    fun toHistoryEntry(): HistoryEntry {
        val entry = HistoryEntry(
            authority = authority,
            lang = lang,
            apiTitle = apiTitle,
            displayTitle = displayTitle,
            id = id,
            namespace = namespace,
            timestamp = timestamp,
            source = source
        )
        entry.title.thumbUrl = thumbnailUrl
        entry.title.description = description

        return entry
    }

    fun toPageTitle(): PageTitle {
        val wiki = wiki
        wiki?.languageCode = lang
        return PageTitle(
            apiTitle,
            wiki!!,
            thumbnailUrl,
            description,
            displayTitle
        )
    }
}

sealed class Cursor {
    data class UserContribCursor(val token: String?) : Cursor()
    data class HistoryEntryCursor(val offset: Int) : Cursor()
    data class ReadingListCursor(val offset: Int) : Cursor()
}

data class TimelinePageKey(
    val cursors: Map<String, Cursor> = emptyMap()
)
