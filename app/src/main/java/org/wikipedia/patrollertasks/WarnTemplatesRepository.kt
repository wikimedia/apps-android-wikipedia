package org.wikipedia.patrollertasks

import org.wikipedia.patrollertasks.db.WarnTemplate
import org.wikipedia.patrollertasks.db.WarnTemplateDao

class WarnTemplatesRepository constructor(private val warnTemplateDao: WarnTemplateDao) {

    suspend fun getAllWarnTemplates() = warnTemplateDao.getAllWarnTemplates()

    suspend fun insertWarnTemplate(warnTemplate: WarnTemplate) {
        warnTemplateDao.insertWarnTemplate(warnTemplate)
    }

    suspend fun updateWarnTemplate(warnTemplate: WarnTemplate) {
        warnTemplateDao.updateWarnTemplate(warnTemplate)
    }

    suspend fun updateWarnTemplates(warnTemplates: List<WarnTemplate>) {
        warnTemplateDao.updateWarnTemplates(warnTemplates)
    }

    suspend fun deleteWarnTemplates(warnTemplate: WarnTemplate) {
        warnTemplateDao.deleteWarnTemplate(warnTemplate)
    }
}
