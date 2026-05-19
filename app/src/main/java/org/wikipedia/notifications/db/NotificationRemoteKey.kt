package org.wikipedia.notifications.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class NotificationRemoteKey(
    // used with constant value
    @PrimaryKey val wiki: String,
    // the value of "notcontinue" received in the API response
    val nextContinueStr: String?,
    // user has already loaded all data in previous run of the app
    val endOfPaginationReached: Boolean
)
