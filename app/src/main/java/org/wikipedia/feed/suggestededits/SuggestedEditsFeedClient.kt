package org.wikipedia.feed.suggestededits

import android.content.Context
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.model.Card
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.suggestededits.SuggestedEditsUserStats
import org.wikipedia.suggestededits.provider.MissingDescriptionProvider
import org.wikipedia.util.StringUtil
import java.util.*

class SuggestedEditsFeedClient(private var action: DescriptionEditActivity.Action) : FeedClient {
    interface Callback {
        fun updateCardContent(card: SuggestedEditsCard)
    }

    private var age: Int = 0
    private val disposables = CompositeDisposable()
    private val app = WikipediaApp.getInstance()
    private val langFromCode: String = app.language().appLanguageCode
    private val langToCode: String = if (app.language().appLanguageCodes.size == 1) "" else app.language().appLanguageCodes[1]

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        this.age = age
        cancel()

        if (age == 0) {
            // In the background, fetch the user's latest contribution stats, so that we can update whether the
            // Suggested Edits feature is paused or disabled, the next time the feed is refreshed.
            SuggestedEditsUserStats.updateStatsInBackground()
        }

        if (SuggestedEditsUserStats.isDisabled() || SuggestedEditsUserStats.maybePauseAndGetEndDate() != null) {
            FeedCoordinator.postCardsToCallback(cb, Collections.emptyList())
            return
        }

