package org.wikipedia.usercontrib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.UserContribution
import org.wikipedia.settings.Prefs
import java.util.*
import kotlin.collections.flatMap
import kotlin.math.ceil

object UserContribStats {
    private const val REVERT_SEVERITY_PAUSE_THRESHOLD = 5
    private const val REVERT_SEVERITY_DISABLE_THRESHOLD = 7
    private const val PAUSE_DURATION_DAYS = 7

    private var totalEdits: Int = 0
    var totalReverts: Int = 0

    fun verifyEditCountsAndPauseState(totalContributionsList: List<UserContribution>) {
        totalEdits = totalContributionsList.size
        totalReverts = totalContributionsList.count { it.tags.contains("mw-reverted") || it.tags.contains("mw-rollback") }
        maybePauseAndGetEndDate()
    }

    suspend fun getPageViews(homeWikiContributions: List<UserContribution>, wikidataContributions: List<UserContribution>): Long {
        // If the user has contributions in the main namespace on their home wiki, get pageviews from those.
        val mainNamespaceContributions = homeWikiContributions.filter { it.ns == 0 }
        if (mainNamespaceContributions.isNotEmpty()) {
            val pageTitles = mainNamespaceContributions.map { it.title }
            return ServiceFactory.get(WikipediaApp.instance.wikiSite).getPageViewsForTitles(pageTitles.joinToString("|"))
                .query?.pages?.sumOf { it.pageViewsMap.values.sumOf { it ?: 0 } } ?: 0
        }

        // ...otherwise, get pageviews from the Wikidata descriptions they've added.
        val qLangMap = mutableMapOf<String, MutableSet<String>>()

        for (userContribution in wikidataContributions) {
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
                        entity.getSiteLinks()[WikiSite.forLanguageCode(lang).dbName()]?.let {
                            langArticleMap.getOrPut(lang) { mutableListOf() }.add(it.title)
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

    fun maybePauseAndGetEndDate(): Date? {
        val pauseDate = Prefs.suggestedEditsPauseDate
        var pauseEndDate: Date? = null

        // Are we currently in a pause period?
        if (pauseDate.time != 0L) {
            val cal = Calendar.getInstance()
            cal.time = pauseDate
            cal.add(Calendar.DAY_OF_YEAR, PAUSE_DURATION_DAYS)
            pauseEndDate = cal.time

            if (Date().after((pauseEndDate))) {
                // We've exceeded the pause period, so remove it.
                Prefs.suggestedEditsPauseDate = Date(0)
                pauseEndDate = null
            }
        }

        if (getRevertSeverity() > REVERT_SEVERITY_PAUSE_THRESHOLD) {
            // Do we need to impose a new pause?
            if (totalReverts > Prefs.suggestedEditsPauseReverts) {
                val cal = Calendar.getInstance()
                cal.time = Date()
                Prefs.suggestedEditsPauseDate = cal.time
                Prefs.suggestedEditsPauseReverts = totalReverts

                cal.add(Calendar.DAY_OF_YEAR, PAUSE_DURATION_DAYS)
                pauseEndDate = cal.time
            }
        }
        return pauseEndDate
    }
}
