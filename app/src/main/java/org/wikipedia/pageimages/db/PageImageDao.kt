package org.wikipedia.pageimages.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface PageImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPageImage(pageImage: PageImage)
}
