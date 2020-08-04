package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_cards_item.*
import kotlinx.android.synthetic.main.view_image_detail_horizontal.view.*
import org.wikipedia.R
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ImageZoomHelper

class SuggestedEditsCardsItemFragment : SuggestedEditsItemFragment() {
    var sourceSummaryForEdit: PageSummaryForEdit? = null
    var targetSummaryForEdit: PageSummaryForEdit? = null
    var addedContribution: String = ""
        internal set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_cards_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setConditionalLayoutDirection(viewArticleContainer, parent().langFromCode)

        viewArticleImage.setOnClickListener {
            if (Prefs.shouldShowImageZoomTooltip()) {
                Prefs.setShouldShowImageZoomTooltip(false)
                FeedbackUtil.showMessage(requireActivity(), R.string.suggested_edits_image_zoom_tooltip)
            }
        }

        cardItemErrorView.setBackClickListener { requireActivity().finish() }
        cardItemErrorView.setRetryClickListener {
            cardItemProgressBar.isVisible = true
            getArticleWithMissingDescription()
        }
        updateContents()
        if (sourceSummaryForEdit == null) {
            getArticleWithMissingDescription()
        }

        viewArticleContainer.setOnClickListener {
            if (sourceSummaryForEdit != null) {
                parent().onSelectPage()
            }
        }
        showAddedContributionView(addedContribution)
    }

    private fun getArticleWithMissingDescription() {
        when (parent().action) {
            TRANSLATE_DESCRIPTION -> {
                disposables.add(EditingSuggestionsProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(parent().langFromCode), parent().langToCode, true)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ pair ->
                            val source = pair.second
                            val target = pair.first

                            sourceSummaryForEdit = PageSummaryForEdit(
                                    source.apiTitle,
                                    source.lang,
                                    source.getPageTitle(WikiSite.forLanguageCode(parent().langFromCode)),
                                    source.displayTitle,
                                    source.description,
                                    source.thumbnailUrl,
                                    source.extractHtml
                            )

                            targetSummaryForEdit = PageSummaryForEdit(
                                    target.apiTitle,
                                    target.lang,
                                    target.getPageTitle(WikiSite.forLanguageCode(parent().langToCode)),
                                    target.displayTitle,
                                    target.description,
                                    target.thumbnailUrl,
                                    target.extractHtml
                            )
                            updateContents()
                        }, { this.setErrorState(it) })!!)
            }

            ADD_CAPTION -> {
                disposables.add(EditingSuggestionsProvider.getNextImageWithMissingCaption(parent().langFromCode)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap { title ->
                            ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(title, parent().langFromCode)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                        }
                        .subscribe({ response ->
                            val page = response.query()!!.pages()!![0]
                            if (page.imageInfo() != null) {
                                val imageInfo = page.imageInfo()!!
                                val title = if (imageInfo.commonsUrl.isEmpty()) page.title() else WikiSite(Service.COMMONS_URL).titleForUri(imageInfo.commonsUrl.toUri()).prefixedText

                                sourceSummaryForEdit = PageSummaryForEdit(
                                        title,
                                        parent().langFromCode,
                                        PageTitle(
                                                Namespace.FILE.name,
                                                StringUtil.removeNamespace(title),
                                                null,
                                                imageInfo.thumbUrl,
                                                WikiSite.forLanguageCode(parent().langFromCode)
                                        ),
                                        StringUtil.removeHTMLTags(title),
                                        imageInfo.metadata!!.imageDescription(),
                                        imageInfo.thumbUrl,
                                        null,
                                        imageInfo.timestamp,
                                        imageInfo.user,
                                        imageInfo.metadata
                                )
                            }
                            updateContents()
                        }, { this.setErrorState(it) })!!)
            }

            TRANSLATE_CAPTION -> {
                var fileCaption: String? = null
                disposables.add(EditingSuggestionsProvider.getNextImageWithMissingCaption(parent().langFromCode, parent().langToCode)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap { pair ->
                            fileCaption = pair.first
                            ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(pair.second, parent().langFromCode)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                        }
                        .subscribe({ response ->
                            val page = response.query()!!.pages()!![0]
                            if (page.imageInfo() != null) {
                                val imageInfo = page.imageInfo()!!
                                val title = if (imageInfo.commonsUrl.isEmpty()) page.title() else WikiSite(Service.COMMONS_URL).titleForUri(imageInfo.commonsUrl.toUri()).prefixedText

                                sourceSummaryForEdit = PageSummaryForEdit(
                                        title,
                                        parent().langFromCode,
                                        PageTitle(
                                                Namespace.FILE.name,
                                                StringUtil.removeNamespace(title),
                                                null,
                                                imageInfo.thumbUrl,
                                                WikiSite.forLanguageCode(parent().langFromCode)
                                        ),
                                        StringUtil.removeHTMLTags(title),
                                        fileCaption,
                                        imageInfo.thumbUrl,
                                        null,
                                        imageInfo.timestamp,
                                        imageInfo.user,
                                        imageInfo.metadata
                                )

                                targetSummaryForEdit = sourceSummaryForEdit!!.copy(
                                        description = null,
                                        lang = parent().langToCode,
                                        pageTitle = PageTitle(
                                                Namespace.FILE.name,
                                                StringUtil.removeNamespace(title),
                                                null,
                                                imageInfo.thumbUrl,
                                                WikiSite.forLanguageCode(parent().langToCode)
                                        )
                                )
                            }
                            updateContents()
                        }, { this.setErrorState(it) })!!)
            }

            else -> {
                disposables.add(EditingSuggestionsProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(parent().langFromCode))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ pageSummary ->
                            sourceSummaryForEdit = PageSummaryForEdit(
                                    pageSummary.apiTitle,
                                    parent().langFromCode,
                                    pageSummary.getPageTitle(WikiSite.forLanguageCode(parent().langFromCode)),
                                    pageSummary.displayTitle,
                                    pageSummary.description,
                                    pageSummary.thumbnailUrl,
                                    pageSummary.extractHtml
                            )
                            updateContents()
                        }, { this.setErrorState(it) }))
            }
        }
    }

    fun showAddedContributionView(addedContribution: String?) {
        if (!addedContribution.isNullOrEmpty()) {
            viewArticleSubtitleContainer.isVisible = true
            viewArticleSubtitle.text = addedContribution
            this.addedContribution = addedContribution
        }
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        cardItemErrorView.setError(t)
        cardItemErrorView.isVisible = true
        cardItemProgressBar.isVisible = false
        cardItemContainer.isVisible = false
    }

    private fun updateContents() {
        val sourceAvailable = sourceSummaryForEdit != null
        cardItemErrorView.isVisible = false
        cardItemContainer.isVisible = sourceAvailable
        cardItemProgressBar.isVisible = !sourceAvailable
        if (!sourceAvailable) {
            return
        }

        ImageZoomHelper.setViewZoomable(viewArticleImage)

        if (parent().action == ADD_DESCRIPTION || parent().action == TRANSLATE_DESCRIPTION) {
            updateDescriptionContents()
        } else {
            updateCaptionContents()
        }
    }

    private fun updateDescriptionContents() {
        viewArticleTitle.text = StringUtil.fromHtml(sourceSummaryForEdit!!.displayTitle)
        viewArticleTitle.isVisible = true

        if (parent().action == TRANSLATE_DESCRIPTION) {
            viewArticleSubtitleContainer.isVisible = true
            viewArticleSubtitle.text = if (addedContribution.isNotEmpty()) addedContribution else sourceSummaryForEdit!!.description
        }

        viewImageSummaryContainer.isVisible = false

        viewArticleExtract.text = StringUtil.removeHTMLTags(sourceSummaryForEdit!!.extractHtml!!)
        if (sourceSummaryForEdit!!.thumbnailUrl.isNullOrBlank()) {
            viewArticleImagePlaceholder.isVisible = false
        } else {
            viewArticleImagePlaceholder.isVisible = true
            viewArticleImage.loadImage(sourceSummaryForEdit!!.getPreferredSizeThumbnailUrl().toUri())
        }
    }

    private fun updateCaptionContents() {
        viewArticleTitle.isVisible = false
        viewArticleSubtitleContainer.isVisible = true

        val descriptionText = when {
            addedContribution.isNotEmpty() -> addedContribution
            sourceSummaryForEdit!!.description!!.isNotEmpty() -> sourceSummaryForEdit!!.description!!
            else -> getString(R.string.suggested_edits_no_description)
        }

        viewArticleSubtitle.text = StringUtil.strip(StringUtil.removeHTMLTags(descriptionText))
        viewImageFileName.setDetailText(StringUtil.removeNamespace(sourceSummaryForEdit!!.displayTitle!!))

        if (!sourceSummaryForEdit!!.user.isNullOrEmpty()) {
            viewImageArtist.titleText.text = getString(R.string.suggested_edits_image_caption_summary_title_author)
            viewImageArtist.setDetailText(sourceSummaryForEdit!!.user)
        } else {
            viewImageArtist.titleText.text = StringUtil.removeHTMLTags(sourceSummaryForEdit!!.metadata!!.artist())
        }

        viewImageDate.setDetailText(DateUtil.getReadingListsLastSyncDateString(sourceSummaryForEdit!!.timestamp!!))
        viewImageSource.setDetailText(sourceSummaryForEdit!!.metadata!!.credit())
        viewImageLicense.setDetailText(sourceSummaryForEdit!!.metadata!!.licenseShortName())

        viewArticleImage.loadImage(sourceSummaryForEdit!!.getPreferredSizeThumbnailUrl().toUri())
        viewArticleExtract.isVisible = false
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsCardsItemFragment()
        }
    }
}
