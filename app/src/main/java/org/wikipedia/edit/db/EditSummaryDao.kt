package org.wikipedia.edit.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EditSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEditSummary(summary: EditSummary)

    @Query("SELECT * FROM EditSummary ORDER BY lastUsed DESC")
    suspend fun getEditSummaries(): List<EditSummary>

    @Query("DELETE FROM EditSummary")
    suspend fun deleteAll()
}
