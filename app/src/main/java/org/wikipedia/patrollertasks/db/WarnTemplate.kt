package org.wikipedia.patrollertasks.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WarnTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var order: Int,
    var title: String,
    var subject: String,
    var message: String)
