package org.wikipedia.feed.suggestededits

import android.content.Context
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource.*
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.model.Card
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.suggestededits.provider.MissingDescriptionProvider
import org.wikipedia.util.StringUtil

class SuggestedEditsFeedClient(private var invokeSource: Constants.InvokeSource) : FeedClient {
    interface Callback {
        fun updateCardContent(card: SuggestedEditsCard)
    }

    private var age: Int = 0
    private val disposables = CompositeDisposable()
    private val app = WikipediaApp.getInstance()
    private var sourceSummary: SuggestedEditsSummary? = null
    private var targetSummary: SuggestedEditsSummary? = null
    private val langFromCode: String = app.language().appLanguageCode
    private val langToCode: String = if (app.language().appLanguageCodes.size == 1) "" else app.language().appLanguageCodes[1]

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        this.age = age
        cancel()
        fetchSuggestedEditForType(cb, null)
    }

    override fun cancel() {
        disposables.clear()
    }

    private fun toSuggestedEditsCard(wiki: WikiSite): SuggestedEditsCard {
        return SuggestedEditsCard(wiki, invokeSource, sourceSummary, targetSummary, age)
    }

    fun fetchSuggestedEditForType(cb: FeedClient.Callback?, callback: Callback?) {
        when (invokeSource) {
            FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC -> getArticleToTranslateDescription(cb, callback)
            FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION -> getImageToAddCaption(cb, callback)
            FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION -> getImageToTranslateCaption(cb, callback)
            else -> getArticleToAddDescription(cb, callback)
        }
    }

    private fun getArticleToAddDescription(cb: FeedClient.Callback?, callback: Callback?) {
        disposables.add(MissingDescriptionProvider
                .getNextArticleWithMissingDescription(WikiSite.forLanguageCode(langFromCode))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pageSummary ->
                    sourceSummary = SuggestedEditsSummary(
                            pageSummary.title,
                            langFromCode,
                            pageSummary.getPageTitle(WikiSite.forLanguageCode(langFromCode)),
                            pageSummary.normalizedTitle,
                            pageSummary.displayTitle,
                            pageSummary.description,
                            pageSummary.thumbnailUrl,
                            pageSummary.extractHtml,
                            null, null, null
                    )

                    val card: SuggestedEditsCard = toSuggestedEditsCard(WikiSite.forLanguageCode(langFromCode))

                    if (callback == null) {
                        FeedCoordinator.postCardsToCallback(cb!!, if (sourceSummary == null) emptyList<Card>() else listOf(card))
                    } else {
                        callback.updateCardContent(card)
                    }

                }, { if (callback == null) cb!!.success(emptyList()) }))
    }

    private fun getArticleToTranslateDescription(cb: FeedClient.Callback?, callback: Callback?) {
        disposables.add(MissingDescriptionProvider
                .getNextArticleWithMissingDescription(WikiSite.forLanguageCode(langFromCode), langToCode, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pair ->
                    val source = pair.second
                    val target = pair.first

                    sourceSummary = SuggestedEditsSummary(
                            source.title,
                            langFromCode,
                            source.getPageTitle(WikiSite.forLanguageCode(langFromCode)),
                            source.normalizedTitle,
                            source.displayTitle,
                            source.description,
                            source.thumbnailUrl,
                            source.extractHtml,
                            null, null, null
                    )

                    targetSummary = SuggestedEditsSummary(
                            target.title,
                            langToCode,
                            target.getPageTitle(WikiSite.forLanguageCode(langToCode)),
                            target.normalizedTitle,
                            target.displayTitle,
                            target.description,
                            target.thumbnailUrl,
                            target.extractHtml,
                            null, null, null
                    )

                    val card: SuggestedEditsCard = toSuggestedEditsCard(WikiSite.forLanguageCode(langFromCode))

                    if (callback == null) {
                        FeedCoordinator.postCardsToCallback(cb!!, if (pair == null) emptyList<Card>() else listOf(card))
                    } else {
                        callback.updateCardContent(card)
                    }

                }, { if (callback != null) cb!!.success(emptyList()) }))
    }

    private fun getImageToAddCaption(cb: FeedClient.Callback?, callback: Callback?) {
        disposables.add(MissingDescriptionProvider.getNextImageWithMissingCaption(langFromCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { title ->
                    ServiceFactory.get(WikiSite.forLanguageCode(langFromCode)).getImageExtMetadata(title)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                }
                .subscribe({ response ->
                    val page = response.query()!!.pages()!![0]
                    if (page.imageInfo() != null) {
                        val title = page.title()
                        val imageInfo = page.imageInfo()!!

                        sourceSummary = SuggestedEditsSummary(
                                title,
                                langFromCode,
                                PageTitle(
                                        Namespace.FILE.name,
                                        StringUtil.removeNamespace(title),
                                        null,
                                        imageInfo.thumbUrl,
                                        WikiSite.forLanguageCode(langFromCode)
                                ),
                                StringUtil.removeUnderscores(title),
                                StringUtil.removeHTMLTags(title),
                                imageInfo.metadata!!.imageDescription(),
                                imageInfo.thumbUrl,
                                null,
                                imageInfo.timestamp,
                                imageInfo.user,
                                imageInfo.metadata
                        )
                        val card: SuggestedEditsCard = toSuggestedEditsCard(WikiSite.forLanguageCode(langFromCode))
                        if (callback == null) {
                            FeedCoordinator.postCardsToCallback(cb!!, if (sourceSummary == null) emptyList<Card>() else listOf(card))
                        } else {
                            callback.updateCardContent(card)
                        }
                    }
                }, { if (callback != null) cb!!.success(emptyList()) }))
    }

    private fun getImageToTranslateCaption(cb: FeedClient.Callback?, callback: Callback?) {
        var fileCaption: String? = null

        disposables.add(MissingDescriptionProvider.getNextImageWithMissingCaption(langFromCode, langToCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { pair ->
                    fileCaption = pair.first
                    ServiceFactory.get(WikiSite.forLanguageCode(langFromCode)).getImageExtMetadata(pair.second)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                }
                .subscribe({ response ->
                    val page = response.query()!!.pages()!![0]
                    if (page.imageInfo() != null) {
                        val title = page.title()
                        val imageInfo = page.imageInfo()!!

                        sourceSummary = SuggestedEditsSummary(
                                title,
                                langFromCode,
                                PageTitle(
                                        Namespace.FILE.name,
                                        StringUtil.removeNamespace(title),
                                        null,
                                        imageInfo.thumbUrl,
                                        WikiSite.forLanguageCode(langFromCode)
                                ),
                                StringUtil.removeUnderscores(title),
                                StringUtil.removeHTMLTags(title),
                                fileCaption,
                                imageInfo.thumbUrl,
                                null,
                                imageInfo.timestamp,
                                imageInfo.user,
                                imageInfo.metadata
                        )

                        targetSummary = sourceSummary!!.copy(
                                description = null,
                                lang = langToCode,
                                pageTitle = PageTitle(
                                        Namespace.FILE.name,
                                        StringUtil.removeNamespace(title),
                                        null,
                                        imageInfo.thumbUrl,
                                        WikiSite.forLanguageCode(langToCode)
                                )
                        )

                        val card: SuggestedEditsCard = toSuggestedEditsCard(WikiSite.forLanguageCode(langToCode))
                        if (callback == null) {
                            FeedCoordinator.postCardsToCallback(cb!!, if (targetSummary == null) emptyList<Card>() else listOf(card))
                        } else {
                            callback.updateCardContent(card)
                        }
                    }
                }, { if (callback == null) cb!!.success(emptyList()) }))
    }

}
