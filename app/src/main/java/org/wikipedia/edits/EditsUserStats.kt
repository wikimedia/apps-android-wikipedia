package org.wikipedia.edits

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
import kotlin.math.ceil

object EditsUserStats {
    private const val REVERT_SEVERITY_PAUSE_THRESHOLD = 5
    private const val REVERT_SEVERITY_DISABLE_THRESHOLD = 7
    private const val PAUSE_DURATION_DAYS = 7

    var totalEdits: Int = 0
    var totalDescriptionEdits: Int = 0
    var totalImageCaptionEdits: Int = 0
    var totalImageTagEdits: Int = 0
    var totalReverts: Int = 0

    fun getEditCountsObservable(): Observable<MwQueryResponse> {
        return ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).editorTaskCounts
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    if (!it.query()!!.userInfo()!!.isBlocked) {
                        val editorTaskCounts = it.query()!!.editorTaskCounts()!!
                        totalEdits = editorTaskCounts.totalEdits
                        totalDescriptionEdits = editorTaskCounts.totalDescriptionEdits
                        totalImageCaptionEdits = editorTaskCounts.totalImageCaptionEdits
                        totalImageTagEdits = editorTaskCounts.totalDepictsEdits
                        totalReverts = editorTaskCounts.totalReverts
                        maybePauseAndGetEndDate()
                    }
                }
    }

    fun getPageViewsObservable(): Observable<Long> {
        return ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getUserContributions(AccountUtil.getUserName()!!, 10, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { response ->
                    getPageViewsObservable(response)
                }
    }

    fun getPageViewsObservable(response: MwQueryResponse): Observable<Long> {
        val qLangMap = HashMap<String, HashSet<String>>()

        for (userContribution in response.query()!!.userContributions()) {
            var descLang = ""
            val strArr = userContribution.comment.split(" ")
            for (str in strArr) {
                if (str.contains("wbsetdescription")) {
                    val descArr = str.split("|")
                    if (descArr.size > 1) {
                        descLang = descArr[1]
                        break
                    }
                }
            }
            if (descLang.isEmpty()) {
                continue
            }

            if (!qLangMap.containsKey(userContribution.title)) {
                qLangMap[userContribution.title] = HashSet()
            }
            qLangMap[userContribution.title]!!.add(descLang)
        }

        return ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getWikidataLabelsAndDescriptions(qLangMap.keys.joinToString("|"))
                .subscribeOn(Schedulers.io())
                .flatMap {
                    if (it.entities().isEmpty()) {
                        return@flatMap Observable.just(0L)
                    }
                    val langArticleMap = HashMap<String, ArrayList<String>>()
                    for (entityKey in it.entities().keys) {
                        val entity = it.entities()[entityKey]!!
                        for (qKey in qLangMap.keys) {
                            if (qKey == entityKey) {
                                for (lang in qLangMap[qKey]!!) {
                                    val dbName = WikiSite.forLanguageCode(lang).dbName()
                                    if (entity.sitelinks().containsKey(dbName)) {
                                        if (!langArticleMap.containsKey(lang)) {
                                            langArticleMap[lang] = ArrayList()
                                        }
                                        langArticleMap[lang]!!.add(entity.sitelinks()[dbName]!!.title)
                                    }
                                }
                                break
                            }
                        }
                    }

                    val observableList = ArrayList<Observable<MwQueryResponse>>()

                    for (lang in langArticleMap.keys) {
                        val site = WikiSite.forLanguageCode(lang)
                        observableList.add(ServiceFactory.get(site).getPageViewsForTitles(langArticleMap[lang]!!.joinToString("|"))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread()))
                    }

                    Observable.zip(observableList) { resultList ->
                        var totalPageViews = 0L
                        for (result in resultList) {
                            if (result is MwQueryResponse && result.query() != null) {
                                for (page in result.query()!!.pages()!!) {
                                    for (day in page.pageViewsMap.values) {
                                        totalPageViews += day ?: 0
                                    }
                                }
                            }
                        }
                        totalPageViews
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

