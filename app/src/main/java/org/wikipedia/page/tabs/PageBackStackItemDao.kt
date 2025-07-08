package org.wikipedia.page.tabs

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PageBackStackItemDao {

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertPageBackStackItem(item: PageBackStackItem)

    @Query("SELECT * FROM PageBackStackItem WHERE tabId = :tabId ORDER BY timestamp DESC")
    suspend fun getPageBackStackItems(tabId: Long): List<PageBackStackItem>

    @Delete
    suspend fun deletePageBackStackItem(item: PageBackStackItem)
}
