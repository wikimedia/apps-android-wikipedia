package org.wikipedia.pageimages.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PageImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageImage(pageImage: PageImage)

    @Query("SELECT * FROM PageImage")
    fun getAllPageImages(): List<PageImage>
}
