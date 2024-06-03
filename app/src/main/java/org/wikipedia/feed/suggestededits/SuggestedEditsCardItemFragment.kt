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
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource.FEED
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentSuggestedEditsCardItemBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_CAPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_IMAGE_TAGS
import org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION
import org.wikipedia.descriptions.DescriptionEditReviewView.Companion.ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
import org.wikipedia.descriptions.DescriptionEditReviewView.Companion.ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
import org.wikipedia.extensions.parcelable
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity
import org.wikipedia.suggestededits.SuggestedEditsSnackbars
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil

class SuggestedEditsCardItemFragment : Fragment() {
    private var _binding: FragmentSuggestedEditsCardItemBinding? = null
    private val binding get() = _binding!!

    private lateinit var cardActionType: Action
    private var age = 0
    private var app = WikipediaApp.instance
    private var appLanguages = app.languageState.appLanguageCodes
    private var langFromCode: String = appLanguages[0]
    private var targetLanguage: String? = null
    private var sourceSummaryForEdit: PageSummaryForEdit? = null
    private var targetSummaryForEdit: PageSummaryForEdit? = null
    private var imageTagPage: MwQueryPage? = null
    private var itemClickable = false
    private var previousImageTagPage: MwQueryPage? = null
    private var previousSourceSummaryForEdit: PageSummaryForEdit? = null

    private val requestSuggestedEditsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            if (isAdded) {
                previousImageTagPage = imageTagPage
                previousSourceSummaryForEdit = sourceSummaryForEdit

                val openPageListener = SuggestedEditsSnackbars.OpenPageListener {
                    if (cardActionType === ADD_IMAGE_TAGS) {
                        startActivity(FilePageActivity.newIntent(requireActivity(), PageTitle(previousImageTagPage!!.title, WikiSite(appLanguages[0]))))
                        return@OpenPageListener
                    }
                    val pageTitle = previousSourceSummaryForEdit!!.pageTitle
                    if (cardActionType === ADD_CAPTION || cardActionType === TRANSLATE_CAPTION) {
                        startActivity(GalleryActivity.newIntent(requireActivity(), pageTitle, pageTitle.prefixedText, pageTitle.wikiSite, 0))
                    } else {
                        startActivity(PageActivity.newIntentForNewTab(requireContext(), HistoryEntry(pageTitle, HistoryEntry.SOURCE_SUGGESTED_EDITS), pageTitle))
                    }
                }
                SuggestedEditsSnackbars.show(requireActivity(), cardActionType, true, targetLanguage, true, openPageListener)
                showCardContent()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            age = it.getInt(AGE)
            val pageSummary = it.parcelable<SuggestedEditsFeedClient.SuggestedEditsSummary>(PAGE_SUMMARY)
            if (pageSummary != null) {
                sourceSummaryForEdit = pageSummary.sourceSummaryForEdit
                targetSummaryForEdit = pageSummary.targetSummaryForEdit
                cardActionType = pageSummary.cardActionType
                targetLanguage = targetSummaryForEdit?.lang
            } else {
                cardActionType = ADD_IMAGE_TAGS
                imageTagPage = JsonUtil.decodeFromString(it.getString(IMAGE_TAG_PAGE))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSuggestedEditsCardItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateContents()
    }

    private fun updateContents() {
        binding.cardItemContainer.setOnClickListener(startDescriptionEditScreenListener())
        binding.callToActionButton.setOnClickListener(startDescriptionEditScreenListener())
        binding.seCardErrorView.backClickListener = View.OnClickListener {
            binding.seCardErrorView.visibility = GONE
            showCardContent()
        }
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
        if (cardActionType == ADD_IMAGE_TAGS) {
            imageTagPage?.let {
                requestSuggestedEditsLauncher.launch(SuggestedEditsImageTagEditActivity.newIntent(requireActivity(), it, FEED))
            }
            return
        }
        sourceSummaryForEdit?.let {
            val pageTitle = if (cardActionType == TRANSLATE_DESCRIPTION || cardActionType == TRANSLATE_CAPTION) targetSummaryForEdit!!.pageTitle else it.pageTitle
            requestSuggestedEditsLauncher.launch(DescriptionEditActivity.newIntent(
                requireContext(), pageTitle, null, sourceSummaryForEdit, targetSummaryForEdit, cardActionType, FEED
            ))
        }
    }

    private fun showCardContent() {
        if (!isAdded || (cardActionType != ADD_IMAGE_TAGS && sourceSummaryForEdit == null)) {
            return
        }
        itemClickable = true
        binding.seFeedCardProgressBar.visibility = GONE
        binding.seCardErrorView.visibility = GONE
        binding.callToActionButton.visibility = VISIBLE
        sourceSummaryForEdit?.let {
            val langCode = targetSummaryForEdit?.lang ?: it.lang
            L10nUtil.setConditionalLayoutDirection(binding.cardView, langCode)
        }

        when (cardActionType) {
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
        binding.viewArticleExtract.text = StringUtil.removeNamespace(imageTagPage!!.title)
    }

    private fun showAddDescriptionUI() {
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_description_button)
        binding.articleDescriptionPlaceHolder1.visibility = VISIBLE
        binding.articleDescriptionPlaceHolder2.visibility = VISIBLE
        binding.viewArticleTitle.visibility = VISIBLE
        binding.divider.visibility = VISIBLE
        binding.viewArticleTitle.text = StringUtil.fromHtml(sourceSummaryForEdit?.displayTitle)
        binding.viewArticleExtract.text = StringUtil.fromHtml(sourceSummaryForEdit?.extract)
        binding.viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        showItemImage()
    }

    private fun showTranslateDescriptionUI() {
        showAddDescriptionUI()
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_translation_in_language_button,
                app.languageState.getAppLanguageCanonicalName(targetLanguage))
        binding.viewArticleSubtitle.visibility = VISIBLE
        binding.viewArticleSubtitle.text = sourceSummaryForEdit?.description
    }

