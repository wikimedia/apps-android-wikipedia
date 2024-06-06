package org.wikipedia.feed.suggestededits

import android.app.Activity.RESULT_OK
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource.FEED
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentSuggestedEditsCardItemBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_CAPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_IMAGE_TAGS
import org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION
import org.wikipedia.descriptions.DescriptionEditReviewView.Companion.ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
import org.wikipedia.descriptions.DescriptionEditReviewView.Companion.ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity
import org.wikipedia.suggestededits.SuggestedEditsSnackbars
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil

class SuggestedEditsCardItemFragment : Fragment() {
    private var _binding: FragmentSuggestedEditsCardItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SuggestedEditsCardItemViewModel by viewModels { SuggestedEditsCardItemViewModel.Factory(requireArguments()) }

    private var itemClickable = false

    private val requestSuggestedEditsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            if (isAdded) {
                val openPageListener = SuggestedEditsSnackbars.OpenPageListener {
                    if (viewModel.cardActionType === ADD_IMAGE_TAGS) {
                        startActivity(FilePageActivity.newIntent(requireActivity(), PageTitle(viewModel.imageTagPage?.title, WikiSite(WikipediaApp.instance.appOrSystemLanguageCode))))
                        return@OpenPageListener
                    }
                    val pageTitle = viewModel.sourceSummaryForEdit!!.pageTitle
                    if (viewModel.cardActionType === ADD_CAPTION || viewModel.cardActionType === TRANSLATE_CAPTION) {
                        startActivity(GalleryActivity.newIntent(requireActivity(), pageTitle, pageTitle.prefixedText, pageTitle.wikiSite, 0))
                    } else {
                        startActivity(PageActivity.newIntentForNewTab(requireContext(), HistoryEntry(pageTitle, HistoryEntry.SOURCE_SUGGESTED_EDITS), pageTitle))
                    }
                }
                SuggestedEditsSnackbars.show(requireActivity(), viewModel.cardActionType, true, viewModel.targetSummaryForEdit?.lang, true, openPageListener)
                showCardContent()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSuggestedEditsCardItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.seCardErrorView.backClickListener = View.OnClickListener { viewModel.fetchCardData() }
        binding.seCardErrorView.retryClickListener = View.OnClickListener { viewModel.fetchCardData() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect {
                        when (it) {
                            is Resource.Loading -> onLoading()
                            is Resource.Success -> updateContents()
                            is Resource.Error -> onError(it.throwable)
                        }
                    }
                }
            }
        }
    }

    private fun updateContents() {
        binding.cardItemContainer.setOnClickListener(startDescriptionEditScreenListener())
        binding.callToActionButton.setOnClickListener(startDescriptionEditScreenListener())
        showCardContent()
    }

    private fun startDescriptionEditScreenListener() = View.OnClickListener {
        if (itemClickable) {
            startDescriptionEditScreen()
        }
    }

    private fun startDescriptionEditScreen() {
        if (!isAdded) {
            return
        }
        if (viewModel.cardActionType == ADD_IMAGE_TAGS) {
            viewModel.imageTagPage?.let {
                requestSuggestedEditsLauncher.launch(SuggestedEditsImageTagEditActivity.newIntent(requireActivity(), it, FEED))
            }
            return
        }
        viewModel.sourceSummaryForEdit?.let {
            val pageTitle = if (viewModel.cardActionType == TRANSLATE_DESCRIPTION || viewModel.cardActionType == TRANSLATE_CAPTION) viewModel.targetSummaryForEdit!!.pageTitle else it.pageTitle
            requestSuggestedEditsLauncher.launch(DescriptionEditActivity.newIntent(
                requireContext(), pageTitle, null, viewModel.sourceSummaryForEdit, viewModel.targetSummaryForEdit, viewModel.cardActionType, FEED
            ))
        }
    }

    private fun showCardContent() {
        if (!isAdded || (viewModel.cardActionType != ADD_IMAGE_TAGS && viewModel.sourceSummaryForEdit == null)) {
            return
        }
        itemClickable = true
        binding.seFeedCardProgressBar.visibility = GONE
        binding.seCardErrorView.visibility = GONE
        binding.callToActionButton.visibility = VISIBLE
        viewModel.sourceSummaryForEdit?.let {
            val langCode = viewModel.targetSummaryForEdit?.lang ?: it.lang
            L10nUtil.setConditionalLayoutDirection(binding.cardView, langCode)
        }

        when (viewModel.cardActionType) {
            TRANSLATE_DESCRIPTION -> showTranslateDescriptionUI()
            ADD_CAPTION -> showAddImageCaptionUI()
            TRANSLATE_CAPTION -> showTranslateImageCaptionUI()
            ADD_IMAGE_TAGS -> showImageTagsUI()
            else -> showAddDescriptionUI()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun showImageTagsUI() {
        showAddImageCaptionUI()
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_image_tags)
        binding.viewArticleExtract.text = StringUtil.removeNamespace(viewModel.imageTagPage!!.title)
    }

    private fun showAddDescriptionUI() {
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_description_button)
        binding.articleDescriptionPlaceHolder1.visibility = VISIBLE
        binding.articleDescriptionPlaceHolder2.visibility = VISIBLE
        binding.viewArticleTitle.visibility = VISIBLE
        binding.divider.visibility = VISIBLE
        binding.viewArticleTitle.text = StringUtil.fromHtml(viewModel.sourceSummaryForEdit?.displayTitle)
        binding.viewArticleExtract.text = StringUtil.fromHtml(viewModel.sourceSummaryForEdit?.extract)
        binding.viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        showItemImage()
    }

    private fun showTranslateDescriptionUI() {
        showAddDescriptionUI()
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_translation_in_language_button,
                WikipediaApp.instance.languageState.getAppLanguageCanonicalName(viewModel.targetSummaryForEdit?.lang))
        binding.viewArticleSubtitle.visibility = VISIBLE
        binding.viewArticleSubtitle.text = viewModel.sourceSummaryForEdit?.description
    }

    private fun showAddImageCaptionUI() {
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_image_caption)
        binding.viewArticleTitle.visibility = GONE
        binding.viewArticleExtract.visibility = VISIBLE
        binding.viewArticleExtract.text = StringUtil.removeNamespace(viewModel.sourceSummaryForEdit?.displayTitle.orEmpty())
        showItemImage()
    }

    private fun showTranslateImageCaptionUI() {
        showAddImageCaptionUI()
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_translate_image_caption,
                WikipediaApp.instance.languageState.getAppLanguageCanonicalName(viewModel.targetSummaryForEdit?.lang))
        binding.viewArticleSubtitle.visibility = VISIBLE
        binding.viewArticleSubtitle.text = viewModel.sourceSummaryForEdit?.description
    }

    private fun showItemImage() {
        binding.viewArticleImage.visibility = VISIBLE
        if (viewModel.cardActionType == ADD_IMAGE_TAGS) {
            binding.viewArticleImage.loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize
            (viewModel.imageTagPage!!.imageInfo()!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)))
        } else {
            if (viewModel.sourceSummaryForEdit!!.thumbnailUrl.isNullOrBlank()) {
                binding.viewArticleImage.visibility = GONE
                binding.viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
            } else {
                binding.viewArticleImage.loadImage(Uri.parse(viewModel.sourceSummaryForEdit!!.thumbnailUrl))
                binding.viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
            }
        }
    }

    fun onLoading() {
        binding.seFeedCardProgressBar.visibility = VISIBLE
        binding.seCardErrorView.visibility = GONE
    }

    fun onError(caught: Throwable?) {
        binding.seFeedCardProgressBar.visibility = GONE
        binding.seCardErrorView.setError(caught)
        binding.seCardErrorView.visibility = VISIBLE
        binding.seCardErrorView.bringToFront()
    }

    companion object {
        const val EXTRA_AGE = "age"
        const val EXTRA_ACTION_TYPE = "actionType"
        const val MAX_RETRY_LIMIT = 5L

        fun newInstance(age: Int, cardActionType: DescriptionEditActivity.Action) =
                SuggestedEditsCardItemFragment().apply {
                    arguments = bundleOf(
                        EXTRA_AGE to age,
                        EXTRA_ACTION_TYPE to cardActionType
                    )
                }
    }
}
