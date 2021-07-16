package org.wikipedia.userprofile

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.settings.Prefs
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.ceil

object UserContributionsStats {
    private const val REVERT_SEVERITY_PAUSE_THRESHOLD = 5
    private const val REVERT_SEVERITY_DISABLE_THRESHOLD = 7
    private const val PAUSE_DURATION_DAYS = 7

    private var totalEdits: Int = 0
    var totalReverts: Int = 0
    var totalDescriptionEdits: Int = 0
    var totalImageCaptionEdits: Int = 0
    var totalImageTagEdits: Int = 0

    fun getEditCountsObservable(): Observable<MwQueryResponse> {
        return ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).editorTaskCounts
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    if (it.query?.userInfo()?.isBlocked != true) {
                        val editorTaskCounts = it.query?.editorTaskCounts()!!
                        totalEdits = editorTaskCounts.totalEdits
                        totalDescriptionEdits = editorTaskCounts.totalDescriptionEdits
                        totalImageCaptionEdits = editorTaskCounts.totalImageCaptionEdits
                        totalImageTagEdits = editorTaskCounts.totalDepictsEdits
                        totalReverts = editorTaskCounts.totalReverts
                        maybePauseAndGetEndDate()
                    }
                }
    }

    fun getPageViewsObservable(localDescriptionsContributions: List<String>): Observable<Long> {
        return ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getUserContributions(AccountUtil.userName!!, 10, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { response ->
                    getPageViewsObservable(response, localDescriptionsContributions)
                }
    }

    fun getPageViewsObservable(response: MwQueryResponse, localDescriptionsContributions: List<String>): Observable<Long> {
        val qLangMap = HashMap<String, HashSet<String>>()

        for (userContribution in response.query!!.userContributions()) {
            val descLang = userContribution.comment.split(" ")
                    .filter { "wbsetdescription" in it }
                    .flatMap { it.split("|") }
                    .getOrNull(1)
            if (descLang.isNullOrEmpty()) {
                continue
            }

            qLangMap.getOrPut(userContribution.title, { HashSet() }).add(descLang)
        }

        return ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getWikidataLabelsAndDescriptions(qLangMap.keys.joinToString("|"))
                .subscribeOn(Schedulers.io())
                .flatMap { entities ->
                    if (entities.entities.isEmpty()) {
                        return@flatMap Observable.just(0L)
                    }
                    val langArticleMap = HashMap<String, ArrayList<String>>()
                    entities.entities.forEach { (entityKey, entity) ->
                        for (qKey in qLangMap.keys) {
                            if (qKey == entityKey) {
                                for (lang in qLangMap[qKey]!!) {
                                    val dbName = WikiSite.forLanguageCode(lang).dbName()
                                    if (entity.sitelinks.containsKey(dbName)) {
                                        langArticleMap.getOrPut(lang, { ArrayList() })
                                                .add(entity.sitelinks[dbName]?.title!!)
                                    }
                                }
                                break
                            }
                        }
                    }

                    // TODO: support multiple local descriptions
                    langArticleMap.getOrPut("en", { ArrayList() }).addAll(localDescriptionsContributions)

                    val observableList = langArticleMap.map { (key, value) ->
                        val site = WikiSite.forLanguageCode(key)
                        ServiceFactory.get(site).getPageViewsForTitles(value.joinToString("|"))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                    }

                    Observable.zip(observableList) { resultList ->
                        resultList.filterIsInstance<MwQueryResponse>()
                                .mapNotNull { it.query }
                                .flatMap { it.pages()!! }
                                .flatMap { it.pageViewsMap.values }
                                .sumOf { it ?: 0 }
                    }
                }
    }

    fun updateStatsInBackground() {
        getEditCountsObservable().subscribe()
    }

    fun getRevertSeverity(): Int {
        return if (totalEdits <= 100) totalReverts else ceil(totalReverts.toFloat() / totalEdits.toFloat() * 100f).toInt()
    }

    fun isDisabled(): Boolean {
        return getRevertSeverity() > REVERT_SEVERITY_DISABLE_THRESHOLD
    }

    fun maybePauseAndGetEndDate(): Date? {
        val pauseDate = Prefs.getSuggestedEditsPauseDate()
        var pauseEndDate: Date? = null

        // Are we currently in a pause period?
        if (pauseDate.time != 0L) {
            val cal = Calendar.getInstance()
            cal.time = pauseDate
            cal.add(Calendar.DAY_OF_YEAR, PAUSE_DURATION_DAYS)
            pauseEndDate = cal.time

            if (Date().after((pauseEndDate))) {
                // We've exceeded the pause period, so remove it.
                Prefs.setSuggestedEditsPauseDate(Date(0))
                pauseEndDate = null
            }
        }

        if (getRevertSeverity() > REVERT_SEVERITY_PAUSE_THRESHOLD) {
            // Do we need to impose a new pause?
            if (totalReverts > Prefs.getSuggestedEditsPauseReverts()) {
                val cal = Calendar.getInstance()
                cal.time = Date()
                Prefs.setSuggestedEditsPauseDate(cal.time)
                Prefs.setSuggestedEditsPauseReverts(totalReverts)

                cal.add(Calendar.DAY_OF_YEAR, PAUSE_DURATION_DAYS)
                pauseEndDate = cal.time
            }
        }
        return pauseEndDate
    }
}
