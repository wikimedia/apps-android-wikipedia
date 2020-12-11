package org.wikipedia.feed.suggestededits

import android.content.Context
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.userprofile.UserContributionsStats
import java.util.*

class SuggestedEditsFeedClient : FeedClient {
    interface Callback {
        fun updateCardContent(card: SuggestedEditsCard)
    }

    private var age: Int = 0
    private val disposables = CompositeDisposable()

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        this.age = age
        cancel()

        if (age == 0) {
            // In the background, fetch the user's latest contribution stats, so that we can update whether the
            // Suggested Edits feature is paused or disabled, the next time the feed is refreshed.
            UserContributionsStats.updateStatsInBackground()
        }

        if (UserContributionsStats.isDisabled() || UserContributionsStats.maybePauseAndGetEndDate() != null) {
            FeedCoordinator.postCardsToCallback(cb, Collections.emptyList())
            return
        }
        FeedCoordinator.postCardsToCallback(cb, listOf(SuggestedEditsCard(wiki, age)))
    }

    override fun cancel() {
        disposables.clear()
    }
}
