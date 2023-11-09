package org.wikipedia.talk.db

import androidx.room.*

@Dao
interface TalkTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(talkTemplate: TalkTemplate)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTemplate(talkTemplate: TalkTemplate)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTemplates(list: List<TalkTemplate>)

    @Query("SELECT * FROM TalkTemplate ORDER BY `order`")
    suspend fun getAllTemplates(): List<TalkTemplate>

    @Query("SELECT * FROM TalkTemplate WHERE `id` IN (:id)")
    suspend fun getTemplateById(id: Int): TalkTemplate?

    @Query("SELECT `order` FROM TalkTemplate WHERE `order` ORDER BY `order` DESC LIMIT 1")
    suspend fun getLastOrderNumber(): Int?

    @Delete
    suspend fun deleteTemplates(talkTemplates: List<TalkTemplate>)
}