        fetchSuggestedEditForType(cb, null)
    }

    override fun cancel() {
        disposables.clear()
    }

    private fun toSuggestedEditsCard(wiki: WikiSite, sourceSummary: SuggestedEditsSummary?, targetSummary: SuggestedEditsSummary?, page: MwQueryPage?): SuggestedEditsCard {
        return SuggestedEditsCard(wiki, action, sourceSummary, targetSummary, page, age)
    }

    fun fetchSuggestedEditForType(cb: FeedClient.Callback?, callback: Callback?) {
        when (action) {
            TRANSLATE_DESCRIPTION -> getArticleToTranslateDescription(cb, callback)
            ADD_CAPTION -> getImageToAddCaption(cb, callback)
            TRANSLATE_CAPTION -> getImageToTranslateCaption(cb, callback)
            ADD_IMAGE_TAGS -> getImageToAddTags(cb, callback)
            else -> getArticleToAddDescription(cb, callback)
        }
    }

    private fun getImageToAddTags(cb: FeedClient.Callback?, callback: Callback?) {
        disposables.add(MissingDescriptionProvider
                .getNextImageWithMissingTags(langFromCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ page ->
                    val card: SuggestedEditsCard = toSuggestedEditsCard(WikiSite.forLanguageCode(langFromCode), null, null, page)

                    callback?.updateCardContent(card)
                    if (cb != null) {
                        FeedCoordinator.postCardsToCallback(cb, if (page == null) emptyList<Card>() else listOf(card))
                    }
                }, { cb?.error(it) }))
    }

    private fun getArticleToAddDescription(cb: FeedClient.Callback?, callback: Callback?) {
        disposables.add(MissingDescriptionProvider
                .getNextArticleWithMissingDescription(WikiSite.forLanguageCode(langFromCode))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pageSummary ->
                    val sourceSummary = SuggestedEditsSummary(
                            pageSummary.apiTitle,
                            langFromCode,
                            pageSummary.getPageTitle(WikiSite.forLanguageCode(langFromCode)),
                            pageSummary.displayTitle,
                            pageSummary.description,
                            pageSummary.thumbnailUrl,
                            pageSummary.extractHtml
                    )

                    val card: SuggestedEditsCard = toSuggestedEditsCard(WikiSite.forLanguageCode(langFromCode), sourceSummary, null, null)

                    callback?.updateCardContent(card)
                    if (cb != null) {
                        FeedCoordinator.postCardsToCallback(cb, listOf(card))
                    }
                }, { cb?.error(it) }))
    }

    private fun getArticleToTranslateDescription(cb: FeedClient.Callback?, callback: Callback?) {
        if (langToCode.isEmpty()) {
            if (cb != null) {
                FeedCoordinator.postCardsToCallback(cb, emptyList<Card>())
            }
            return
        }
        disposables.add(MissingDescriptionProvider
                .getNextArticleWithMissingDescription(WikiSite.forLanguageCode(langFromCode), langToCode, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pair ->
                    val source = pair.second
                    val target = pair.first

                    val sourceSummary = SuggestedEditsSummary(
                            source.apiTitle,
                            langFromCode,
                            source.getPageTitle(WikiSite.forLanguageCode(langFromCode)),
                            source.displayTitle,
                            source.description,
                            source.thumbnailUrl,
                            source.extractHtml
                    )

                    val targetSummary = SuggestedEditsSummary(
                            target.apiTitle,
                            langToCode,
                            target.getPageTitle(WikiSite.forLanguageCode(langToCode)),
                            target.displayTitle,
                            target.description,
                            target.thumbnailUrl,
                            target.extractHtml
                    )

                    val card: SuggestedEditsCard = toSuggestedEditsCard(WikiSite.forLanguageCode(langFromCode), sourceSummary, targetSummary, null)

                    callback?.updateCardContent(card)
                    if (cb != null) {
                        FeedCoordinator.postCardsToCallback(cb, if (pair == null) emptyList<Card>() else listOf(card))
                    }
                }, { cb?.error(it) }))
    }

    private fun getImageToAddCaption(cb: FeedClient.Callback?, callback: Callback?) {
        disposables.add(MissingDescriptionProvider.getNextImageWithMissingCaption(langFromCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { title ->
                    ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(title, langFromCode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                }
                .subscribe({ response ->
                    val page = response.query()!!.pages()!![0]
                    if (page.imageInfo() != null) {
                        val title = page.title()
                        val imageInfo = page.imageInfo()!!

                        val sourceSummary = SuggestedEditsSummary(
                                title,
                                langFromCode,
                                PageTitle(
                                        Namespace.FILE.name,
                                        StringUtil.removeNamespace(title),
                                        null,
                                        imageInfo.thumbUrl,
                                        WikiSite.forLanguageCode(langFromCode)
                                ),
                                StringUtil.removeHTMLTags(title),
                                imageInfo.metadata!!.imageDescription(),
                                imageInfo.thumbUrl,
                                null,
                                imageInfo.timestamp,
                                imageInfo.user,
                                imageInfo.metadata
                        )
                        val card: SuggestedEditsCard = toSuggestedEditsCard(WikiSite.forLanguageCode(langFromCode), sourceSummary, null, null)
                        callback?.updateCardContent(card)
                        if (cb != null) {
                            FeedCoordinator.postCardsToCallback(cb, listOf(card))
                        }
                    }
                }, { cb?.error(it) }))
    }

    private fun getImageToTranslateCaption(cb: FeedClient.Callback?, callback: Callback?) {
        if (langToCode.isEmpty()) {
            if (cb != null) {
                FeedCoordinator.postCardsToCallback(cb, emptyList<Card>())
            }
            return
        }
        var fileCaption: String? = null
        disposables.add(MissingDescriptionProvider.getNextImageWithMissingCaption(langFromCode, langToCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { pair ->
                    fileCaption = pair.first
                    ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(pair.second, langFromCode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                }
                .subscribe({ response ->
                    val page = response.query()!!.pages()!![0]
                    if (page.imageInfo() != null) {
                        val title = page.title()
                        val imageInfo = page.imageInfo()!!

                        val sourceSummary = SuggestedEditsSummary(
                                title,
                                langFromCode,
                                PageTitle(
                                        Namespace.FILE.name,
                                        StringUtil.removeNamespace(title),
                                        null,
                                        imageInfo.thumbUrl,
                                        WikiSite.forLanguageCode(langFromCode)
                                ),
                                StringUtil.removeHTMLTags(title),
                                fileCaption,
                                imageInfo.thumbUrl,
                                null,
                                imageInfo.timestamp,
                                imageInfo.user,
                                imageInfo.metadata
                        )

                        val targetSummary = sourceSummary.copy(
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

                        val card: SuggestedEditsCard = toSuggestedEditsCard(WikiSite.forLanguageCode(langToCode), sourceSummary, targetSummary, null)
                        callback?.updateCardContent(card)
                        if (cb != null) {
                            FeedCoordinator.postCardsToCallback(cb, listOf(card))
                        }
                    }
                }, { cb?.error(it) }))
    }

}
