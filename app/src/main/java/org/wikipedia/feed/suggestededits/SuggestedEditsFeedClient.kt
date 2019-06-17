package org.wikipedia.feed.suggestededits

import android.content.Context
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.model.Card
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.suggestededits.provider.MissingDescriptionProvider

class SuggestedEditsFeedClient(private var isTranslation: Boolean) : FeedClient {

    interface Callback {
        fun updateCardContent(card: SuggestedEditsCard)
    }

    private val disposables = CompositeDisposable()
    private val app = WikipediaApp.getInstance()
    private var sourceSummary: SuggestedEditsSummary? = null
    private var targetSummary: SuggestedEditsSummary? = null

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        getArticleWithMissingDescription(cb, null)
    }

    override fun cancel() {
        disposables.clear()
    }

    private fun toSuggestedEditsCard(translation: Boolean, wiki: WikiSite): SuggestedEditsCard {
        return SuggestedEditsCard(wiki, translation, sourceSummary, targetSummary)
    }

    fun getArticleWithMissingDescription(cb: FeedClient.Callback?, callback: Callback?) {
        if (isTranslation) {
            disposables.add(MissingDescriptionProvider
                    .getNextArticleWithMissingDescription(WikiSite.forLanguageCode(app.language().appLanguageCodes[0]), app.language().appLanguageCodes[1], true)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pair ->
                        val source = pair.second
                        val target = pair.first

                        sourceSummary = SuggestedEditsSummary(
                                source.title,
                                source.lang,
                                source.getPageTitle(WikiSite.forLanguageCode(app.language().appLanguageCodes[0])),
                                source.normalizedTitle,
                                source.displayTitle,
                                source.description,
                                source.thumbnailUrl,
                                source.originalImageUrl,
                                source.extractHtml,
                                null, null, null
                        )

                        targetSummary = SuggestedEditsSummary(
                                target.title,
                                target.lang,
                                target.getPageTitle(WikiSite.forLanguageCode(app.language().appLanguageCodes[1])),
                                target.normalizedTitle,
                                target.displayTitle,
                                target.description,
                                target.thumbnailUrl,
                                target.originalImageUrl,
                                target.extractHtml,
                                null, null, null
                        )

                        val card: SuggestedEditsCard = toSuggestedEditsCard(isTranslation, WikiSite.forLanguageCode(app.language().appLanguageCodes[1]))

                        if (callback == null) {
                            FeedCoordinator.postCardsToCallback(cb!!, if (pair == null) emptyList<Card>() else listOf(card))
                        } else {
                            callback.updateCardContent(card)
                        }

                    }, { if (callback != null) cb!!.success(emptyList()) }))

        } else {
            disposables.add(MissingDescriptionProvider
                    .getNextArticleWithMissingDescription(WikiSite.forLanguageCode(app.language().appLanguageCode))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pageSummary ->
                        sourceSummary = SuggestedEditsSummary(
                                pageSummary.title,
                                pageSummary.lang,
                                pageSummary.getPageTitle(WikiSite.forLanguageCode(app.language().appLanguageCode)),
                                pageSummary.normalizedTitle,
                                pageSummary.displayTitle,
                                pageSummary.description,
                                pageSummary.thumbnailUrl,
                                pageSummary.originalImageUrl,
                                pageSummary.extractHtml,
                                null, null, null
                        )

                        val card: SuggestedEditsCard = toSuggestedEditsCard(isTranslation, WikiSite.forLanguageCode(app.language().appLanguageCodes[0]))

                        if (callback == null) {
                            FeedCoordinator.postCardsToCallback(cb!!, if (sourceSummary == null) emptyList<Card>() else listOf(card))
                        } else {
                            callback.updateCardContent(card)
                        }

                    }, { if (callback == null) cb!!.success(emptyList()) }))
        }
    }
}
