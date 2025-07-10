package org.wikipedia.page.tabs

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
interface TabDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabs(tabs: List<Tab>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: Tab)

    @Query("SELECT * FROM Tab")
    suspend fun getTabs(): List<Tab>

    @Query("DELETE FROM Tab")
    suspend fun deleteAll()

    @Delete
    suspend fun deleteTabs(tabs: List<Tab>)

    @Update
    suspend fun updateTab(tab: Tab)

    @Update
    suspend fun updateTabs(tabs: List<Tab>)

    @Delete
    suspend fun deleteTab(tab: Tab)

    suspend fun hasTabs(): Boolean {
        return withContext(Dispatchers.IO) {
            getTabs().isNotEmpty()
        }
    }
}
