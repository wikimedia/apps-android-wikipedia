package org.wikipedia.pageimages.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.wikipedia.history.HistoryEntry

@Dao
interface PageImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPageImageSync(pageImage: PageImage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageImage(pageImage: PageImage)

    @Query("SELECT * FROM PageImage")
    fun getAllPageImages(): List<PageImage>

    @Query("SELECT * FROM PageImage WHERE lang = :lang AND namespace = :namespace AND apiTitle = :apiTitle")
    suspend fun findItemsBy(lang: String, namespace: String, apiTitle: String): List<PageImage>

    @Transaction
    suspend fun upsertForTimeSpent(entry: HistoryEntry, timeSpent: Int) {
        val items = findItemsBy(entry.lang, entry.namespace, entry.apiTitle)
        if (items.isNotEmpty()) {
            items.forEach {
                it.timeSpentSec += timeSpent
                insertPageImage(it)
            }
        } else {
            insertPageImage(PageImage(entry.lang, entry.namespace, entry.apiTitle, entry.title.thumbUrl,
                entry.title.description, timeSpent))
        }
    }

    @Transaction
    suspend fun upsertForMetadata(entry: HistoryEntry, imageName: String?, description: String?, geoLat: Double?, geoLon: Double?) {
        val items = findItemsBy(entry.lang, entry.namespace, entry.apiTitle)
        if (items.isNotEmpty()) {
            items.forEach {
                it.imageName = imageName
                it.description = description
                it.geoLat = geoLat ?: 0.0
                it.geoLon = geoLon ?: 0.0
                insertPageImage(it)
            }
        } else {
            insertPageImage(PageImage(entry.lang, entry.namespace, entry.apiTitle, imageName, description,
                0, geoLat ?: 0.0, geoLon ?: 0.0))
        }
    }
}
