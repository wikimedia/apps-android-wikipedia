package org.wikipedia.feed.suggestededits

import android.net.Uri
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource.FEED
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.FeedFunnel
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.descriptions.DescriptionEditReviewView
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.FaceAndColorDetectImageView
import org.wikipedia.views.GoneIfEmptyTextView
import org.wikipedia.views.WikiCardView
import org.wikipedia.views.WikiErrorView

class SeCardsViewHolder internal constructor(var view: View) : RecyclerView.ViewHolder(view) {
    var cardActionType: DescriptionEditActivity.Action? = null
    private var app = WikipediaApp.getInstance()
    private var appLanguages = app.language().appLanguageCodes
    var langFromCode: String = appLanguages[0]
    var targetLanguage: String? = null
    private val disposables = CompositeDisposable()
    var sourceSummaryForEdit: PageSummaryForEdit? = null
    var targetSummaryForEdit: PageSummaryForEdit? = null
    var imageTagPage: MwQueryPage? = null
    var itemClickable = false
    var funnel = FeedFunnel(app)
    private val cardView: WikiCardView = view.findViewById(R.id.cardView)
    private val seCardErrorView: WikiErrorView = view.findViewById(R.id.seCardErrorView)
    private val seFeedCardProgressBar: ProgressBar = view.findViewById(R.id.seFeedCardProgressBar)
    val suggestedEditsFragmentViewGroup: Group = view.findViewById(R.id.suggestedEditsFragmentViewGroup)
    private val viewArticleImage: FaceAndColorDetectImageView = view.findViewById(R.id.viewArticleImage)
    private val viewArticleTitle: TextView = view.findViewById(R.id.viewArticleTitle)
    private val viewArticleSubtitle: GoneIfEmptyTextView = view.findViewById(R.id.viewArticleSubtitle)
    private val articleDescriptionPlaceHolder1: View = view.findViewById(R.id.articleDescriptionPlaceHolder1)
    private val articleDescriptionPlaceHolder2: View = view.findViewById(R.id.articleDescriptionPlaceHolder2)
    private val divider: View = view.findViewById(R.id.divider)
    private val viewArticleExtract: TextView = view.findViewById(R.id.viewArticleExtract)
    private val callToActionButton: MaterialButton = view.findViewById(R.id.callToActionButton)

    fun bindItem(age: Int, action: DescriptionEditActivity.Action) {
        cardActionType = action
        if (appLanguages.size > 1) {
            targetLanguage = appLanguages[age % appLanguages.size]
            if (cardActionType == ADD_DESCRIPTION && !targetLanguage.equals(langFromCode))
                cardActionType = TRANSLATE_DESCRIPTION
            if (cardActionType == ADD_CAPTION && !targetLanguage.equals(appLanguages[0]))
                cardActionType = TRANSLATE_CAPTION
        }
        SuggestedEditsFunnel.get(FEED).impression(cardActionType)
        updateContents()
    }

    private fun updateContents() {
        suggestedEditsFragmentViewGroup.visibility = View.GONE
        seCardErrorView.setBackClickListener {
            seCardErrorView.visibility = View.GONE
            fetchCardTypeEdit()
        }
        fetchCardTypeEdit()
    }

    fun fetchCardTypeEdit() {
        seFeedCardProgressBar.visibility = View.VISIBLE
        when (cardActionType) {
            ADD_DESCRIPTION -> addDescription()
            TRANSLATE_DESCRIPTION -> translateDescription()
            ADD_CAPTION -> addCaption()
            TRANSLATE_CAPTION -> translateCaption()
            ADD_IMAGE_TAGS -> addImageTags()
        }
    }

