package org.wikipedia.page.tabs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PageBackStackItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageBackStackItem(item: PageBackStackItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageBackStackItems(items: List<PageBackStackItem>): List<Long>

    @Query("SELECT * FROM PageBackStackItem WHERE id IN (:ids)")
    suspend fun getPageBackStackItems(ids: List<Long>): List<PageBackStackItem>

    @Query("DELETE FROM PageBackStackItem WHERE id IN (:ids)")
    suspend fun deletePageBackStackItemsById(ids: List<Long>)

    @Query("DELETE FROM PageBackStackItem")
    suspend fun deleteAll()
}
