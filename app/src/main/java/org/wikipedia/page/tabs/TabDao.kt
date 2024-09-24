package org.wikipedia.page.tabs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TabDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: Tab)

    @Query("SELECT * FROM Tab")
    suspend fun getTabs(): List<Tab>

    @Query("DELETE FROM Tab")
    suspend fun deleteAll()

    @Query("DELETE FROM Tab WHERE id = :id")
    suspend fun deleteBy(id: Long)

    suspend fun delete(tab: Tab) {
        deleteBy(tab.id)
    }
}
