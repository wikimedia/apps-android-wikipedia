package org.wikipedia.activitytab

import androidx.paging.PagingSource
import androidx.paging.PagingState

class TimelinePagingSource(
    private val sources: List<TimelineSource>
) : PagingSource<TimelinePageKey, TimelineItem>() {
    override fun getRefreshKey(state: PagingState<TimelinePageKey, TimelineItem>): TimelinePageKey? {
        return null
    }

    override suspend fun load(params: LoadParams<TimelinePageKey>): LoadResult<TimelinePageKey, TimelineItem> {
        return try {
            val key = params.key ?: TimelinePageKey()
            val allItems = mutableListOf<TimelineItem>()
            val newCursors = mutableMapOf<String, Cursor>()

            sources.forEach { source ->
                val sourceName = source.javaClass.simpleName
                val cursor = key.cursors[sourceName]
                if (cursor != null || key.cursors.isEmpty()) {
                    val (items, nextCursor) = source.fetch(params.loadSize, cursor)
                    allItems.addAll(items)
                    nextCursor?.let { newCursors[sourceName] = it }
                }
            }

            val merged = allItems
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }

            LoadResult.Page(
                data = merged,
                prevKey = null,
                nextKey = if (newCursors.isEmpty()) null else TimelinePageKey(newCursors)
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
