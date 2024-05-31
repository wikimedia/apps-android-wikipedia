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
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.FragmentSuggestedEditsCardsItemBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_CAPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_DESCRIPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION
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
            if (Prefs.showImageZoomTooltip) {
                Prefs.showImageZoomTooltip = false
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
                        }, { setErrorState(it) }))
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
        binding.viewArticleImage.contentDescription = getString(R.string.image_content_description, sourceSummaryForEdit?.displayTitle)
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
        binding.viewArticleTitle.text = StringUtil.fromHtml(sourceSummaryForEdit?.displayTitle)
        binding.viewArticleTitle.visibility = VISIBLE

        if (parent().action == TRANSLATE_DESCRIPTION) {
            binding.viewArticleSubtitleContainer.visibility = VISIBLE
            binding.viewArticleSubtitle.text = addedContribution.ifEmpty { sourceSummaryForEdit?.description }
        }

        binding.viewImageSummaryContainer.visibility = GONE

        binding.viewArticleExtract.text = StringUtil.removeHTMLTags(sourceSummaryForEdit?.extractHtml)
        if (sourceSummaryForEdit!!.thumbnailUrl.isNullOrBlank()) {
            binding.viewArticleImagePlaceholder.visibility = GONE
        } else {
            binding.viewArticleImagePlaceholder.visibility = VISIBLE
            binding.viewArticleImage.loadImage(Uri.parse(sourceSummaryForEdit?.getPreferredSizeThumbnailUrl()))
        }
    }

    private fun updateCaptionContents() {
        binding.viewArticleTitle.visibility = GONE
        binding.viewArticleSubtitleContainer.visibility = VISIBLE

        val descriptionText = addedContribution.ifEmpty {
            sourceSummaryForEdit!!.description!!.ifEmpty { getString(R.string.suggested_edits_no_description) }
        }

        binding.viewArticleSubtitle.text = StringUtil.strip(StringUtil.removeHTMLTags(descriptionText))
        binding.viewImageFileName.setDetailText(StringUtil.removeNamespace(sourceSummaryForEdit?.displayTitle.orEmpty()))

        if (!sourceSummaryForEdit?.user.isNullOrEmpty()) {
            binding.viewImageArtist.setTitleText(getString(R.string.suggested_edits_image_caption_summary_title_author))
            binding.viewImageArtist.setDetailText(sourceSummaryForEdit?.user)
        } else {
            binding.viewImageArtist.setTitleText(StringUtil.removeHTMLTags(sourceSummaryForEdit?.metadata?.artist()))
        }

        binding.viewImageDate.setDetailText(DateUtil.getTimeAndDateString(requireContext(), sourceSummaryForEdit?.timestamp.orEmpty()))
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
