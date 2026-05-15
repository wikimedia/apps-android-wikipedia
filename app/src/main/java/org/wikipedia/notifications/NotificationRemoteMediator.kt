package org.wikipedia.notifications

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import org.wikipedia.notifications.db.Notification

class NotificationRemoteMediator: RemoteMediator<Int, Notification> {
    @OptIn(ExperimentalPagingApi::class)
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Notification>
    ): MediatorResult {
        TODO("Not yet implemented")
    }
}