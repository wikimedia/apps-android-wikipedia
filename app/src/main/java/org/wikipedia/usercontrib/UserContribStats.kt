package org.wikipedia.usercontrib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.settings.Prefs
import java.time.LocalDateTime
import kotlin.math.ceil

object UserContribStats {
    private const val REVERT_SEVERITY_PAUSE_THRESHOLD = 5
    private const val REVERT_SEVERITY_DISABLE_THRESHOLD = 7
    private const val PAUSE_DURATION_DAYS = 7L

    private var totalEdits: Int = 0
    var totalReverts: Int = 0
    var totalDescriptionEdits: Int = 0
    var totalImageCaptionEdits: Int = 0
    var totalImageTagEdits: Int = 0

    suspend fun verifyEditCountsAndPauseState() {
        val response = ServiceFactory.get(Constants.wikidataWikiSite).getEditorTaskCounts()
        if (response.query?.userInfo?.isBlocked != true) {
            response.query?.editorTaskCounts?.let {
                totalEdits = it.totalEdits
                totalDescriptionEdits = it.totalDescriptionEdits
                totalImageCaptionEdits = it.totalImageCaptionEdits
                totalImageTagEdits = it.totalDepictsEdits
                totalReverts = it.totalReverts
                maybePauseAndGetEndDate()
            }
        }
    }

    suspend fun getPageViews(response: MwQueryResponse): Long {
        val qLangMap = mutableMapOf<String, MutableSet<String>>()

        for (userContribution in response.query!!.userContributions) {
            val descLang = userContribution.comment.split(" ")
                .filter { "wbsetdescription" in it }
                .flatMap { it.split("|") }
                .getOrNull(1)
            if (!descLang.isNullOrEmpty()) {
                qLangMap.getOrPut(userContribution.title) { mutableSetOf() }.add(descLang)
            }
        }

        val entities = ServiceFactory.get(Constants.wikidataWikiSite).getWikidataLabelsAndDescriptions(qLangMap.keys.joinToString("|"))
        if (entities.entities.isEmpty()) {
            return 0L
        }

        val langArticleMap = mutableMapOf<String, MutableList<String>>()
        entities.entities.forEach { (entityKey, entity) ->
            for ((qKey, langs) in qLangMap) {
                if (qKey == entityKey) {
                    for (lang in langs) {
                        val dbName = WikiSite.forLanguageCode(lang).dbName()
                        if (entity.sitelinks.containsKey(dbName)) {
                            langArticleMap.getOrPut(lang) { mutableListOf() }.add(entity.sitelinks[dbName]?.title!!)
                        }
                    }
                    break
                }
            }
        }

        withContext(Dispatchers.IO) {
            langArticleMap.map { (key, value) ->
                async { ServiceFactory.get(WikiSite.forLanguageCode(key)).getPageViewsForTitles(value.joinToString("|")) }
            }.awaitAll()
                .mapNotNull { it.query?.pages }
                .flatten()
                .flatMap { it.pageViewsMap.values }
                .sumOf { it ?: 0 }
        }.let {
            return it
        }
    }

    fun getRevertSeverity(): Int {
        return if (totalEdits <= 100) totalReverts else ceil(totalReverts.toFloat() / totalEdits.toFloat() * 100f).toInt()
    }

    fun isDisabled(): Boolean {
        return getRevertSeverity() > REVERT_SEVERITY_DISABLE_THRESHOLD
    }

    fun maybePauseAndGetEndDate(): LocalDateTime? {
        val pauseDateTime = Prefs.suggestedEditsPauseDate
        val now = LocalDateTime.now()
        var pauseEndDateTime: LocalDateTime? = null

        // Are we currently in a pause period?
        if (pauseDateTime != null) {
            pauseEndDateTime = pauseDateTime.plusDays(PAUSE_DURATION_DAYS)

            if (now > pauseEndDateTime) {
                // We've exceeded the pause period, so remove it.
                Prefs.suggestedEditsPauseDate = null
                pauseEndDateTime = null
            }
        }

        if (getRevertSeverity() > REVERT_SEVERITY_PAUSE_THRESHOLD) {
            // Do we need to impose a new pause?
            if (totalReverts > Prefs.suggestedEditsPauseReverts) {
                Prefs.suggestedEditsPauseDate = now
                Prefs.suggestedEditsPauseReverts = totalReverts

                pauseEndDateTime = now.plusDays(PAUSE_DURATION_DAYS)
            }
        }
        return pauseEndDateTime
    }
}
