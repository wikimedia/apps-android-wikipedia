package org.wikipedia.talk.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class DefaultReplies constructor(
    @PrimaryKey val text: String,
    val itemOrder: Int
)
