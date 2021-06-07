package org.wikipedia.feed.random

import android.content.Context
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.log.L

class RandomClient : FeedClient {

    private val disposables = CompositeDisposable()

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        disposables.add(
            Observable.fromIterable(FeedContentType.aggregatedLanguages)
                .flatMap({ lang -> getRandomSummaryObservable(lang) }, { first, second -> Pair(first, second) })
                .observeOn(AndroidSchedulers.mainThread())
                .toList()
                .subscribe({ pairs ->
                    for (pair in pairs) {
                        if (pair.first != null && pair.second != null) {
                            FeedCoordinator.postCardsToCallback(cb, listOf(RandomCard(pair.second!!, age, WikiSite.forLanguageCode(pair.first!!))))
                        }
                    }
                }) { t ->
                    L.v(t)
                    cb.error(t)
                })
    }

    private fun getRandomSummaryObservable(lang: String): Observable<PageSummary?> {
        return ServiceFactory.getRest(WikiSite.forLanguageCode(lang))
            .randomSummary
            .subscribeOn(Schedulers.io())
            .onErrorResumeNext { throwable ->
                Observable.fromCallable {
                    val page = ReadingListDbHelper.randomPage ?: throw throwable as Exception
                    ReadingListPage.toPageSummary(page)
                }
            }
    }

    override fun cancel() {
        disposables.clear()
    }
}
