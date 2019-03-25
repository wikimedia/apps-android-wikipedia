package org.wikipedia.feed.suggestededits

import android.content.Context
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.editactionfeed.provider.MissingDescriptionProvider
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.model.Card
import org.wikipedia.util.log.L

class SuggestedEditFeedClient(var translation: Boolean) : FeedClient {
    private val disposables = CompositeDisposable()
    var sourceDescription: String = ""
    private val app = WikipediaApp.getInstance()
    var summary: RbPageSummary? = null
    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        getArticleWithMissingDescription(cb, wiki)
    }

    private fun getArticleWithMissingDescription(cb: FeedClient.Callback, wiki: WikiSite) {
        if (translation) {
            disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(app.language().appLanguageCodes.get(0)), app.language().appLanguageCodes.get(1), true)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pair ->
                        sourceDescription = StringUtils.defaultString(pair.first)
                        summary = pair.second
                        FeedCoordinator.postCardsToCallback(cb, if (pair == null) emptyList<Card>() else listOf(toSuggestedEditsCard(wiki)))
                    }, { this.setErrorState(it, cb) }))

        } else {
            disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(app.language().appLanguageCode))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pageSummary ->
                        summary = pageSummary
                        FeedCoordinator.postCardsToCallback(cb, if (summary == null) emptyList<Card>() else listOf(toSuggestedEditsCard(wiki)))
                    }, { this.setErrorState(it, cb) }))
        }

    }

    private fun setErrorState(t: Throwable, cb: FeedClient.Callback) {
        L.e(t)
    }

    override fun cancel() {
        disposables.clear()
    }

    private fun toSuggestedEditsCard(wiki: WikiSite): SuggestedEditCard {

        return SuggestedEditCard(wiki, translation, summary, sourceDescription)
    }

}
