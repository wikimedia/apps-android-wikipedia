package org.wikipedia.edit.db

import androidx.room.*
import io.reactivex.rxjava3.core.Single

@Dao
interface EditSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEditSummary(summary: EditSummary): Single<Unit>

    @Query("SELECT * FROM editsummaries ORDER BY lastUsed DESC")
    fun getEditSummaries(): Single<List<EditSummary>>

    @Query("DELETE FROM editsummaries")
    fun deleteAll(): Single<Unit>
}
