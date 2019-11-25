package org.wikipedia.suggestededits

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.settings.Prefs
import java.util.*
import kotlin.math.ceil

object SuggestedEditsUserStats {
    private const val REVERT_SEVERITY_PAUSE_THRESHOLD = 5
    private const val REVERT_SEVERITY_DISABLE_THRESHOLD = 7
    private const val PAUSE_DURATION_DAYS = 7

    var totalEdits: Int = 0
    var totalReverts: Int = 0

    fun getEditCountsObservable(): Observable<MwQueryResponse> {
        return Observable.zip(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).editorTaskCounts, ServiceFactory.get(WikiSite(Service.COMMONS_URL)).editorTaskCounts,
                BiFunction<MwQueryResponse, MwQueryResponse, MwQueryResponse> { wikidataResponse, commonsResponse ->
                    // If the user is blocked on Commons, then boil up the Commons response, otherwise
                    // pass back the Wikidata response, which will be checked for blocking anyway.
                    if (commonsResponse.query()!!.userInfo()!!.isBlocked) commonsResponse else wikidataResponse
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    val editorTaskCounts = it.query()!!.editorTaskCounts()!!
                    totalEdits = editorTaskCounts.totalEdits
                    totalReverts = editorTaskCounts.totalReverts
                    maybePauseAndGetEndDate()
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

