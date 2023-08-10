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

    @Delete
    suspend fun deleteWarnTemplate(warnTemplate: WarnTemplate)
}
