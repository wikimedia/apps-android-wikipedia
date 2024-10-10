package org.wikipedia.feed.suggestededits

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.descriptions.DescriptionEditActivity
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

        cb.success(listOf(SuggestedEditsCard(listOf(
            DescriptionEditActivity.Action.ADD_DESCRIPTION,
            DescriptionEditActivity.Action.ADD_CAPTION,
            DescriptionEditActivity.Action.ADD_IMAGE_TAGS
        ), wiki, age)))
    }

    override fun cancel() {
        clientJob?.cancel()
    }
}
