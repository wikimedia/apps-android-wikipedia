package org.wikipedia.database.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class User(
        @PrimaryKey val _id: Int,
        @ColumnInfo(name = "site") val site: String?,
        @ColumnInfo(name = "title") val title: String?,
        @ColumnInfo(name = "namespace") val namespace: String?,
        @ColumnInfo(name = "lang") val lang: String?,
        @ColumnInfo(name = "timestamp") val timestamp: Int,
        @ColumnInfo(name = "source") val source: Int,
        @ColumnInfo(name = "timeSpent") val timeSpent: Int
)