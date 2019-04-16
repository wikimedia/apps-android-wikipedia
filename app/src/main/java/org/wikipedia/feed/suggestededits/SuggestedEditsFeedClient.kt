package org.wikipedia.feed.suggestededits

import android.content.Context
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.model.Card
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.provider.MissingDescriptionProvider

class SuggestedEditsFeedClient(var translation: Boolean) : FeedClient {
    private val disposables = CompositeDisposable()
    var sourceDescription: String = ""
    var targetPageTitle: PageTitle? = null
    private val app = WikipediaApp.getInstance()
    var summary: RbPageSummary? = null
    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        getArticleWithMissingDescription(cb, wiki)
    }

    private fun getArticleWithMissingDescription(cb: FeedClient.Callback, wiki: WikiSite) {
        if (translation) {
            disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(app.language().appLanguageCodes[0]), app.language().appLanguageCodes[1], true)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pair ->
                        targetPageTitle = pair.first
                        sourceDescription = StringUtils.defaultString(pair.second.description)
                        summary = pair.second
                        FeedCoordinator.postCardsToCallback(cb, if (pair == null) emptyList<Card>() else listOf(toSuggestedEditsCard(wiki)))
                    }, { cb.success(emptyList()) }))

        } else {
            disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(app.language().appLanguageCode))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pageSummary ->
                        summary = pageSummary
                        FeedCoordinator.postCardsToCallback(cb, if (summary == null) emptyList<Card>() else listOf(toSuggestedEditsCard(wiki)))
                    }, { cb.success(emptyList()) }))
        }

    }


    override fun cancel() {
        disposables.clear()
    }

    private fun toSuggestedEditsCard(wiki: WikiSite): SuggestedEditsCard {

        return SuggestedEditsCard(wiki, translation, summary, sourceDescription, targetPageTitle)
    }

}
