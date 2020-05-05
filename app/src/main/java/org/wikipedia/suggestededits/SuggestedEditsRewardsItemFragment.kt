package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_rewards_item.*
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L
import java.util.concurrent.TimeUnit

class SuggestedEditsRewardsItemFragment : SuggestedEditsItemFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_rewards_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cardItemErrorView.setBackClickListener { requireActivity().finish() }
        cardItemErrorView.setRetryClickListener {
            fetchUserContribution()
        }
        fetchUserContribution()
    }

    private fun fetchUserContribution() {
        L.d("SE rewards fetchUserContribution")
        contentContainer.visibility = View.VISIBLE
        cardItemProgressBar.visibility = View.VISIBLE
        cardItemErrorView.visibility = View.GONE
        disposables.add(SuggestedEditsUserStats.getEditCountsObservable()
                .doAfterTerminate { Prefs.setSuggestedEditsRewardInterstitialEnabled(false) }
                .subscribe({ response ->
                    cardItemProgressBar.visibility = View.GONE
                    val editorTaskCounts = response.query()!!.editorTaskCounts()!!
                    val day = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - editorTaskCounts.lastEditDate.time).toInt()

                    L.d("SE rewards fetchUserContribution day $day")
                    if (editorTaskCounts.totalEdits == CONTRIBUTION_INITIAL_COUNT || editorTaskCounts.totalEdits % CONTRIBUTION_COUNT == 0) {
                        rewardImage.setImageResource(R.drawable.ic_illustration_heart)
                        rewardText.text = getString(R.string.suggested_edits_rewards_contribution, editorTaskCounts.totalEdits)
                    } else if (editorTaskCounts.editStreak % EDIT_STREAK_COUNT == 0) {
                        rewardImage.setImageResource(R.drawable.ic_illustration_calendar)
                        rewardText.text = getString(R.string.suggested_edits_rewards_edit_streak, editorTaskCounts.editStreak, AccountUtil.getUserName())
                    } else if (day == EDIT_QUALITY_ON_DAY && SuggestedEditsUserStats.getRevertSeverity() <= EDIT_STREAK_MAX_REVERT_SEVERITY) {
                       when(SuggestedEditsUserStats.getRevertSeverity()) {
                           0 -> {
                               rewardImage.setImageResource(R.drawable.ic_illustration_quality_perfect)
                               rewardText.text = getString(R.string.suggested_edits_rewards_edit_quality, getString(R.string.suggested_edits_quality_perfect_text))
                           }
                           1 -> {
                               rewardImage.setImageResource(R.drawable.ic_illustration_quality_excellent)
                               rewardText.text = getString(R.string.suggested_edits_rewards_edit_quality, getString(R.string.suggested_edits_quality_excellent_text))
                           }
                           2 -> {
                               rewardImage.setImageResource(R.drawable.ic_illustration_quality_very_good)
                               rewardText.text = getString(R.string.suggested_edits_rewards_edit_quality, getString(R.string.suggested_edits_quality_very_good_text))
                           }
                           else -> {
                               rewardImage.setImageResource(R.drawable.ic_illustration_quality_good)
                               rewardText.text = getString(R.string.suggested_edits_rewards_edit_quality, getString(R.string.suggested_edits_quality_good_text))
                           }
                       }
                    } else if (day == PAGEVIEWS_ON_DAY) {
                        cardItemProgressBar.visibility = View.VISIBLE
                        getPageViews()
                    }
                }, { t ->
                    setErrorState(t)
                }))
    }

    private fun getPageViews() {
        val qLangMap = HashMap<String, HashSet<String>>()
        disposables.add(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getUserContributions(AccountUtil.getUserName()!!, 10)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { response ->
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
                    ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getWikidataLabelsAndDescriptions(qLangMap.keys.joinToString("|"))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                }
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
                .subscribe({ pageViewsCount ->
                    rewardImage.setImageResource(R.drawable.ic_illustration_views)
                    rewardText.text = getString(R.string.suggested_edits_rewards_pageviews, pageViewsCount)
                }, { t ->
                    setErrorState(t)
                }))
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        cardItemErrorView.setError(t)
        cardItemErrorView.visibility = View.VISIBLE
        cardItemProgressBar.visibility = View.GONE
        contentContainer.visibility = View.GONE
    }

    companion object {
        private const val CONTRIBUTION_INITIAL_COUNT = 5
        private const val CONTRIBUTION_COUNT = 50
        private const val EDIT_STREAK_COUNT = 5
        private const val EDIT_STREAK_MAX_REVERT_SEVERITY = 3
        private const val EDIT_QUALITY_ON_DAY = 14
        private const val PAGEVIEWS_ON_DAY = 30

        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsRewardsItemFragment()
        }
    }
}
