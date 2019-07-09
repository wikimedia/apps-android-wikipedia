package org.wikipedia.feed.suggestededits


import android.content.Context
import android.net.Uri
import android.view.View
import io.reactivex.annotations.NonNull
import kotlinx.android.synthetic.main.view_suggested_edit_card.view.*
import org.wikipedia.Constants.InvokeSource.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ItemTouchHelperSwipeAdapter

class SuggestedEditsCardView(context: Context) : DefaultFeedCardView<SuggestedEditsCard>(context), ItemTouchHelperSwipeAdapter.SwipeableView, SuggestedEditsFeedClient.Callback {
    interface Callback {
        fun onSuggestedEditsCardClick(view: SuggestedEditsCardView)
    }

    private var sourceDescription: String = ""
    private val app = WikipediaApp.getInstance()
    private var card: SuggestedEditsCard? = null

    init {
        inflate(getContext(), R.layout.view_suggested_edit_card, this)
    }

    fun isTranslation(): Boolean {
        return card!!.invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC || card!!.invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION
    }

    override fun setCard(@NonNull card: SuggestedEditsCard) {
        super.setCard(card)
        this.card = card

        setLayoutDirectionByWikiSite(WikiSite.forLanguageCode(card.sourceSummary!!.lang), this)

        cardView.setOnClickListener {
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
        viewArticleSubtitle.visibility = View.GONE
        when (card!!.invokeSource) {
            FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC -> showTranslateDescriptionUI()
            FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION -> showAddImageCaptionUI()
            FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION -> showTranslateImageCaptionUI()
            else -> showAddDescriptionUI()
        }
    }

    private fun showAddDescriptionUI() {
        viewArticleTitle.text = StringUtil.fromHtml(card!!.sourceSummary!!.displayTitle!!)
        callToActionText.text = if (card!!.invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC) context.getString(R.string.suggested_edits_feed_card_add_translation_in_language_button, app.language().getAppLanguageCanonicalName(card!!.targetSummary!!.lang)) else context.getString(R.string.suggested_edits_add_description_button)
        showImageOrExtract()
    }

    private fun showTranslateDescriptionUI() {
        sourceDescription = card!!.sourceSummary!!.description!!.capitalize()
        viewArticleSubtitle.visibility = View.VISIBLE
        viewArticleSubtitle.text = sourceDescription
        showAddDescriptionUI()
    }

    private fun showAddImageCaptionUI() {
        viewArticleImage.visibility = View.VISIBLE
        viewArticleExtract.visibility = View.GONE
        divider.visibility = View.GONE
        viewArticleImage.loadImage(Uri.parse(card!!.sourceSummary!!.thumbnailUrl))
        viewArticleTitle.text = StringUtil.removeNamespace(card!!.sourceSummary!!.displayTitle!!)
        callToActionText.text = if (card!!.invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION) context.getString(R.string.suggested_edits_feed_card_translate_image_caption, app.language().getAppLanguageCanonicalName(card!!.targetSummary!!.lang)) else context.getString(R.string.suggested_edits_feed_card_add_image_caption)
    }

    private fun showTranslateImageCaptionUI() {
        sourceDescription = card!!.sourceSummary!!.description!!.capitalize()
        viewArticleSubtitle.visibility = View.VISIBLE
        viewArticleSubtitle.text = sourceDescription
        showAddImageCaptionUI()
    }

    private fun showImageOrExtract() {
        if (card!!.sourceSummary!!.thumbnailUrl.isNullOrBlank()) {
            viewArticleImage.visibility = View.GONE
            viewArticleExtract.visibility = View.VISIBLE
            divider.visibility = View.VISIBLE
            viewArticleExtract.text = StringUtil.fromHtml(card!!.sourceSummary!!.extractHtml)
            viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            viewArticleImage.visibility = View.VISIBLE
            viewArticleExtract.visibility = View.GONE
            divider.visibility = View.GONE
            viewArticleImage.loadImage(Uri.parse(card!!.sourceSummary!!.thumbnailUrl))
        }
    }

    private fun header(card: SuggestedEditsCard) {
        headerView!!.setTitle(card.title())
                .setImage(R.drawable.ic_mode_edit_white_24dp)
                .setImageCircleColor(R.color.base30)
                .setLangCode(if (card.invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION || card.invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC) card.wikiSite().languageCode() else "")
                .setCard(card)
                .setCallback(callback)
    }

    fun refreshCardContent() {
        SuggestedEditsFeedClient(card!!.invokeSource).fetchSuggestedEditForType(null, this)
    }

    override fun updateCardContent(card: SuggestedEditsCard) {
        setCard(card)
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 6
    }
}
