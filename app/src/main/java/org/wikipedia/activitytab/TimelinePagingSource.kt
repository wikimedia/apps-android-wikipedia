package org.wikipedia.activitytab

import androidx.paging.PagingSource
import androidx.paging.PagingState

class TimelinePagingSource(
    private val repository: TimelineRepository
) : PagingSource<TimelinePageKey, TimelineItem>() {
    override fun getRefreshKey(state: PagingState<TimelinePageKey, TimelineItem>): TimelinePageKey? {
        return null
    }

    override suspend fun load(params: LoadParams<TimelinePageKey>): LoadResult<TimelinePageKey, TimelineItem> {
        return try {
            val key = params.key ?: TimelinePageKey(continueToken = null, dbOnly = false, dbOffset = 0)
            val result = repository.getTimelinePage(
                pageSize = 20,
                continueToken = key.continueToken,
                dbOnly = key.dbOnly,
                dbOffset = key.dbOffset,
                startDate = key.startDate
            )

            val nextKey = when {
                result.isApiExhausted && result.items.isNotEmpty() -> {
                    TimelinePageKey(null, dbOnly = true, dbOffset = 0, startDate = result.lastDbDate)
                }
                key.dbOnly && result.items.isNotEmpty() -> {
                    // here start will be the last date from the last item in the exhausted api
                    TimelinePageKey(null, dbOnly = true, dbOffset = key.dbOffset + result.items.size, startDate = result.lastDbDate)
                }
                result.nextContinueToken != null -> {
                    TimelinePageKey(result.nextContinueToken, dbOnly = false, dbOffset = 0)
                }
                else -> null // No more data
            }
            LoadResult.Page(
                data = result.items,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