    private fun addDescription() {
        disposables.add(EditingSuggestionsProvider
                .getNextArticleWithMissingDescription(WikiSite.forLanguageCode(langFromCode), MAX_RETRY_LIMIT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pageSummary ->
                    sourceSummaryForEdit = PageSummaryForEdit(
                            pageSummary.apiTitle,
                            langFromCode,
                            pageSummary.getPageTitle(WikiSite.forLanguageCode(langFromCode)),
                            pageSummary.displayTitle,
                            pageSummary.description,
                            pageSummary.thumbnailUrl,
                            pageSummary.extract,
                            pageSummary.extractHtml
                    )
                    updateUI()
                }, {
                    showError(it)
                }))
    }

    private fun translateDescription() {
        cardActionType = TRANSLATE_DESCRIPTION
        if (targetLanguage!!.isEmpty()) {
            return
        }
        disposables.add(EditingSuggestionsProvider
                .getNextArticleWithMissingDescription(WikiSite.forLanguageCode(langFromCode), targetLanguage!!, true, MAX_RETRY_LIMIT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pair ->
                    val source = pair.second
                    val target = pair.first

                    sourceSummaryForEdit = PageSummaryForEdit(
                            source.apiTitle,
                            langFromCode,
                            source.getPageTitle(WikiSite.forLanguageCode(langFromCode)),
                            source.displayTitle,
                            source.description,
                            source.thumbnailUrl,
                            source.extract,
                            source.extractHtml
                    )

                    targetSummaryForEdit = PageSummaryForEdit(
                            target.apiTitle,
                            targetLanguage!!,
                            target.getPageTitle(WikiSite.forLanguageCode(targetLanguage!!)),
                            target.displayTitle,
                            target.description,
                            target.thumbnailUrl,
                            target.extract,
                            target.extractHtml
                    )
                    updateUI()
                }, {
                    showError(it)
                }))
    }

    private fun addCaption() {
        disposables.add(EditingSuggestionsProvider.getNextImageWithMissingCaption(langFromCode, MAX_RETRY_LIMIT)
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

                        sourceSummaryForEdit = PageSummaryForEdit(
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
                                null,
                                imageInfo.timestamp,
                                imageInfo.user,
                                imageInfo.metadata
                        )
                        updateUI()
                    }
                }, {
                    showError(it)
                }))
    }

    private fun translateCaption() {
        cardActionType = TRANSLATE_CAPTION
        if (targetLanguage!!.isEmpty()) {
            return
        }
        var fileCaption: String? = null
        disposables.add(EditingSuggestionsProvider.getNextImageWithMissingCaption(langFromCode, targetLanguage!!, MAX_RETRY_LIMIT)
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

                        sourceSummaryForEdit = PageSummaryForEdit(
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
                                null,
                                imageInfo.timestamp,
                                imageInfo.user,
                                imageInfo.metadata
                        )

                        targetSummaryForEdit = sourceSummaryForEdit!!.copy(
                                description = null,
                                lang = targetLanguage!!,
                                pageTitle = PageTitle(
                                        Namespace.FILE.name,
                                        StringUtil.removeNamespace(title),
                                        null,
                                        imageInfo.thumbUrl,
                                        WikiSite.forLanguageCode(targetLanguage!!)
                                )
                        )
                    }
                    updateUI()
                }, {
                    showError(it)
                }))
    }

    private fun addImageTags() {
        disposables.add(EditingSuggestionsProvider
                .getNextImageWithMissingTags(MAX_RETRY_LIMIT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ page ->
                    imageTagPage = page
                    updateUI()
                }, {
                    showError(it)
                }))
    }

    private fun updateUI() {
        if (cardActionType != ADD_IMAGE_TAGS && sourceSummaryForEdit == null) {
            return
        }
        itemClickable = true
        seFeedCardProgressBar.visibility = View.GONE
        seCardErrorView.visibility = View.GONE
        suggestedEditsFragmentViewGroup.visibility = View.VISIBLE
        callToActionButton.visibility = View.VISIBLE
        if (sourceSummaryForEdit != null) {
            cardView.layoutDirection = if (L10nUtil.isLangRTL(if (targetSummaryForEdit != null)
                        targetSummaryForEdit!!.lang else sourceSummaryForEdit!!.lang))
                View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }

        when (cardActionType) {
            TRANSLATE_DESCRIPTION -> showTranslateDescriptionUI()
            ADD_CAPTION -> showAddImageCaptionUI()
            TRANSLATE_CAPTION -> showTranslateImageCaptionUI()
            ADD_IMAGE_TAGS -> showImageTagsUI()
            else -> showAddDescriptionUI()
        }
    }

    private fun showImageTagsUI() {
        showAddImageCaptionUI()
        callToActionButton.text = view.context.getString(R.string.suggested_edits_feed_card_add_image_tags)
        viewArticleExtract.text = StringUtil.removeNamespace(imageTagPage!!.title())
    }

    private fun showAddDescriptionUI() {
        callToActionButton.text = view.context.getString(R.string.suggested_edits_feed_card_add_description_button)
        articleDescriptionPlaceHolder1.visibility = View.VISIBLE
        articleDescriptionPlaceHolder2.visibility = View.VISIBLE
        viewArticleTitle.visibility = View.VISIBLE
        divider.visibility = View.VISIBLE
        viewArticleTitle.text = StringUtil.fromHtml(sourceSummaryForEdit!!.displayTitle!!)
        viewArticleExtract.text = StringUtil.fromHtml(sourceSummaryForEdit!!.extract)
        viewArticleExtract.maxLines = DescriptionEditReviewView.ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        showItemImage()
    }

    private fun showTranslateDescriptionUI() {
        showAddDescriptionUI()
        callToActionButton.text = view.context.getString(R.string.suggested_edits_feed_card_add_translation_in_language_button,
                app.language().getAppLanguageCanonicalName(targetLanguage))
        viewArticleSubtitle.visibility = View.VISIBLE
        viewArticleSubtitle.text = sourceSummaryForEdit?.description
    }

    private fun showAddImageCaptionUI() {
        callToActionButton.text = view.context.getString(R.string.suggested_edits_feed_card_add_image_caption)
        viewArticleTitle.visibility = View.GONE
        viewArticleExtract.visibility = View.VISIBLE
        viewArticleExtract.text = StringUtil.removeNamespace(StringUtils.defaultString(sourceSummaryForEdit?.displayTitle))
        showItemImage()
    }

    private fun showTranslateImageCaptionUI() {
        showAddImageCaptionUI()
        callToActionButton.text = view.context.getString(R.string.suggested_edits_feed_card_translate_image_caption,
                app.language().getAppLanguageCanonicalName(targetLanguage))
        viewArticleSubtitle.visibility = View.VISIBLE
        viewArticleSubtitle.text = sourceSummaryForEdit?.description
    }

    private fun showItemImage() {
        viewArticleImage.visibility = View.VISIBLE
        if (cardActionType == ADD_IMAGE_TAGS) {
            viewArticleImage.loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize
            (imageTagPage!!.imageInfo()!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)))
        } else {
            if (sourceSummaryForEdit!!.thumbnailUrl.isNullOrBlank()) {
                viewArticleImage.visibility = View.GONE
                viewArticleExtract.maxLines = DescriptionEditReviewView.ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
            } else {
                viewArticleImage.loadImage(Uri.parse(sourceSummaryForEdit!!.thumbnailUrl))
                viewArticleExtract.maxLines = DescriptionEditReviewView.ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
            }
        }
    }

    fun showError(caught: Throwable?) {
        seFeedCardProgressBar.visibility = View.GONE
        seCardErrorView.setError(caught)
        seCardErrorView.visibility = View.VISIBLE
        seCardErrorView.bringToFront()
    }

    companion object {
        const val MAX_RETRY_LIMIT: Long = 5
    }
}