    private fun showAddImageCaptionUI() {
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_image_caption)
        binding.viewArticleTitle.visibility = GONE
        binding.viewArticleExtract.visibility = VISIBLE
        binding.viewArticleExtract.text = StringUtil.removeNamespace(sourceSummaryForEdit?.displayTitle.orEmpty())
        showItemImage()
    }

    private fun showTranslateImageCaptionUI() {
        showAddImageCaptionUI()
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_translate_image_caption,
                app.languageState.getAppLanguageCanonicalName(targetLanguage))
        binding.viewArticleSubtitle.visibility = VISIBLE
        binding.viewArticleSubtitle.text = sourceSummaryForEdit?.description
    }

    private fun showItemImage() {
        binding.viewArticleImage.visibility = VISIBLE
        if (cardActionType == ADD_IMAGE_TAGS) {
            binding.viewArticleImage.loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize
            (imageTagPage!!.imageInfo()!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)))
        } else {
            if (sourceSummaryForEdit!!.thumbnailUrl.isNullOrBlank()) {
                binding.viewArticleImage.visibility = GONE
                binding.viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
            } else {
                binding.viewArticleImage.loadImage(Uri.parse(sourceSummaryForEdit!!.thumbnailUrl))
                binding.viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
            }
        }
    }

    fun showError(caught: Throwable?) {
        binding.seFeedCardProgressBar.visibility = GONE
        binding.seCardErrorView.setError(caught)
        binding.seCardErrorView.visibility = VISIBLE
        binding.seCardErrorView.bringToFront()
    }

    companion object {
        private const val AGE = "age"
        private const val PAGE_SUMMARY = "pageSummary"
        private const val IMAGE_TAG_PAGE = "imageTagPage"
        const val MAX_RETRY_LIMIT = 5L

        fun newInstance(age: Int, pageSummary: SuggestedEditsFeedClient.SuggestedEditsSummary?, imageTagPage: MwQueryPage?) =
                SuggestedEditsCardItemFragment().apply {
                    arguments = bundleOf(AGE to age, PAGE_SUMMARY to pageSummary, IMAGE_TAG_PAGE to JsonUtil.encodeToString(imageTagPage))
                }
    }
}
