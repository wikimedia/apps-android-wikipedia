package org.wikipedia.feed.becauseyouread

import android.content.Context
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.history.HistoryEntry
import org.wikipedia.util.StringUtil

class BecauseYouReadClient : FeedClient {

    private val disposables = CompositeDisposable()

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        disposables.add(
            Observable.fromCallable {
                AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(age,
                    context.resources.getInteger(R.integer.article_engagement_threshold_sec))
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ entries ->
                    if (entries.size <= age) cb.success(emptyList()) else getCardForHistoryEntry(entries[age], cb)
                }) { cb.success(emptyList()) })
    }

    override fun cancel() {
        disposables.clear()
    }

    private fun getCardForHistoryEntry(entry: HistoryEntry, cb: FeedClient.Callback) {

        // If the language code has a parent language code, it means set "Accept-Language" will slow down the loading time of /page/related
        // TODO: remove when https://phabricator.wikimedia.org/T271145 is resolved.
        val hasParentLanguageCode = !WikipediaApp.instance.languageState.getDefaultLanguageCode(entry.title.wikiSite.languageCode).isNullOrEmpty()
        val searchTerm = StringUtil.removeUnderscores(entry.title.prefixedText)
        disposables.add(ServiceFactory.get(entry.title.wikiSite)
            .searchMoreLike(searchTerm,
                Constants.SUGGESTION_REQUEST_ITEMS, Constants.SUGGESTION_REQUEST_ITEMS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { response ->
                val relatedPages = mutableListOf<PageSummary>()
                val langCode = entry.title.wikiSite.languageCode
                relatedPages.add(PageSummary(entry.title.displayText, entry.title.prefixedText, entry.title.description,
                    entry.title.extract, entry.title.thumbUrl, langCode))
                response.query?.pages?.forEach {
                    if (it.title != searchTerm) {
                        relatedPages.add(PageSummary(it.displayTitle(langCode), it.title, it.description,
                            it.extract, it.thumbUrl(), langCode))
                    }
                }
                Observable.fromIterable(relatedPages)
            }
            .concatMap { pageSummary ->
                if (hasParentLanguageCode) ServiceFactory.getRest(entry.title.wikiSite).getSummary(entry.referrer, pageSummary.apiTitle)
                else Observable.just(pageSummary)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .toList()
            .subscribe({ list ->
                val headerPage = list.removeAt(0)
                FeedCoordinator.postCardsToCallback(cb,
                    if (list.isEmpty()) emptyList()
                    else listOf(toBecauseYouReadCard(list, headerPage, entry.title.wikiSite))
                )
            }) { caught -> cb.error(caught) })
    }

    private fun toBecauseYouReadCard(results: List<PageSummary>, pageSummary: PageSummary, wikiSite: WikiSite): BecauseYouReadCard {
        val itemCards = results.map { BecauseYouReadItemCard(it.getPageTitle(wikiSite)) }
        return BecauseYouReadCard(pageSummary.getPageTitle(wikiSite), itemCards)
    }
}
