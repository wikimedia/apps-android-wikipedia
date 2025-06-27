package org.wikipedia.feed.suggestededits

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditUtil
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.usercontrib.UserContribStats
import org.wikipedia.util.log.L

class SuggestedEditsFeedClient(
    private val coroutineScope: CoroutineScope
) : FeedClient {

    private lateinit var cb: FeedClient.Callback
    private var age: Int = 0
    private var clientJob: Job? = null

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        clientJob?.cancel()
        this.age = age
        this.cb = cb

        if (UserContribStats.isDisabled() || UserContribStats.maybePauseAndGetEndDate() != null) {
            cb.success(emptyList())
            return
        }

        if (age == 0) {
            cardTypes.clear()
            clientJob = coroutineScope.launch(CoroutineExceptionHandler { _, throwable ->
                L.e(throwable)
                cb.error(throwable)
            }) {
                val homeSiteCall = ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserContributions(AccountUtil.userName, 50, null, null)
                val totalContributions = homeSiteCall.query?.userInfo?.editCount ?: 0

                val usesLocalDescriptions = DescriptionEditUtil.wikiUsesLocalDescriptions(WikipediaApp.instance.wikiSite.languageCode)
                val sufficientContributionsForArticleDescription = totalContributions > (if (usesLocalDescriptions) 50 else 3)
                if (sufficientContributionsForArticleDescription) {
                    cardTypes.add(DescriptionEditActivity.Action.ADD_DESCRIPTION)
                }
                cardTypes.add(DescriptionEditActivity.Action.ADD_CAPTION)
                cardTypes.add(DescriptionEditActivity.Action.ADD_IMAGE_TAGS)
                cb.success(listOf(SuggestedEditsCard(cardTypes, wiki, age)))
            }
        } else {
            cb.success(listOf(SuggestedEditsCard(cardTypes, wiki, age)))
        }
    }

    override fun cancel() {
        clientJob?.cancel()
    }

    companion object {
        private val cardTypes = mutableListOf<DescriptionEditActivity.Action>()
    }
}
