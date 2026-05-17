package org.wikipedia.notifications.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class NotificationRemoteKey(
    @PrimaryKey val wiki: String,   // used with constant value
    val nextContinueStr: String?,   // the value of "notcontinue" received in the API response
    val endOfPaginationReached: Boolean // user has already loaded all data in previous run of the app
)
