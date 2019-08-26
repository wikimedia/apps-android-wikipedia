package org.wikipedia.suggestededits

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_cards_item.*
import kotlinx.android.synthetic.main.view_image_detail_horizontal.view.*
import org.wikipedia.Constants.InvokeSource.*
import org.wikipedia.R
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.provider.MissingDescriptionProvider
import org.wikipedia.util.DateUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class SuggestedEditsCardsItemFragment : Fragment() {
    private val disposables = CompositeDisposable()
    var sourceSummary: SuggestedEditsSummary? = null
    var targetSummary: SuggestedEditsSummary? = null
    var addedContribution: String = ""
        internal set
    var pagerPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_cards_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setConditionalLayoutDirection(viewArticleContainer, parent().langFromCode)
        viewArticleImage.setLegacyVisibilityHandlingEnabled(true)
        cardItemErrorView.setBackClickListener { requireActivity().finish() }
        cardItemErrorView.setRetryClickListener {
            cardItemProgressBar.visibility = VISIBLE
            getArticleWithMissingDescription()
        }
        updateContents()
        if (sourceSummary == null) {
            getArticleWithMissingDescription()
        }

        cardClickArea.setOnClickListener {
            if (sourceSummary != null) {
                parent().onSelectPage()
            }
        }
        showAddedContributionView(addedContribution)
        if (savedInstanceState != null) {
            pagerPosition = savedInstanceState.getInt("pagerPosition", -1)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("pagerPosition", pagerPosition)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun getArticleWithMissingDescription() {
        when (parent().source) {
            SUGGESTED_EDITS_TRANSLATE_DESC -> {
                disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(parent().langFromCode), parent().langToCode, true)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ pair ->
                            val source = pair.second
                            val target = pair.first

                            sourceSummary = SuggestedEditsSummary(
                                    source.title,
                                    source.lang,
                                    source.getPageTitle(WikiSite.forLanguageCode(parent().langFromCode)),
                                    source.normalizedTitle,
                                    source.displayTitle,
                                    source.description,
                                    source.thumbnailUrl,
                                    source.extractHtml,
                                    null, null, null
                            )

                            targetSummary = SuggestedEditsSummary(
                                    target.title,
                                    target.lang,
                                    target.getPageTitle(WikiSite.forLanguageCode(parent().langToCode)),
                                    target.normalizedTitle,
                                    target.displayTitle,
                                    target.description,
                                    target.thumbnailUrl,
                                    target.extractHtml,
                                    null, null, null
                            )
                            updateContents()
                        }, { this.setErrorState(it) })!!)
            }

            SUGGESTED_EDITS_ADD_CAPTION -> {
                disposables.add(MissingDescriptionProvider.getNextImageWithMissingCaption(parent().langFromCode)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap { title ->
                            ServiceFactory.get(WikiSite.forLanguageCode(parent().langFromCode)).getImageExtMetadata(title)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                        }
                        .subscribe({ response ->
                            val page = response.query()!!.pages()!![0]
                            if (page.imageInfo() != null) {
                                val imageInfo = page.imageInfo()!!
                                val title = if (imageInfo.commonsUrl.isEmpty()) page.title() else WikiSite(Service.COMMONS_URL).titleForUri(Uri.parse(imageInfo.commonsUrl)).prefixedText

                                sourceSummary = SuggestedEditsSummary(
                                        title,
                                        parent().langFromCode,
                                        PageTitle(
                                                Namespace.FILE.name,
                                                StringUtil.removeNamespace(title),
                                                null,
                                                imageInfo.thumbUrl,
                                                WikiSite.forLanguageCode(parent().langFromCode)
                                        ),
                                        StringUtil.removeUnderscores(title),
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

            SUGGESTED_EDITS_TRANSLATE_CAPTION -> {
                var fileCaption: String? = null
                disposables.add(MissingDescriptionProvider.getNextImageWithMissingCaption(parent().langFromCode, parent().langToCode)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap { pair ->
                            fileCaption = pair.first
                            ServiceFactory.get(WikiSite.forLanguageCode(parent().langFromCode)).getImageExtMetadata(pair.second)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                        }
                        .subscribe({ response ->
                            val page = response.query()!!.pages()!![0]
                            if (page.imageInfo() != null) {
                                val imageInfo = page.imageInfo()!!
                                val title = if (imageInfo.commonsUrl.isEmpty()) page.title() else WikiSite(Service.COMMONS_URL).titleForUri(Uri.parse(imageInfo.commonsUrl)).prefixedText

                                sourceSummary = SuggestedEditsSummary(
                                        title,
                                        parent().langFromCode,
                                        PageTitle(
                                                Namespace.FILE.name,
                                                StringUtil.removeNamespace(title),
                                                null,
                                                imageInfo.thumbUrl,
                                                WikiSite.forLanguageCode(parent().langFromCode)
                                        ),
                                        StringUtil.removeUnderscores(title),
                                        StringUtil.removeHTMLTags(title),
                                        fileCaption,
                                        imageInfo.thumbUrl,
                                        null,
                                        imageInfo.timestamp,
                                        imageInfo.user,
                                        imageInfo.metadata
                                )

                                targetSummary = sourceSummary!!.copy(
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
                disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(parent().langFromCode))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ pageSummary ->
                            sourceSummary = SuggestedEditsSummary(
                                    pageSummary.title,
                                    pageSummary.lang,
                                    pageSummary.getPageTitle(WikiSite.forLanguageCode(pageSummary.lang)),
                                    pageSummary.normalizedTitle,
                                    pageSummary.displayTitle,
                                    pageSummary.description,
                                    pageSummary.thumbnailUrl,
                                    pageSummary.extractHtml,
                                    null, null, null
                            )
                            updateContents()
                        }, { this.setErrorState(it) }))
            }
        }
    }

    fun showAddedContributionView(addedContribution: String?) {
        if (!addedContribution.isNullOrEmpty()) {
            viewArticleSubtitleContainer.visibility = VISIBLE
            viewArticleSubtitle.text = addedContribution
            viewArticleExtract.maxLines = viewArticleExtract.maxLines - 1
            this.addedContribution = addedContribution
        }
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        cardItemErrorView.setError(t)
        cardItemErrorView.visibility = VISIBLE
        cardItemProgressBar.visibility = GONE
        cardItemContainer.visibility = GONE
    }

    private fun updateContents() {
        val sourceAvailable = sourceSummary != null
        cardItemErrorView.visibility = GONE
        cardItemContainer.visibility = if (sourceAvailable) VISIBLE else GONE
        cardItemProgressBar.visibility = if (sourceAvailable) GONE else VISIBLE
        if (!sourceAvailable) {
            return
        }

        if (parent().source == SUGGESTED_EDITS_ADD_DESC || parent().source == SUGGESTED_EDITS_TRANSLATE_DESC) {
            updateDescriptionContents()
        } else {
            updateCaptionContents()
        }
    }

    private fun updateDescriptionContents() {
        viewArticleTitle.text = StringUtil.fromHtml(sourceSummary!!.displayTitle)

        if (parent().source == SUGGESTED_EDITS_TRANSLATE_DESC) {
            viewArticleSubtitleContainer.visibility = VISIBLE
            viewArticleSubtitle.text = (if (addedContribution.isNotEmpty()) addedContribution else sourceSummary!!.description)?.capitalize()
        }

        viewImageSummaryContainer.visibility = GONE

        viewArticleExtract.text = StringUtil.removeHTMLTags(sourceSummary!!.extractHtml!!)
        if (sourceSummary!!.thumbnailUrl.isNullOrBlank()) {
            viewArticleImage.visibility = GONE
            viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            viewArticleImage.visibility = VISIBLE
            viewArticleImage.loadImage(Uri.parse(sourceSummary!!.getPreferredSizeThumbnailUrl()))
            viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
        }
    }

    private fun updateCaptionContents() {
        viewArticleTitle.text = StringUtil.removeNamespace(sourceSummary!!.displayTitle!!)
        viewArticleSubtitleContainer.visibility = VISIBLE

        val descriptionText = when {
            addedContribution.isNotEmpty() -> addedContribution
            sourceSummary!!.description!!.isNotEmpty() -> sourceSummary!!.description!!
            else -> getString(R.string.suggested_edits_no_description)
        }

        viewArticleSubtitle.text = StringUtil.strip(StringUtil.removeHTMLTags(descriptionText.capitalize()))

        if (!sourceSummary!!.user.isNullOrEmpty()) {
            viewImageArtist!!.titleText.text = getString(R.string.suggested_edits_image_caption_summary_title_author)
            viewImageArtist!!.setDetailText(sourceSummary!!.user)
        } else {
            viewImageArtist!!.titleText.text = StringUtil.removeHTMLTags(sourceSummary!!.metadata!!.artist())
        }

        viewImageDate!!.setDetailText(DateUtil.getReadingListsLastSyncDateString(sourceSummary!!.timestamp!!))
        viewImageSource!!.setDetailText(sourceSummary!!.metadata!!.credit())
        viewImageLicense!!.setDetailText(sourceSummary!!.metadata!!.licenseShortName())

        viewArticleImage.loadImage(Uri.parse(sourceSummary!!.getPreferredSizeThumbnailUrl()))
        viewArticleExtract.visibility = GONE
    }

    private fun parent(): SuggestedEditsCardsFragment {
        return requireActivity().supportFragmentManager.fragments[0] as SuggestedEditsCardsFragment
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE = 5
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 12

        fun newInstance(): SuggestedEditsCardsItemFragment {
            return SuggestedEditsCardsItemFragment()
        }
    }
}
