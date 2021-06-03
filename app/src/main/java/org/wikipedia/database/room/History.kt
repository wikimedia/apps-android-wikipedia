package org.wikipedia.database.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "his")
public class History(
        @PrimaryKey val _id: Int,
        @ColumnInfo(name = "site") val site: String?,
        @ColumnInfo(name = "title") val title: String?
)
