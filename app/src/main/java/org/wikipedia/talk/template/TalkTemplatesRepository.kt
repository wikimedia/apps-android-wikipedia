package org.wikipedia.talk.template

import org.wikipedia.talk.db.TalkTemplate
import org.wikipedia.talk.db.TalkTemplateDao

class TalkTemplatesRepository(private val talkTemplateDao: TalkTemplateDao) {

    suspend fun getAllTemplates() = talkTemplateDao.getAllTemplates()

    suspend fun insertTemplate(talkTemplate: TalkTemplate) {
        talkTemplateDao.insertTemplate(talkTemplate)
    }

    suspend fun getLastOrderNumber(): Int {
        return talkTemplateDao.getLastOrderNumber() ?: 0
    }

    suspend fun updateTemplate(talkTemplate: TalkTemplate) {
        talkTemplateDao.updateTemplate(talkTemplate)
    }

    suspend fun updateTemplates(talkTemplates: List<TalkTemplate>) {
        talkTemplateDao.updateTemplates(talkTemplates)
    }

    suspend fun deleteTemplates(talkTemplates: List<TalkTemplate>) {
        talkTemplateDao.deleteTemplates(talkTemplates.map { it.id })
    }
}
