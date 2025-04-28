package org.wikipedia.feed.suggestededits

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditUtil
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
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
        val cardTypes = JsonUtil.decodeFromString<MutableList<DescriptionEditActivity.Action>>(Prefs.suggestedEditsFeedCardTypes) ?: mutableListOf()
        if (cardTypes.isEmpty()) {
            clientJob = coroutineScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
                L.e(throwable)
                cb.error(throwable)
            }) {
                val homeSiteCall = async { ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserContributions(AccountUtil.userName, 50, null, null) }
                val homeSiteResponse = homeSiteCall.await()
                var totalContributions = homeSiteResponse.query?.userInfo?.editCount ?: 0

                val usesLocalDescriptions = DescriptionEditUtil.wikiUsesLocalDescriptions(WikipediaApp.instance.wikiSite.languageCode)
                val sufficientContributionsForArticleDescription = totalContributions > (if (usesLocalDescriptions) 50 else 3)
                if (sufficientContributionsForArticleDescription) {
                    cardTypes.add(DescriptionEditActivity.Action.ADD_DESCRIPTION)
                }
                cardTypes.add(DescriptionEditActivity.Action.ADD_CAPTION)
                cardTypes.add(DescriptionEditActivity.Action.ADD_IMAGE_TAGS)
                withContext(Dispatchers.Main) {
                    Prefs.suggestedEditsFeedCardTypes = JsonUtil.encodeToString(cardTypes).orEmpty()
                    cb.success(listOf(SuggestedEditsCard(cardTypes, wiki, age)))
                }
            }
        } else {
            cb.success(listOf(SuggestedEditsCard(cardTypes, wiki, age)))
        }
    }

    override fun cancel() {
        clientJob?.cancel()
    }
}
