package org.wikipedia.patrollertasks.db

import androidx.room.*

@Dao
interface WarnTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarnTemplate(warnTemplate: WarnTemplate)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWarnTemplate(warnTemplate: WarnTemplate)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWarnTemplates(list: List<WarnTemplate>)

    @Query("SELECT * FROM WarnTemplate")
    suspend fun getAllWarnTemplates(): List<WarnTemplate>

    @Query("SELECT * FROM WarnTemplate WHERE `id` IN (:id)")
    suspend fun getWarnTemplateById(id: Int): WarnTemplate?

    @Query("SELECT `order` FROM WarnTemplate WHERE `order` ORDER BY `order` DESC LIMIT 1")
    suspend fun getLastOrderNumber(): Int?

    @Delete
    suspend fun deleteWarnTemplate(warnTemplate: WarnTemplate)
}
