package org.wikipedia.feed.becauseyouread

import android.content.Context
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.dataclient.restbase.RbRelatedPages
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.history.HistoryEntry

class BecauseYouReadClient : FeedClient {
    private val disposables = CompositeDisposable()
    override fun request(context: Context, wiki: WikiSite, age: Int,
                         cb: FeedClient.Callback) {
        cancel()
        disposables.add(Observable.fromCallable(MainPageReadMoreTopicTask(age))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ entry: HistoryEntry -> getCardForHistoryEntry(entry, cb) }
                ) { cb.success(emptyList()) })
    }

    override fun cancel() {
        disposables.clear()
    }

    private fun getCardForHistoryEntry(entry: HistoryEntry,
                                       cb: FeedClient.Callback) {
        disposables.add(ServiceFactory.getRest(entry.title.wikiSite).getRelatedPages(entry.title.prefixedText)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { response: RbRelatedPages -> response.getPages(Constants.SUGGESTION_REQUEST_ITEMS) }
                .subscribe({ results: List<PageSummary>? -> FeedCoordinator.postCardsToCallback(cb, if (results.isNullOrEmpty()) emptyList() else listOf(toBecauseYouReadCard(results, entry))) }) { caught: Throwable? -> cb.error(caught!!) })
    }

    private fun toBecauseYouReadCard(results: List<PageSummary>, entry: HistoryEntry) =
        BecauseYouReadCard(entry,
                results.map { BecauseYouReadItemCard(it.getPageTitle(entry.title.wikiSite)) }.toMutableList())

}
