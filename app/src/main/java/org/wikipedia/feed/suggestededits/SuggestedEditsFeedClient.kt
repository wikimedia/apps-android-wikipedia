package org.wikipedia.feed.suggestededits

import android.content.Context
import android.os.Parcelable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.parcelize.Parcelize
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.usercontrib.UserContribStats
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import java.util.*

class SuggestedEditsFeedClient : FeedClient {

    fun interface ClientCallback {
        fun onComplete(suggestedEditsSummary: SuggestedEditsSummary?, imageTagPage: MwQueryPage?)
    }

    interface Callback {
        fun onReceiveSource(pageSummaryForEdit: PageSummaryForEdit)
        fun onReceiveTarget(pageSummaryForEdit: PageSummaryForEdit)
        fun onReceiveImageTag(imageTagPage: MwQueryPage)
    }

    private var age: Int = 0
    private val disposables = CompositeDisposable()
    private var appLanguages = WikipediaApp.instance.languageState.appLanguageCodes
    private var langFromCode = appLanguages[0]
    private var targetLanguage: String? = null

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        this.age = age
        this.targetLanguage

        if (age == 0) {
            // In the background, fetch the user's latest contribution stats, so that we can update whether the
            // Suggested Edits feature is paused or disabled, the next time the feed is refreshed.
            UserContribStats.updateStatsInBackground()
        }

        if (UserContribStats.isDisabled() || UserContribStats.maybePauseAndGetEndDate() != null) {
            FeedCoordinator.postCardsToCallback(cb, Collections.emptyList())
            return
        }

