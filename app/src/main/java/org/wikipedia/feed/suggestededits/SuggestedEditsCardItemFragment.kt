package org.wikipedia.feed.suggestededits

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.wikipedia.Constants
import org.wikipedia.Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT
import org.wikipedia.Constants.InvokeSource.FEED
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.FeedFunnel
import org.wikipedia.analytics.GalleryFunnel
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentSuggestedEditsCardItemBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.descriptions.DescriptionEditReviewView.Companion.ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
import org.wikipedia.descriptions.DescriptionEditReviewView.Companion.ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
import org.wikipedia.feed.model.CardType
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity
import org.wikipedia.suggestededits.SuggestedEditsSnackbars
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil

class SuggestedEditsCardItemFragment : Fragment() {
    private var _binding: FragmentSuggestedEditsCardItemBinding? = null
    private val binding get() = _binding!!

    private var age = 0
    private var cardActionType: Action? = null
    private var app = WikipediaApp.getInstance()
    private var appLanguages = app.language().appLanguageCodes
    private var langFromCode: String = appLanguages[0]
    private var targetLanguage: String? = null
    private val disposables = CompositeDisposable()
    private var sourceSummaryForEdit: PageSummaryForEdit? = null
    private var targetSummaryForEdit: PageSummaryForEdit? = null
    private var imageTagPage: MwQueryPage? = null
    private var itemClickable = false
    private var funnel = FeedFunnel(app)
    private var previousImageTagPage: MwQueryPage? = null
    private var previousSourceSummaryForEdit: PageSummaryForEdit? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            age = it.getInt(AGE)
            cardActionType = it.getSerializable(CARD_TYPE) as Action
        }
        if (appLanguages.size > 1) {
            targetLanguage = appLanguages[age % appLanguages.size]
            if (cardActionType == ADD_DESCRIPTION && !targetLanguage.equals(langFromCode))
                cardActionType = TRANSLATE_DESCRIPTION
            if (cardActionType == ADD_CAPTION && !targetLanguage.equals(appLanguages[0]))
                cardActionType = TRANSLATE_CAPTION
        }
        SuggestedEditsFunnel[FEED].impression(cardActionType!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSuggestedEditsCardItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateContents()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_REQUEST_DESCRIPTION_EDIT && resultCode == RESULT_OK) {
            if (!isAdded)
                return
            previousImageTagPage = imageTagPage
            previousSourceSummaryForEdit = sourceSummaryForEdit
            SuggestedEditsFunnel.get().log()
            SuggestedEditsFunnel.reset()

            if (cardActionType != null) {
                val openPageListener = SuggestedEditsSnackbars.OpenPageListener {
                    if (cardActionType === ADD_IMAGE_TAGS) {
                        startActivity(FilePageActivity.newIntent(requireActivity(), PageTitle(previousImageTagPage!!.title(), WikiSite(appLanguages[0]))))
                        return@OpenPageListener
                    }
                    val pageTitle: PageTitle = previousSourceSummaryForEdit!!.pageTitle
                    if (cardActionType === ADD_CAPTION || cardActionType === TRANSLATE_CAPTION) {
                        startActivity(GalleryActivity.newIntent(requireActivity(),
                            pageTitle, pageTitle.prefixedText, pageTitle.wikiSite, 0, GalleryFunnel.SOURCE_NON_LEAD_IMAGE))
                    } else {
                        startActivity(PageActivity.newIntentForNewTab(requireContext(), HistoryEntry(pageTitle, HistoryEntry.SOURCE_SUGGESTED_EDITS), pageTitle))
                    }
                }
                SuggestedEditsSnackbars.show(requireActivity(), cardActionType, true,
                        targetLanguage, true, openPageListener)
            }
            fetchCardTypeEdit()
        }
    }

    private fun updateContents() {
        binding.cardItemContainer.setOnClickListener(startDescriptionEditScreenListener())
        binding.callToActionButton.setOnClickListener(startDescriptionEditScreenListener())
        binding.seCardErrorView.backClickListener = View.OnClickListener {
            binding.seCardErrorView.visibility = GONE
            fetchCardTypeEdit()
        }
        fetchCardTypeEdit()
    }

    private fun startDescriptionEditScreenListener() = View.OnClickListener {
        if (itemClickable) {
            funnel.cardClicked(CardType.SUGGESTED_EDITS, if (targetLanguage != null && targetLanguage.equals(langFromCode)) langFromCode else targetLanguage)
            startDescriptionEditScreen()
        }
    }

    private fun startDescriptionEditScreen() {
        if (!isAdded) {
            return
        }
        if (cardActionType == ADD_IMAGE_TAGS) {
            startActivityForResult(SuggestedEditsImageTagEditActivity.newIntent(requireActivity(), imageTagPage!!, FEED), ACTIVITY_REQUEST_DESCRIPTION_EDIT)
            return
        }
        if (sourceSummaryForEdit == null) {
            return
        }
        val pageTitle: PageTitle = if (cardActionType == TRANSLATE_DESCRIPTION || cardActionType == TRANSLATE_CAPTION)
            targetSummaryForEdit!!.pageTitle else sourceSummaryForEdit!!.pageTitle

        startActivityForResult(DescriptionEditActivity.newIntent(requireContext(), pageTitle, null,
                sourceSummaryForEdit, targetSummaryForEdit,
                cardActionType!!, FEED),
                ACTIVITY_REQUEST_DESCRIPTION_EDIT)
    }

    private fun fetchCardTypeEdit() {
        binding.seFeedCardProgressBar.visibility = VISIBLE
        when (cardActionType) {
            ADD_DESCRIPTION -> addDescription()
            TRANSLATE_DESCRIPTION -> translateDescription()
            ADD_CAPTION -> addCaption()
            TRANSLATE_CAPTION -> translateCaption()
            ADD_IMAGE_TAGS -> addImageTags()
        }
    }

    override fun onDestroyView() {
        disposables.clear()
        _binding = null
        super.onDestroyView()
    }

    private fun updateUI() {
        if (!isAdded || (cardActionType != ADD_IMAGE_TAGS && sourceSummaryForEdit == null)) {
            return
        }
        itemClickable = true
        binding.seFeedCardProgressBar.visibility = GONE
        binding.seCardErrorView.visibility = GONE
        binding.callToActionButton.visibility = VISIBLE
        if (sourceSummaryForEdit != null) {
            binding.cardView.layoutDirection = if (L10nUtil.isLangRTL(if (targetSummaryForEdit != null)
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
                    val source = pair.first
                    val target = pair.second

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
                    val page = response.query?.firstPage()!!
                    page.imageInfo()?.let {
                        sourceSummaryForEdit = PageSummaryForEdit(
                            page.title(), langFromCode,
                            PageTitle(Namespace.FILE.name,
                                StringUtil.removeNamespace(page.title()),
                                null,
                                it.thumbUrl,
                                WikiSite.forLanguageCode(langFromCode)),
                            StringUtil.removeHTMLTags(page.title()),
                            it.metadata!!.imageDescription(),
                            it.thumbUrl,
                            null,
                            null,
                            it.timestamp,
                            it.user,
                            it.metadata
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
                    val page = response.query?.firstPage()!!
                    page.imageInfo()?.let {
                        sourceSummaryForEdit = PageSummaryForEdit(
                            page.title(),
                            langFromCode,
                            PageTitle(
                                Namespace.FILE.name,
                                StringUtil.removeNamespace(page.title()),
                                null,
                                it.thumbUrl,
                                WikiSite.forLanguageCode(langFromCode)
                            ),
                            StringUtil.removeHTMLTags(page.title()),
                            fileCaption,
                            it.thumbUrl,
                            null,
                            null,
                            it.timestamp,
                            it.user,
                            it.metadata
                        )

                        targetSummaryForEdit = sourceSummaryForEdit!!.copy(
                            description = null,
                            lang = targetLanguage!!,
                            pageTitle = PageTitle(
                                Namespace.FILE.name,
                                StringUtil.removeNamespace(page.title()),
                                null,
                                it.thumbUrl,
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

    private fun showImageTagsUI() {
        showAddImageCaptionUI()
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_image_tags)
        binding.viewArticleExtract.text = StringUtil.removeNamespace(imageTagPage!!.title())
    }

    private fun showAddDescriptionUI() {
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_description_button)
        binding.articleDescriptionPlaceHolder1.visibility = VISIBLE
        binding.articleDescriptionPlaceHolder2.visibility = VISIBLE
        binding.viewArticleTitle.visibility = VISIBLE
        binding.divider.visibility = VISIBLE
        binding.viewArticleTitle.text = StringUtil.fromHtml(sourceSummaryForEdit!!.displayTitle!!)
        binding.viewArticleExtract.text = StringUtil.fromHtml(sourceSummaryForEdit!!.extract)
        binding.viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        showItemImage()
    }

    private fun showTranslateDescriptionUI() {
        showAddDescriptionUI()
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_translation_in_language_button,
                app.language().getAppLanguageCanonicalName(targetLanguage))
        binding.viewArticleSubtitle.visibility = VISIBLE
        binding.viewArticleSubtitle.text = sourceSummaryForEdit?.description
    }

    private fun showAddImageCaptionUI() {
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_image_caption)
        binding.viewArticleTitle.visibility = GONE
        binding.viewArticleExtract.visibility = VISIBLE
        binding.viewArticleExtract.text = StringUtil.removeNamespace(StringUtils.defaultString(sourceSummaryForEdit?.displayTitle))
        showItemImage()
    }

    private fun showTranslateImageCaptionUI() {
        showAddImageCaptionUI()
        binding.callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_translate_image_caption,
                app.language().getAppLanguageCanonicalName(targetLanguage))
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
        private const val CARD_TYPE = "cardType"
        const val MAX_RETRY_LIMIT: Long = 5

        @JvmStatic
        fun newInstance(age: Int, cardType: Action) =
                SuggestedEditsCardItemFragment().apply {
                    arguments = bundleOf(AGE to age, CARD_TYPE to cardType)
                }
    }
}
