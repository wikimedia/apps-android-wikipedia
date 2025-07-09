package org.wikipedia.page.tabs

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PageBackStackItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageBackStackItem(item: PageBackStackItem): Long

    @Query("SELECT * FROM PageBackStackItem WHERE id = :id")
    suspend fun getPageBackStackItem(id: Long): PageBackStackItem

    @Query("SELECT * FROM PageBackStackItem WHERE id IN (:ids)")
    suspend fun getPageBackStackItems(ids: List<Long>): List<PageBackStackItem>

    @Delete
    suspend fun deletePageBackStackItem(item: PageBackStackItem)
}
