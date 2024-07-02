package org.wikipedia.feed.suggestededits

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.descriptions.DescriptionEditActivity
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
        this.age = age
        this.cb = cb

        if (age == 0) {
            // In the background, fetch the user's latest contribution stats, so that we can update whether the
            // Suggested Edits feature is paused or disabled, the next time the feed is refreshed.
            coroutineScope.launch(CoroutineExceptionHandler { _, caught ->
                // Log the exception; will retry next time the feed is refreshed.
                L.e(caught)
            }) {
                UserContribStats.verifyEditCountsAndPauseState()
            }
        }

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
