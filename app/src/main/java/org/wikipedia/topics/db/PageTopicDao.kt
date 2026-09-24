package org.wikipedia.topics.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.db.HistoryEntryWithImage

@Dao
interface PageTopicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topics: List<PageTopic>)

    @Query("SELECT * FROM PageTopic WHERE lang = :lang AND namespace = :namespace AND apiTitle = :apiTitle ORDER BY score DESC")
    suspend fun findTopicsFor(lang: String, namespace: String, apiTitle: String): List<PageTopic>

    @Query("DELETE FROM PageTopic WHERE lang = :lang AND namespace = :namespace AND apiTitle = :apiTitle")
    suspend fun deleteTopicsFor(lang: String, namespace: String, apiTitle: String)

    @Query("DELETE FROM PageTopic")
    suspend fun deleteAll()

    @Query("SELECT PageTopic.topic AS topic, COUNT(*) AS articleCount, CAST(SUM(COALESCE(PageImage.timeSpentSec, 0)) AS INTEGER) AS timeSpentSec FROM" +
            "  (SELECT DISTINCT lang, namespace, apiTitle FROM HistoryEntry WHERE timestamp BETWEEN :startMillis AND :endMillis) AS ReadArticles" +
            "  INNER JOIN PageTopic ON (PageTopic.lang = ReadArticles.lang AND PageTopic.namespace = ReadArticles.namespace AND PageTopic.apiTitle = ReadArticles.apiTitle)" +
            "  LEFT OUTER JOIN PageImage ON (PageImage.lang = ReadArticles.lang AND PageImage.namespace = ReadArticles.namespace AND PageImage.apiTitle = ReadArticles.apiTitle)" +
            "  GROUP BY PageTopic.topic ORDER BY articleCount DESC LIMIT :limit")
    suspend fun getTopTopicsByArticleCount(startMillis: Long, endMillis: Long = System.currentTimeMillis(), limit: Int = 10): List<TopicReadStats>

    @Query("SELECT PageTopic.topic AS topic, COUNT(*) AS articleCount, CAST(SUM(COALESCE(PageImage.timeSpentSec, 0)) AS INTEGER) AS timeSpentSec FROM" +
            "  (SELECT DISTINCT lang, namespace, apiTitle FROM HistoryEntry WHERE timestamp BETWEEN :startMillis AND :endMillis) AS ReadArticles" +
            "  INNER JOIN PageTopic ON (PageTopic.lang = ReadArticles.lang AND PageTopic.namespace = ReadArticles.namespace AND PageTopic.apiTitle = ReadArticles.apiTitle)" +
            "  LEFT OUTER JOIN PageImage ON (PageImage.lang = ReadArticles.lang AND PageImage.namespace = ReadArticles.namespace AND PageImage.apiTitle = ReadArticles.apiTitle)" +
            "  GROUP BY PageTopic.topic ORDER BY timeSpentSec DESC LIMIT :limit")
    suspend fun getTopTopicsByTimeSpent(startMillis: Long, endMillis: Long = System.currentTimeMillis(), limit: Int = 10): List<TopicReadStats>

    @Query("SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec FROM HistoryEntry" +
            "  INNER JOIN PageTopic ON (HistoryEntry.lang = PageTopic.lang AND HistoryEntry.namespace = PageTopic.namespace AND HistoryEntry.apiTitle = PageTopic.apiTitle)" +
            "  LEFT OUTER JOIN PageImage ON (HistoryEntry.lang = PageImage.lang AND HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle)" +
            "  INNER JOIN (SELECT lang, namespace, apiTitle, MAX(timestamp) AS max_timestamp FROM HistoryEntry WHERE timestamp BETWEEN :startMillis AND :endMillis GROUP BY lang, namespace, apiTitle) LatestEntries" +
            "    ON (HistoryEntry.lang = LatestEntries.lang AND HistoryEntry.namespace = LatestEntries.namespace AND HistoryEntry.apiTitle = LatestEntries.apiTitle AND HistoryEntry.timestamp = LatestEntries.max_timestamp)" +
            "  WHERE PageTopic.topic = :topic AND HistoryEntry.timestamp BETWEEN :startMillis AND :endMillis" +
            "  ORDER BY HistoryEntry.timestamp DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    suspend fun findEntriesByTopic(topic: String, startMillis: Long = 0, endMillis: Long = System.currentTimeMillis(), limit: Int = 100): List<HistoryEntryWithImage>

    @Transaction
    suspend fun upsertForPage(entry: HistoryEntry, topics: List<PageTopic>) {
        deleteTopicsFor(entry.lang, entry.namespace, entry.apiTitle)
        if (topics.isNotEmpty()) {
            insertAll(topics)
        }
    }
}
