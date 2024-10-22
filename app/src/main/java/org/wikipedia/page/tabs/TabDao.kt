package org.wikipedia.page.tabs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
interface TabDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabs(tabs: List<Tab>)

    @Query("SELECT * FROM Tab")
    suspend fun getTabs(): List<Tab>

    @Query("DELETE FROM Tab")
    suspend fun deleteAll()

    suspend fun hasTabs(): Boolean {
        return withContext(Dispatchers.IO) {
            getTabs().isNotEmpty()
        }
    }
}
