package org.wikipedia.feed

import android.content.Context
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
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
        val online = WikipediaApp.getInstance().isOnline
        conditionallyAddPendingClient(SearchClient(), age == 0)
        conditionallyAddPendingClient(AnnouncementClient(), age == 0 && online)
        conditionallyAddPendingClient(OnboardingClient(), age == 0)
        conditionallyAddPendingClient(OfflineCardClient(), age == 0 && !online)

        val orderedContentTypes = mutableListOf<FeedContentType>()
        orderedContentTypes.addAll(FeedContentType.values())
        orderedContentTypes.sortWith { a, b -> a.order.compareTo(b.order) }
        for (contentType in orderedContentTypes) {
            addPendingClient(contentType.newClient(aggregatedClient, age))
        }
    }

    companion object {
        @JvmStatic
        fun postCardsToCallback(cb: FeedClient.Callback, cards: List<Card?>) {
            Completable.fromAction {
                val delayMillis = 150L
                Thread.sleep(delayMillis)
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { cb.success(cards) }
        }
    }
}
