package org.wikipedia.activitytab.timeline

import kotlinx.coroutines.delay
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.HistoryEntry.Companion.SOURCE_SEARCH
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.page.Namespace
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
        delay(500)
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
    private val userName: String,
    private val historyEntryWithImageDao: HistoryEntryWithImageDao
) : TimelineSource {

    private val maxBatchSize = 50

    override val id: String = "user_contrib"

    override suspend fun fetch(pageSize: Int, cursor: Cursor?): Pair<List<TimelineItem>, Cursor?> {
        val token = (cursor as? Cursor.UserContribCursor)?.token
        val service = ServiceFactory.get(wikiSite)
        val userContribResponse = service.getUserContrib(username = userName, maxCount = pageSize, ns = null, filter = null, uccontinue = token, ucdir = "older")

        val missingPageInfoIds = mutableListOf<Int>()
        val timelineItemsByPageId = mutableMapOf<Long, TimelineItem>()
        userContribResponse.query?.userContributions?.forEach { contribution ->
            // pageId for article namespace and revid for other namespace as key because they can have similar pageId for example (User talk namespace)
            // Only check database cache for article namespace article
            val savedHistoryItem = if (contribution.ns == Namespace.MAIN.code()) historyEntryWithImageDao.getHistoryItemWIthImage(contribution.title).firstOrNull() else null
            val keyForMap = savedHistoryItem?.id ?: contribution.revid
            val timelineItem = TimelineItem(
                id = contribution.revid,
                pageId = contribution.pageid,
                apiTitle = contribution.title,
                displayTitle = contribution.title,
                description = savedHistoryItem?.description,
                thumbnailUrl = savedHistoryItem?.imageName,
                timestamp = Date.from(contribution.parsedDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                activitySource = ActivitySource.EDIT,
                source = -1
            )
            timelineItemsByPageId[keyForMap] = timelineItem
            // only fetch page info for contribution in article namespace
            if (contribution.ns == Namespace.MAIN.code() && savedHistoryItem == null) {
                missingPageInfoIds.add(contribution.pageid)
            }
        }

        // Fetching missing page info in batches
        missingPageInfoIds.chunked(maxBatchSize).forEach { batch ->
            val pages = service.getInfoByPageIdsOrTitles(pageIds = batch.joinToString(separator = "|")).query?.pages.orEmpty()
            pages.forEach { page ->
                timelineItemsByPageId[page.pageId.toLong()]?.let { existingItem ->
                    timelineItemsByPageId[page.pageId.toLong()] = existingItem.copy(
                        description = page.description,
                        thumbnailUrl = page.thumbUrl()
                    )
                }
            }
        }

        val items = timelineItemsByPageId.values.sortedByDescending { it.timestamp }

        val nextCursor = userContribResponse.continuation?.ucContinuation?.let { Cursor.UserContribCursor(it) }
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
                id = it.mtime + it.atime + it.id,
                pageId = 0,
                apiTitle = it.apiTitle,
                displayTitle = it.displayTitle,
                description = it.description,
                thumbnailUrl = it.thumbUrl,
                timestamp = Date(it.mtime),
                wiki = WikiSite.forLanguageCode(it.lang),
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
