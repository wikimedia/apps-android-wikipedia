package org.wikipedia.notifications

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import org.wikipedia.notifications.db.Notification

@OptIn(ExperimentalPagingApi::class)
class NotificationRemoteMediator(private val repository: NotificationRepository): RemoteMediator<Int, Notification>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Notification>
    ): MediatorResult {
        return try {
            val wikiKey = "aggregated" // Using a constant key for cross-wiki aggregated notifications
            
            val continueStr = when (loadType) {
                LoadType.REFRESH -> {
                    null
                }
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val key = repository.getRemoteKey(wikiKey) ?: return MediatorResult.Success(
                        endOfPaginationReached = true
                    )
                    // above code will return if NULL is stored as key in the database
                    key // a non-NULL value is present.
                }
            }

            val nextContinueStr = repository.fetchAndSave("read|!read", continueStr)
            Log.d("NotificationRemoteMediator", "(loadType=$loadType): repository.fetchAndSave responded with $nextContinueStr")
            if (loadType == LoadType.REFRESH) {
                repository.clearRemoteKeys()
            }
            repository.saveRemoteKey(wikiKey, nextContinueStr)

            MediatorResult.Success(endOfPaginationReached = nextContinueStr == null)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
