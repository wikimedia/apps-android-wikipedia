package org.wikipedia.edit.db

import androidx.room.*
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface EditSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEditSummary(summary: EditSummary): Completable

    @Query("SELECT * FROM EditSummary ORDER BY lastUsed DESC")
    fun getEditSummaries(): Single<List<EditSummary>>

    @Query("DELETE FROM EditSummary")
    fun deleteAll(): Completable
}
