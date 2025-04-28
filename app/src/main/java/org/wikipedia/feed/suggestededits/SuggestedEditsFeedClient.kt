package org.wikipedia.feed.suggestededits

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditUtil
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.usercontrib.UserContribStats

class SuggestedEditsFeedClient(
    private val coroutineScope: CoroutineScope
) : FeedClient {

    private lateinit var cb: FeedClient.Callback
    private var age: Int = 0
    private var clientJob: Job? = null

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        this.age = age
        this.cb = cb

        if (UserContribStats.isDisabled() || UserContribStats.maybePauseAndGetEndDate() != null) {
            cb.success(emptyList())
            return
        }
        val cardTypes = mutableListOf(
            DescriptionEditActivity.Action.ADD_CAPTION,
            DescriptionEditActivity.Action.ADD_IMAGE_TAGS
        )
        coroutineScope.launch(Dispatchers.IO) {
            val homeSiteCall = async { ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserContributions(AccountUtil.userName, 50, null, null) }
            // val homeSiteParamCall = async { ServiceFactory.get(WikipediaApp.instance.wikiSite).getParamInfo("query+growthtasks") }
            val commonsCall =  async  { ServiceFactory.get(Constants.commonsWikiSite).getUserContributions(AccountUtil.userName, 10, null, null) }
            val wikidataCall = async  { ServiceFactory.get(Constants.wikidataWikiSite).getUserContributions(AccountUtil.userName, 10, 0, null) }
            val homeSiteResponse = homeSiteCall.await()
            val commonsResponse = commonsCall.await()
            val wikidataResponse = wikidataCall.await()

            var totalContributions = homeSiteResponse.query?.userInfo?.editCount ?: 0
            totalContributions += commonsResponse.query?.userInfo?.editCount ?: 0
            totalContributions += wikidataResponse.query?.userInfo?.editCount ?: 0

            val usesLocalDescriptions = DescriptionEditUtil.wikiUsesLocalDescriptions(WikipediaApp.instance.wikiSite.languageCode)
            val sufficientContributionsForArticleDescription = totalContributions > (if (usesLocalDescriptions) 50 else 3)
            if (sufficientContributionsForArticleDescription) {
                cardTypes.add(DescriptionEditActivity.Action.ADD_DESCRIPTION)
            }
            withContext(Dispatchers.Main) {
                cb.success(listOf(SuggestedEditsCard(cardTypes, wiki, age)))
            }
        }
    }

    override fun cancel() {
        clientJob?.cancel()
    }
}
