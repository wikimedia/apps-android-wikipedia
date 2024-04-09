package org.wikipedia.feed

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.aggregated.AggregatedFeedContentClient
import org.wikipedia.feed.announcement.AnnouncementClient
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.offline.OfflineCardClient
import org.wikipedia.feed.onboarding.OnboardingClient
import org.wikipedia.feed.searchbar.SearchClient

class FeedCoordinator internal constructor(context: Context) : FeedCoordinatorBase(context) {

    private val aggregatedClient = AggregatedFeedContentClient()

    init {
        FeedContentType.restoreState()
    }

    override fun reset() {
        super.reset()
        aggregatedClient.invalidate()
    }

    override fun buildScript(age: Int) {
        val online = WikipediaApp.instance.isOnline
        conditionallyAddPendingClient(SearchClient(), age == 0)
        conditionallyAddPendingClient(AnnouncementClient(), age == 0 && online)
        conditionallyAddPendingClient(OnboardingClient(), age == 0)
        conditionallyAddPendingClient(OfflineCardClient(), age == 0 && !online)

        for (contentType in FeedContentType.entries.sortedBy { it.order }) {
            addPendingClient(contentType.newClient(aggregatedClient, age))
        }
    }

    companion object {
        fun postCardsToCallback(cb: FeedClient.Callback, cards: List<Card>) {
            CoroutineScope(Dispatchers.Default).launch {
                delay(150L)
                withContext(Dispatchers.Main) {
                    cb.success(cards)
                }
            }
        }
    }
}