        // Request three different SE cards
        val list = mutableListOf<SuggestedEditsSummary>()
        getCardTypeAndData(DescriptionEditActivity.Action.ADD_DESCRIPTION) { descriptionSummary, _ ->
            list.add(descriptionSummary!!)
            getCardTypeAndData(DescriptionEditActivity.Action.ADD_CAPTION) { captionSummary, _ ->
                list.add(captionSummary!!)
                getCardTypeAndData(DescriptionEditActivity.Action.ADD_IMAGE_TAGS) { _, imageTagsPage ->
                    FeedCoordinator.postCardsToCallback(cb, listOf(SuggestedEditsCard(list, wiki, age)))
                    cancel()
                }
            }
        }
    }

    override fun cancel() {
        disposables.clear()
    }

    private fun getCardTypeAndData(cardActionType: DescriptionEditActivity.Action, clientCallback: ClientCallback) {
        val suggestedEditsCard = SuggestedEditsSummary(cardActionType)
        if (appLanguages.size > 1) {
            targetLanguage = appLanguages[age % appLanguages.size]
            if (cardActionType == DescriptionEditActivity.Action.ADD_DESCRIPTION && !targetLanguage.equals(langFromCode))
                suggestedEditsCard.cardActionType = DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION
            if (cardActionType == DescriptionEditActivity.Action.ADD_CAPTION && !targetLanguage.equals(appLanguages[0]))
                suggestedEditsCard.cardActionType = DescriptionEditActivity.Action.TRANSLATE_CAPTION
        }

        when (cardActionType) {
            DescriptionEditActivity.Action.ADD_DESCRIPTION -> addDescription(actionCallback(suggestedEditsCard, clientCallback))
            DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> translateDescription(actionCallback(suggestedEditsCard, clientCallback))
            DescriptionEditActivity.Action.ADD_CAPTION -> addCaption(actionCallback(suggestedEditsCard, clientCallback))
            DescriptionEditActivity.Action.TRANSLATE_CAPTION -> translateCaption(actionCallback(suggestedEditsCard, clientCallback))
            DescriptionEditActivity.Action.ADD_IMAGE_TAGS -> addImageTags(actionCallback(suggestedEditsCard, clientCallback))
        }
    }

    private fun actionCallback(suggestedEditsCard: SuggestedEditsSummary, clientCallback: ClientCallback): Callback {
        return object: Callback {
            override fun onReceiveSource(pageSummaryForEdit: PageSummaryForEdit) {
                suggestedEditsCard.sourceSummaryForEdit = pageSummaryForEdit
                clientCallback.onComplete(suggestedEditsCard, null)
            }

            override fun onReceiveTarget(pageSummaryForEdit: PageSummaryForEdit) {
                suggestedEditsCard.targetSummaryForEdit = pageSummaryForEdit
                clientCallback.onComplete(suggestedEditsCard, null)
            }

            override fun onReceiveImageTag(imageTagPage: MwQueryPage) {
                clientCallback.onComplete(null, imageTagPage)
            }
        }
    }

    private fun addDescription(callback: Callback) {
        disposables.add(EditingSuggestionsProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(langFromCode),
            SuggestedEditsCardItemFragment.MAX_RETRY_LIMIT)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ pageSummary ->
                callback.onReceiveSource(
                    PageSummaryForEdit(
                        pageSummary.apiTitle,
                        langFromCode,
                        pageSummary.getPageTitle(WikiSite.forLanguageCode(langFromCode)),
                        pageSummary.displayTitle,
                        pageSummary.description,
                        pageSummary.thumbnailUrl,
                        pageSummary.extract,
                        pageSummary.extractHtml
                    )
                )
            }, {
                L.e(it)
            }))
    }

    private fun translateDescription(callback: Callback) {
        if (targetLanguage.isNullOrEmpty()) {
            return
        }
        disposables.add(
            EditingSuggestionsProvider
            .getNextArticleWithMissingDescription(WikiSite.forLanguageCode(langFromCode), targetLanguage!!, true,
                SuggestedEditsCardItemFragment.MAX_RETRY_LIMIT
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ pair ->
                val source = pair.first
                val target = pair.second

                callback.onReceiveSource(
                    PageSummaryForEdit(
                        source.apiTitle,
                        langFromCode,
                        source.getPageTitle(WikiSite.forLanguageCode(langFromCode)),
                        source.displayTitle,
                        source.description,
                        source.thumbnailUrl,
                        source.extract,
                        source.extractHtml
                    )
                )

                callback.onReceiveTarget(
                    PageSummaryForEdit(
                        target.apiTitle,
                        targetLanguage!!,
                        target.getPageTitle(WikiSite.forLanguageCode(targetLanguage!!)),
                        target.displayTitle,
                        target.description,
                        target.thumbnailUrl,
                        target.extract,
                        target.extractHtml
                    )
                )

            }, {
                L.e(it)
            }))
    }

    private fun addCaption(callback: Callback) {
        disposables.add(
            EditingSuggestionsProvider.getNextImageWithMissingCaption(langFromCode,
                SuggestedEditsCardItemFragment.MAX_RETRY_LIMIT
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { title ->
                ServiceFactory.get(Constants.commonsWikiSite).getImageInfo(title, langFromCode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
            }
            .subscribe({ response ->
                val page = response.query?.firstPage()!!
                page.imageInfo()?.let {
                    callback.onReceiveSource(
                        PageSummaryForEdit(
                            page.title, langFromCode,
                            PageTitle(
                                Namespace.FILE.name,
                                StringUtil.removeNamespace(page.title),
                                null,
                                it.thumbUrl,
                                WikiSite.forLanguageCode(langFromCode)),
                            StringUtil.removeHTMLTags(page.title),
                            it.metadata!!.imageDescription(),
                            it.thumbUrl,
                            null,
                            null,
                            it.timestamp,
                            it.user,
                            it.metadata
                        )
                    )
                }
            }, {
                L.e(it)
            }))
    }

    private fun translateCaption(callback: Callback) {
        if (targetLanguage.isNullOrEmpty()) {
            return
        }
        var fileCaption: String? = null
        disposables.add(
            EditingSuggestionsProvider.getNextImageWithMissingCaption(langFromCode, targetLanguage!!,
                SuggestedEditsCardItemFragment.MAX_RETRY_LIMIT
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { pair ->
                fileCaption = pair.first
                ServiceFactory.get(Constants.commonsWikiSite).getImageInfo(pair.second, langFromCode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
            }
            .subscribe({ response ->
                val page = response.query?.firstPage()!!
                page.imageInfo()?.let {
                    val sourceSummaryForEdit = PageSummaryForEdit(
                        page.title,
                        langFromCode,
                        PageTitle(
                            Namespace.FILE.name,
                            StringUtil.removeNamespace(page.title),
                            null,
                            it.thumbUrl,
                            WikiSite.forLanguageCode(langFromCode)
                        ),
                        StringUtil.removeHTMLTags(page.title),
                        fileCaption,
                        it.thumbUrl,
                        null,
                        null,
                        it.timestamp,
                        it.user,
                        it.metadata
                    )
                    callback.onReceiveSource(sourceSummaryForEdit)
                    callback.onReceiveTarget(
                        sourceSummaryForEdit.copy(
                            description = null,
                            lang = targetLanguage!!,
                            pageTitle = PageTitle(
                                Namespace.FILE.name,
                                StringUtil.removeNamespace(page.title),
                                null,
                                it.thumbUrl,
                                WikiSite.forLanguageCode(targetLanguage!!)
                            )
                        )
                    )
                }
            }, {
                L.e(it)
            }))
    }

    private fun addImageTags(callback: Callback) {
        disposables.add(
            EditingSuggestionsProvider
            .getNextImageWithMissingTags(SuggestedEditsCardItemFragment.MAX_RETRY_LIMIT)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ page ->
                callback.onReceiveImageTag(page)
            }, {
                L.e(it)
            }))
    }

    @Suppress("unused")
    @Parcelize
    data class SuggestedEditsSummary(
        var cardActionType: DescriptionEditActivity.Action,
        var sourceSummaryForEdit: PageSummaryForEdit? = null,
        var targetSummaryForEdit: PageSummaryForEdit? = null
    ) : Parcelable
}
