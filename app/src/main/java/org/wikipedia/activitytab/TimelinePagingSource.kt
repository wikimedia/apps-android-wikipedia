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
            val key = params.key ?: TimelinePageKey(continueToken = null)
            val result = repository.getTimelinePage(
                pageSize = params.loadSize,
                continueToken = key.continueToken
            )

            println("orange --> continue key ${key.continueToken}")
            val nextKey = if (result.nextContinueToken != null) {
                TimelinePageKey(
                    continueToken = result.nextContinueToken
                )
            } else null
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
