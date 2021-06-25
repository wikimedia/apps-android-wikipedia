package org.wikipedia.suggestededits

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.databinding.FragmentSuggestedEditsCardsItemBinding
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
    private var _binding: FragmentSuggestedEditsCardsItemBinding? = null
    private val binding get() = _binding!!
    var sourceSummaryForEdit: PageSummaryForEdit? = null
    var targetSummaryForEdit: PageSummaryForEdit? = null
    var addedContribution: String = ""
        internal set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsCardsItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setConditionalLayoutDirection(binding.viewArticleContainer, parent().langFromCode)

        binding.viewArticleImage.setOnClickListener {
            if (Prefs.shouldShowImageZoomTooltip()) {
                Prefs.setShouldShowImageZoomTooltip(false)
                FeedbackUtil.showMessage(requireActivity(), R.string.suggested_edits_image_zoom_tooltip)
            }
        }

        binding.cardItemErrorView.backClickListener = View.OnClickListener { requireActivity().finish() }
        binding.cardItemErrorView.retryClickListener = View.OnClickListener {
            binding.cardItemProgressBar.visibility = VISIBLE
            getArticleWithMissingDescription()
        }
        updateContents()
        if (sourceSummaryForEdit == null) {
            getArticleWithMissingDescription()
        }

        binding.viewArticleContainer.setOnClickListener {
            if (sourceSummaryForEdit != null) {
                parent().onSelectPage()
            }
        }
        showAddedContributionView(addedContribution)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun getArticleWithMissingDescription() {
        when (parent().action) {
            TRANSLATE_DESCRIPTION -> {
                disposables.add(EditingSuggestionsProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(parent().langFromCode), parent().langToCode, true)
                        .map {
                            if (it.first.description.isNullOrEmpty()) {
                                throw EditingSuggestionsProvider.ListEmptyException()
                            }
                            it
                        }
                        .retry { t: Throwable -> t is EditingSuggestionsProvider.ListEmptyException }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ pair ->
                            val source = pair.first
                            val target = pair.second

                            sourceSummaryForEdit = PageSummaryForEdit(
                                    source.apiTitle,
                                    source.lang,
                                    source.getPageTitle(WikiSite.forLanguageCode(parent().langFromCode)),
                                    source.displayTitle,
                                    source.description,
                                    source.thumbnailUrl,
                                    source.extract,
                                    source.extractHtml
                            )

                            targetSummaryForEdit = PageSummaryForEdit(
                                    target.apiTitle,
                                    target.lang,
                                    target.getPageTitle(WikiSite.forLanguageCode(parent().langToCode)),
                                    target.displayTitle,
                                    target.description,
                                    target.thumbnailUrl,
                                    target.extract,
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
                            val page = response.query?.pages()!![0]
                            if (page.imageInfo() != null) {
                                val imageInfo = page.imageInfo()!!
                                val title = if (imageInfo.commonsUrl.isEmpty()) page.title() else WikiSite(Service.COMMONS_URL).titleForUri(Uri.parse(imageInfo.commonsUrl)).prefixedText

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
                            val page = response.query?.pages()!![0]
                            if (page.imageInfo() != null) {
                                val imageInfo = page.imageInfo()!!
                                val title = if (imageInfo.commonsUrl.isEmpty()) page.title() else WikiSite(Service.COMMONS_URL).titleForUri(Uri.parse(imageInfo.commonsUrl)).prefixedText

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
                                    pageSummary.extract,
                                    pageSummary.extractHtml
                            )
                            updateContents()
                        }, { this.setErrorState(it) }))
            }
        }
    }

    fun showAddedContributionView(addedContribution: String?) {
        if (!addedContribution.isNullOrEmpty()) {
            binding.viewArticleSubtitleContainer.visibility = VISIBLE
            binding.viewArticleSubtitle.text = addedContribution
            this.addedContribution = addedContribution
        }
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        binding.cardItemErrorView.setError(t)
        binding.cardItemErrorView.visibility = VISIBLE
        binding.cardItemProgressBar.visibility = GONE
        binding.cardItemContainer.visibility = GONE
    }

    private fun updateContents() {
        val sourceAvailable = sourceSummaryForEdit != null
        binding.cardItemErrorView.visibility = GONE
        binding.cardItemContainer.visibility = if (sourceAvailable) VISIBLE else GONE
        binding.cardItemProgressBar.visibility = if (sourceAvailable) GONE else VISIBLE
        if (!sourceAvailable) {
            return
        }

        ImageZoomHelper.setViewZoomable(binding.viewArticleImage)

        if (parent().action == ADD_DESCRIPTION || parent().action == TRANSLATE_DESCRIPTION) {
            updateDescriptionContents()
        } else {
            updateCaptionContents()
        }
    }

    private fun updateDescriptionContents() {
        binding.viewArticleTitle.text = StringUtil.fromHtml(sourceSummaryForEdit!!.displayTitle)
        binding.viewArticleTitle.visibility = VISIBLE

        if (parent().action == TRANSLATE_DESCRIPTION) {
            binding.viewArticleSubtitleContainer.visibility = VISIBLE
            binding.viewArticleSubtitle.text = if (addedContribution.isNotEmpty()) addedContribution else sourceSummaryForEdit!!.description
        }

        binding.viewImageSummaryContainer.visibility = GONE

        binding.viewArticleExtract.text = StringUtil.removeHTMLTags(sourceSummaryForEdit!!.extractHtml!!)
        if (sourceSummaryForEdit!!.thumbnailUrl.isNullOrBlank()) {
            binding.viewArticleImagePlaceholder.visibility = GONE
        } else {
            binding.viewArticleImagePlaceholder.visibility = VISIBLE
            binding.viewArticleImage.loadImage(Uri.parse(sourceSummaryForEdit!!.getPreferredSizeThumbnailUrl()))
        }
    }

    private fun updateCaptionContents() {
        binding.viewArticleTitle.visibility = GONE
        binding.viewArticleSubtitleContainer.visibility = VISIBLE

        val descriptionText = when {
            addedContribution.isNotEmpty() -> addedContribution
            sourceSummaryForEdit!!.description!!.isNotEmpty() -> sourceSummaryForEdit!!.description!!
            else -> getString(R.string.suggested_edits_no_description)
        }

        binding.viewArticleSubtitle.text = StringUtil.strip(StringUtil.removeHTMLTags(descriptionText))
        binding.viewImageFileName.setDetailText(StringUtil.removeNamespace(sourceSummaryForEdit!!.displayTitle!!))

        if (!sourceSummaryForEdit!!.user.isNullOrEmpty()) {
            binding.viewImageArtist.setTitleText(getString(R.string.suggested_edits_image_caption_summary_title_author))
            binding.viewImageArtist.setDetailText(sourceSummaryForEdit!!.user)
        } else {
            binding.viewImageArtist.setTitleText(StringUtil.removeHTMLTags(sourceSummaryForEdit!!.metadata!!.artist()))
        }

        binding.viewImageDate.setDetailText(DateUtil.getLastSyncDateString(sourceSummaryForEdit!!.timestamp!!))
        binding.viewImageSource.setDetailText(sourceSummaryForEdit!!.metadata!!.credit())
        binding.viewImageLicense.setDetailText(sourceSummaryForEdit!!.metadata!!.licenseShortName())

        binding.viewArticleImage.loadImage(Uri.parse(sourceSummaryForEdit!!.getPreferredSizeThumbnailUrl()))
        binding.viewArticleExtract.visibility = GONE
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsCardsItemFragment()
        }
    }
}
