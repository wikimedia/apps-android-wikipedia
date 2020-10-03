package org.wikipedia.feed.suggestededits

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_card_item.*
import org.wikipedia.Constants
import org.wikipedia.Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT
import org.wikipedia.Constants.InvokeSource.FEED
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.WikiSite.forLanguageCode
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.descriptions.DescriptionEditReviewView.Companion.ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

private const val AGE = "age"
private const val CARD_TYPE = "param2"

class SuggestedEditsCardItemFragment : Fragment() {
    private var age = 0
    private var cardActionType: Action? = null
    private var app = WikipediaApp.getInstance()
    private var appLanguages = app.language().appLanguageCodes
    private var langFromCode: String = app.language().appLanguageCode
    private var targetLanguage: String? = null
    private val disposables = CompositeDisposable()
    private var sourceSummaryForEdit: PageSummaryForEdit? = null
    private var targetSummaryForEdit: PageSummaryForEdit? = null
    private var imageTagPage: MwQueryPage? = null
    private var sourceDescription: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            age = it.getInt(AGE)
            cardActionType = it.getSerializable(CARD_TYPE) as Action
        }
        if (appLanguages.size > 1) {
            targetLanguage = appLanguages[age % appLanguages.size]
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_suggested_edits_card_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateContents()
    }

    private fun updateContents() {
        seFeedCardProgressBar.visibility = VISIBLE
        cardView.visibility = GONE

        suggestedEditsFragmentViewGroup.addOnClickListener {
            Log.e("#####", "CLICK")
            startDescriptionEditScreen()

        }
        seFeedCardProgressBar.bringToFront()
        when (cardActionType) {
            ADD_DESCRIPTION -> if (targetLanguage == null || targetLanguage.equals(appLanguages[0])) addDescription() else translateDescription()
            ADD_CAPTION -> if (targetLanguage == null || targetLanguage.equals(appLanguages[0])) addCaption() else translateCaption()
            else -> tagImage()
        }
    }

    private fun Group.addOnClickListener(listener: View.OnClickListener) {
        referencedIds.forEach { id ->
            cardView.findViewById<View>(id).setOnClickListener(listener)
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
        val pageTitle: PageTitle = if (cardActionType == TRANSLATE_DESCRIPTION || cardActionType == TRANSLATE_CAPTION)
            targetSummaryForEdit!!.pageTitle else sourceSummaryForEdit!!.pageTitle

        startActivityForResult(DescriptionEditActivity.newIntent(requireContext(), pageTitle, null,
                sourceSummaryForEdit, targetSummaryForEdit,
                cardActionType!!, FEED),
                ACTIVITY_REQUEST_DESCRIPTION_EDIT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_REQUEST_DESCRIPTION_EDIT) {
            /* SuggestedEditsFunnel.get().log()
             SuggestedEditsFunnel.reset()*/
            if (resultCode == Activity.RESULT_OK) {
                /* if (suggestedEditsCardItemView != null && suggestedEditsCardItemView.getCard() != null) {
                   suggestedEditsCardView.refreshCardContent();
                     SuggestedEditsSnackbars.show(requireActivity(), suggestedEditsCardView.getCard().getAction(), true,
                             app.language().getAppLanguageCodes().get(1), true, () -> {
                         PageTitle pageTitle = suggestedEditsCardView.getCard().getSourceSummaryForEdit().getPageTitle();
                         if (suggestedEditsCardView.getCard().getAction() == ADD_IMAGE_TAGS) {
                             startActivity(FilePageActivity.newIntent(requireActivity(), pageTitle));
                         } else if (suggestedEditsCardView.getCard().getAction() == ADD_CAPTION || suggestedEditsCardView.getCard().getAction() == TRANSLATE_CAPTION) {
                             startActivity(GalleryActivity.newIntent(requireActivity(),
                                     pageTitle, pageTitle.getPrefixedText(), pageTitle.getWikiSite(), 0, GalleryFunnel.SOURCE_NON_LEAD_IMAGE));
                         } else {
                             startActivity(PageActivity.newIntentForNewTab(requireContext(), new HistoryEntry(pageTitle, HistoryEntry.SOURCE_SUGGESTED_EDITS), pageTitle));
                         }
                     });
                   }*/
            }
        }
    }

    fun getCardView(): View {
        return cardView
    }

    override fun onDestroyView() {
        //funnel?.stop()
        disposables.clear()
        super.onDestroyView()
    }

    private fun updateUI() {
        seFeedCardProgressBar.visibility = GONE
        cardView.visibility = VISIBLE
        viewArticleSubtitle.visibility = GONE
        when (cardActionType) {
            TRANSLATE_DESCRIPTION -> showTranslateDescriptionUI()
            ADD_CAPTION -> showAddImageCaptionUI()
            TRANSLATE_CAPTION -> showTranslateImageCaptionUI()
            ADD_IMAGE_TAGS -> showImageTagsUI()
            else -> showAddDescriptionUI()
        }

    }

    private fun showImageTagsUI() {
        articleDescriptionPlaceHolder1.visibility = GONE
        articleDescriptionPlaceHolder2.visibility = GONE
        viewArticleImage.visibility = VISIBLE
        viewArticleExtract.visibility = GONE
        divider.visibility = GONE
        viewArticleImage.loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize(imageTagPage!!.imageInfo()!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)))
        viewArticleTitle.visibility = GONE
    }

    private fun showAddDescriptionUI() {
        articleDescriptionPlaceHolder1.visibility = VISIBLE
        articleDescriptionPlaceHolder2.visibility = VISIBLE
        viewArticleTitle.visibility = VISIBLE
        viewArticleTitle.text = StringUtil.fromHtml(sourceSummaryForEdit!!.displayTitle!!)
        showImageOrExtract()
    }

    private fun showTranslateDescriptionUI() {
        articleDescriptionPlaceHolder1.visibility = GONE
        articleDescriptionPlaceHolder2.visibility = GONE
        viewArticleTitle.visibility = VISIBLE
        sourceDescription = sourceSummaryForEdit!!.description!!
        viewArticleSubtitle.visibility = VISIBLE
        viewArticleSubtitle.text = sourceDescription
        showAddDescriptionUI()
    }

    private fun showAddImageCaptionUI() {
        articleDescriptionPlaceHolder1.visibility = GONE
        articleDescriptionPlaceHolder2.visibility = GONE
        viewArticleTitle.visibility = VISIBLE
        viewArticleImage.visibility = VISIBLE
        viewArticleExtract.visibility = GONE
        divider.visibility = GONE
        viewArticleImage.loadImage(Uri.parse(sourceSummaryForEdit!!.thumbnailUrl))
        viewArticleTitle.text = StringUtil.removeNamespace(sourceSummaryForEdit!!.displayTitle!!)
    }

    private fun showTranslateImageCaptionUI() {
        articleDescriptionPlaceHolder1.visibility = GONE
        articleDescriptionPlaceHolder2.visibility = GONE
        viewArticleTitle.visibility = VISIBLE
        sourceDescription = sourceSummaryForEdit!!.description!!
        viewArticleSubtitle.visibility = VISIBLE
        viewArticleSubtitle.text = sourceDescription
        showAddImageCaptionUI()
    }

    private fun showImageOrExtract() {
        if (sourceSummaryForEdit!!.thumbnailUrl.isNullOrBlank()) {
            viewArticleImage.visibility = GONE
            viewArticleExtract.visibility = VISIBLE
            divider.visibility = VISIBLE
            viewArticleExtract.text = StringUtil.fromHtml(sourceSummaryForEdit!!.extractHtml)
            viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            viewArticleImage.visibility = VISIBLE
            viewArticleExtract.visibility = GONE
            divider.visibility = GONE
            viewArticleImage.loadImage(Uri.parse(sourceSummaryForEdit!!.thumbnailUrl))
        }
    }

    private fun addDescription() {
        callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_description_button)
        disposables.add(EditingSuggestionsProvider
                .getNextArticleWithMissingDescription(forLanguageCode(langFromCode), MAX_RETRY_LIMIT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pageSummary ->
                    sourceSummaryForEdit = PageSummaryForEdit(
                            pageSummary.apiTitle,
                            langFromCode,
                            pageSummary.getPageTitle(forLanguageCode(langFromCode)),
                            pageSummary.displayTitle,
                            pageSummary.description,
                            pageSummary.thumbnailUrl,
                            pageSummary.extractHtml
                    )
                    updateUI()
                }, {
                    L.e(it)
                }))
    }

    private fun translateDescription() {
        cardActionType = TRANSLATE_DESCRIPTION
        callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_translation_in_language_button,
                app.language().getAppLanguageCanonicalName(targetLanguage))
        if (targetLanguage!!.isEmpty()) {
            return
        }
        disposables.add(EditingSuggestionsProvider
                .getNextArticleWithMissingDescription(forLanguageCode(langFromCode), targetLanguage!!, true, MAX_RETRY_LIMIT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pair ->
                    val source = pair.second
                    val target = pair.first

                    sourceSummaryForEdit = PageSummaryForEdit(
                            source.apiTitle,
                            langFromCode,
                            source.getPageTitle(forLanguageCode(langFromCode)),
                            source.displayTitle,
                            source.description,
                            source.thumbnailUrl,
                            source.extractHtml
                    )

                    targetSummaryForEdit = PageSummaryForEdit(
                            target.apiTitle,
                            targetLanguage!!,
                            target.getPageTitle(forLanguageCode(targetLanguage!!)),
                            target.displayTitle,
                            target.description,
                            target.thumbnailUrl,
                            target.extractHtml
                    )
                    updateUI()
                }, {
                    L.e(it)
                }))
    }

    private fun addCaption() {
        callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_image_caption)
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
                                        forLanguageCode(langFromCode)
                                ),
                                StringUtil.removeHTMLTags(title),
                                imageInfo.metadata!!.imageDescription(),
                                imageInfo.thumbUrl,
                                null,
                                imageInfo.timestamp,
                                imageInfo.user,
                                imageInfo.metadata
                        )
                        updateUI()
                    }
                }, {
                    L.e(it)
                }))
    }

    private fun translateCaption() {
        cardActionType = TRANSLATE_CAPTION
        callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_translate_image_caption,
                app.language().getAppLanguageCanonicalName(targetLanguage))
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
                                        forLanguageCode(langFromCode)
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
                                lang = targetLanguage!!,
                                pageTitle = PageTitle(
                                        Namespace.FILE.name,
                                        StringUtil.removeNamespace(title),
                                        null,
                                        imageInfo.thumbUrl,
                                        forLanguageCode(targetLanguage!!)
                                )
                        )
                    }
                }, {
                    L.e(it)
                }))
    }

    private fun tagImage() {
        callToActionButton.text = context?.getString(R.string.suggested_edits_feed_card_add_image_tags)
        disposables.add(EditingSuggestionsProvider
                .getNextImageWithMissingTags(MAX_RETRY_LIMIT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ page ->
                    imageTagPage = page
                    updateUI()
                }, {
                    L.e(it)
                }))
    }

    companion object {
        const val MAX_RETRY_LIMIT: Long = 5

        @JvmStatic
        fun newInstance(age: Int, cardType: Action) =
                SuggestedEditsCardItemFragment().apply {
                    arguments = Bundle().apply {
                        putInt(AGE, age)
                        putSerializable(CARD_TYPE, cardType)
                    }
                }
    }
}