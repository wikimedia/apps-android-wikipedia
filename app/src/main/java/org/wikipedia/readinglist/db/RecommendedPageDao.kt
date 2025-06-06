package org.wikipedia.readinglist.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.readinglist.database.RecommendedPage

@Dao
interface RecommendedPageDao {
    @Query("SELECT * FROM RecommendedPage WHERE status = 0 ORDER BY timestamp DESC")
    suspend fun getNewRecommendedPages(): List<RecommendedPage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recommendedPages: RecommendedPage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recommendedPages: List<RecommendedPage>)

    @Query("UPDATE RecommendedPage SET status = 1 WHERE status = 0")
    suspend fun expireOldRecommendedPages()

    @Query("SELECT COUNT(*) FROM RecommendedPage WHERE apiTitle = :apiTitle AND wiki = :wiki")
    suspend fun findIfAny(apiTitle: String, wiki: WikiSite): Int

    @Query("DELETE FROM RecommendedPage")
    suspend fun deleteAll()

    // find if any recommended page exists
    @Query("SELECT * FROM RecommendedPage WHERE id = 0")
    fun findIfAny(): RecommendedPage?
}
