package org.wikipedia.feed.suggestededits

import android.content.Context
import androidx.core.net.toUri
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_suggested_edit_card.view.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.StringUtil

class SuggestedEditsCardView(context: Context) : DefaultFeedCardView<SuggestedEditsCard>(context), SuggestedEditsFeedClient.Callback {
    interface Callback {
        fun onSuggestedEditsCardClick(view: SuggestedEditsCardView)
    }

    private var sourceDescription: String = ""
    private val app = WikipediaApp.getInstance()
    private var card: SuggestedEditsCard? = null

    init {
        inflate(getContext(), R.layout.view_suggested_edit_card, this)
    }

    override fun setCard(card: SuggestedEditsCard) {
        super.setCard(card)
        this.card = card

        if (card.sourceSummaryForEdit != null) {
            setLayoutDirectionByWikiSite(WikiSite.forLanguageCode(card.sourceSummaryForEdit.lang), this)
        }

        cardItemContainer.setOnClickListener {
            if (callback != null) {
                callback!!.onSuggestedEditsCardClick(this)
            }
        }
        header(card)
        updateContents()
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
        headerView.setCallback(callback)
    }

    private fun updateContents() {
        viewArticleSubtitle.isVisible = false
        when (card!!.action) {
            TRANSLATE_DESCRIPTION -> showTranslateDescriptionUI()
            ADD_CAPTION -> showAddImageCaptionUI()
            TRANSLATE_CAPTION -> showTranslateImageCaptionUI()
            ADD_IMAGE_TAGS -> showImageTagsUI()
            else -> showAddDescriptionUI()
        }
    }

    private fun showImageTagsUI() {
        viewArticleImage.isVisible = true
        viewArticleExtract.isVisible = false
        divider.isVisible = false
        viewArticleImage.loadImage(ImageUrlUtil.getUrlForPreferredSize(card!!.page!!.imageInfo()!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE).toUri())
        viewArticleTitle.isVisible = false
        callToActionText.text = context.getString(R.string.suggested_edits_feed_card_add_image_tags)
    }

    private fun showAddDescriptionUI() {
        viewArticleTitle.isVisible = true
        viewArticleTitle.text = StringUtil.fromHtml(card!!.sourceSummaryForEdit!!.displayTitle!!)
        callToActionText.text = if (card!!.action == TRANSLATE_DESCRIPTION) context.getString(R.string.suggested_edits_feed_card_add_translation_in_language_button, app.language().getAppLanguageCanonicalName(card!!.targetSummaryForEdit!!.lang)) else context.getString(R.string.suggested_edits_feed_card_add_description_button)
        showImageOrExtract()
    }

    private fun showTranslateDescriptionUI() {
        viewArticleTitle.isVisible = true
        sourceDescription = card!!.sourceSummaryForEdit!!.description!!
        viewArticleSubtitle.isVisible = true
        viewArticleSubtitle.text = sourceDescription
        showAddDescriptionUI()
    }

    private fun showAddImageCaptionUI() {
        viewArticleTitle.isVisible = true
        viewArticleImage.isVisible = true
        viewArticleExtract.isVisible = false
        divider.isVisible = false
        viewArticleImage.loadImage(card!!.sourceSummaryForEdit!!.thumbnailUrl?.toUri())
        viewArticleTitle.text = StringUtil.removeNamespace(card!!.sourceSummaryForEdit!!.displayTitle!!)
        callToActionText.text = if (card!!.action == TRANSLATE_CAPTION) context.getString(R.string.suggested_edits_feed_card_translate_image_caption, app.language().getAppLanguageCanonicalName(card!!.targetSummaryForEdit!!.lang)) else context.getString(R.string.suggested_edits_feed_card_add_image_caption)
    }

    private fun showTranslateImageCaptionUI() {
        viewArticleTitle.isVisible = true
        sourceDescription = card!!.sourceSummaryForEdit!!.description!!
        viewArticleSubtitle.isVisible = true
        viewArticleSubtitle.text = sourceDescription
        showAddImageCaptionUI()
    }

    private fun showImageOrExtract() {
        if (card!!.sourceSummaryForEdit!!.thumbnailUrl.isNullOrBlank()) {
            viewArticleImage.isVisible = false
            viewArticleExtract.isVisible = true
            divider.isVisible = true
            viewArticleExtract.text = StringUtil.fromHtml(card!!.sourceSummaryForEdit!!.extractHtml)
            viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            viewArticleImage.isVisible = true
            viewArticleExtract.isVisible = false
            divider.isVisible = false
            viewArticleImage.loadImage(card!!.sourceSummaryForEdit!!.thumbnailUrl?.toUri())
        }
    }

    private fun header(card: SuggestedEditsCard) {
        headerView!!.setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_mode_edit_white_24dp)
                .setImageCircleColor(R.color.base30)
                .setLangCode(if (card.action == TRANSLATE_CAPTION || card.action == TRANSLATE_DESCRIPTION) card.targetSummaryForEdit!!.lang else "")
                .setCard(card)
                .setCallback(callback)
    }

    fun refreshCardContent() {
        SuggestedEditsFeedClient(card!!.action).fetchSuggestedEditForType(null, this)
    }

    override fun updateCardContent(card: SuggestedEditsCard) {
        setCard(card)
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 6
    }
}
