package org.wikipedia.feed.suggestededits

import android.content.Context
import android.os.Parcelable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.usercontrib.UserContribStats
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class SuggestedEditsFeedClient(
    private val coroutineScope: CoroutineScope
) : FeedClient {

    private lateinit var cb: FeedClient.Callback
    private var age: Int = 0
    private val disposables = CompositeDisposable()
    private var appLanguages = WikipediaApp.instance.languageState.appLanguageCodes

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        this.age = age
        this.cb = cb

        if (age == 0) {
            // In the background, fetch the user's latest contribution stats, so that we can update whether the
            // Suggested Edits feature is paused or disabled, the next time the feed is refreshed.
            coroutineScope.launch(CoroutineExceptionHandler { _, caught ->
                // Log the exception; will retry next time the feed is refreshed.
                L.e(caught)
            }) {
                UserContribStats.verifyEditCountsAndPauseState()
            }
        }

        if (UserContribStats.isDisabled() || UserContribStats.maybePauseAndGetEndDate() != null) {
            cb.success(emptyList())
            return
        }

        // Request three different SE cards
        coroutineScope.launch(CoroutineExceptionHandler { _, caught ->
            L.e(caught)
            cb.error(caught)
        }) {
            val addDescriptionCard = async { getCardTypeAndData(DescriptionEditActivity.Action.ADD_DESCRIPTION) }
        }
        getCardTypeAndData(DescriptionEditActivity.Action.ADD_DESCRIPTION) { descriptionSummary, _ ->
            getCardTypeAndData(DescriptionEditActivity.Action.ADD_CAPTION) { captionSummary, _ ->
                getCardTypeAndData(DescriptionEditActivity.Action.ADD_IMAGE_TAGS) { _, imageTagsPage ->
                    cb.success(
                        listOf(
                            SuggestedEditsCard(
                                listOf(descriptionSummary!!, captionSummary!!),
                                imageTagsPage,
                                wiki,
                                age
                            )
                        )
                    )
                    cancel()
                }
            }
        }
    }

    override fun cancel() {
        disposables.clear()
    }

    private suspend fun getCardTypeAndData(cardActionType: DescriptionEditActivity.Action): Pair<SuggestedEditsSummary, MwQueryPage?> {
        val suggestedEditsCard = SuggestedEditsSummary(cardActionType)
        val imageTagPage: MwQueryPage? = null
        val langFromCode = appLanguages.first()
        val targetLanguage = appLanguages.getOrElse(age % appLanguages.size) { langFromCode }
        if (appLanguages.size > 1) {
            if (cardActionType == DescriptionEditActivity.Action.ADD_DESCRIPTION && targetLanguage != langFromCode)
                suggestedEditsCard.cardActionType = DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION
            if (cardActionType == DescriptionEditActivity.Action.ADD_CAPTION && targetLanguage != langFromCode)
                suggestedEditsCard.cardActionType = DescriptionEditActivity.Action.TRANSLATE_CAPTION
        }
        when (suggestedEditsCard.cardActionType) {
            DescriptionEditActivity.Action.ADD_DESCRIPTION -> {
                suggestedEditsCard.sourceSummaryForEdit = addDescription(langFromCode)
            }
            DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> {
                translateDescription(langFromCode, targetLanguage).let {
                    suggestedEditsCard.sourceSummaryForEdit = it.first
                    suggestedEditsCard.targetSummaryForEdit = it.second
                }
            }
            DescriptionEditActivity.Action.ADD_CAPTION -> {
                suggestedEditsCard.sourceSummaryForEdit = addCaption(langFromCode)
            }
            DescriptionEditActivity.Action.TRANSLATE_CAPTION -> {
                translateCaption(langFromCode, targetLanguage)?.let {
                    suggestedEditsCard.sourceSummaryForEdit = it.first
                    suggestedEditsCard.targetSummaryForEdit = it.second
                }
            }
            DescriptionEditActivity.Action.ADD_IMAGE_TAGS -> {
                imageTagPage = addImageTags()
            }
            DescriptionEditActivity.Action.IMAGE_RECOMMENDATIONS -> {
                // ignore
            }
            else -> {
                // ignore
            }
        }
        return suggestedEditsCard to imageTagPage
    }

    private suspend fun addDescription(langFromCode: String): PageSummaryForEdit {
        val pageSummary = EditingSuggestionsProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(langFromCode),
            SuggestedEditsCardItemFragment.MAX_RETRY_LIMIT)

        return PageSummaryForEdit(
                pageSummary.apiTitle,
                langFromCode,
                pageSummary.getPageTitle(WikiSite.forLanguageCode(langFromCode)),
                pageSummary.displayTitle,
                pageSummary.description,
                pageSummary.thumbnailUrl,
                pageSummary.extract,
                pageSummary.extractHtml
            )
    }

    private suspend fun translateDescription(langFromCode: String, targetLanguage: String): Pair<PageSummaryForEdit, PageSummaryForEdit> {
        val pair = EditingSuggestionsProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(langFromCode),
            targetLanguage, true, SuggestedEditsCardItemFragment.MAX_RETRY_LIMIT)
        val source = pair.first
        val target = pair.second

        return PageSummaryForEdit(
                source.apiTitle,
                langFromCode,
                source.getPageTitle(WikiSite.forLanguageCode(langFromCode)),
                source.displayTitle,
                source.description,
                source.thumbnailUrl,
                source.extract,
                source.extractHtml
        ) to PageSummaryForEdit(
                target.apiTitle,
                targetLanguage,
                target.getPageTitle(WikiSite.forLanguageCode(targetLanguage)),
                target.displayTitle,
                target.description,
                target.thumbnailUrl,
                target.extract,
                target.extractHtml
        )
    }

    private suspend fun addCaption(langFromCode: String): PageSummaryForEdit? {
        val title = EditingSuggestionsProvider.getNextImageWithMissingCaption(langFromCode,
            SuggestedEditsCardItemFragment.MAX_RETRY_LIMIT)
        val imageInfoResponse = ServiceFactory.get(Constants.commonsWikiSite).getImageInfoSuspend(title, langFromCode)
        val page = imageInfoResponse.query?.firstPage()
        return page?.imageInfo()?.let {
            return@let PageSummaryForEdit(
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
        }
    }

    private suspend fun translateCaption(langFromCode: String, targetLanguage: String): Pair<PageSummaryForEdit, PageSummaryForEdit>? {
        val pair = EditingSuggestionsProvider.getNextImageWithMissingCaption(langFromCode, targetLanguage,
            SuggestedEditsCardItemFragment.MAX_RETRY_LIMIT
        )
        val fileCaption = pair.first
        val imageInfoResponse = ServiceFactory.get(Constants.commonsWikiSite).getImageInfoSuspend(pair.second, langFromCode)
        val page = imageInfoResponse.query?.firstPage()
        return page?.imageInfo()?.let {
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
            val targetSummaryForEdit = sourceSummaryForEdit.copy(
                description = null,
                lang = targetLanguage,
                pageTitle = PageTitle(
                    Namespace.FILE.name,
                    StringUtil.removeNamespace(page.title),
                    null,
                    it.thumbUrl,
                    WikiSite.forLanguageCode(targetLanguage)
                )
            )
            return@let sourceSummaryForEdit to targetSummaryForEdit
        }
    }

    private suspend fun addImageTags(): MwQueryPage {
        return EditingSuggestionsProvider
            .getNextImageWithMissingTags(SuggestedEditsCardItemFragment.MAX_RETRY_LIMIT)
    }

    @Suppress("unused")
    @Parcelize
    data class SuggestedEditsSummary(
        var cardActionType: DescriptionEditActivity.Action,
        var sourceSummaryForEdit: PageSummaryForEdit? = null,
        var targetSummaryForEdit: PageSummaryForEdit? = null
    ) : Parcelable
}
