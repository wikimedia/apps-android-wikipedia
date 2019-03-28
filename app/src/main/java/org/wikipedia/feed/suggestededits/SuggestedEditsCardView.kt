package org.wikipedia.feed.suggestededits


import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.widget.FrameLayout
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_add_title_descriptions_item.view.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ItemTouchHelperSwipeAdapter

class SuggestedEditsCardView(context: Context) : DefaultFeedCardView<SuggestedEditsCard>(context), ItemTouchHelperSwipeAdapter.SwipeableView {
    interface Callback {
        fun onSuggestedEditsCardClick(@NonNull pageTitle: PageTitle, @NonNull sourceDescription: String, @NonNull sourceLangCode: String, @NonNull view: SuggestedEditsCardView)
    }

    private val disposables = CompositeDisposable()
    val CARD_BOTTOM_PADDING = 16.0f
    val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 6
    var translation: Boolean = false
    private var sourceDescription: String = ""
    private val app = WikipediaApp.getInstance()
    private var summary: RbPageSummary? = null
    private var sourceLangCode: String = app.language().appLanguageCode
    private var targetLangCode: String = app.language().appLanguageCodes.get(1)


    init {
        View.inflate(getContext(), R.layout.fragment_add_title_descriptions_item, this)
    }

    override fun setCard(@NonNull card: SuggestedEditsCard) {
        super.setCard(card)
        prepareViews()
        translation = card.isTranslation
        summary = card.summary
        sourceDescription = card.sourceDescription
        setLayoutDirectionByWikiSite(WikiSite.forLanguageCode(sourceLangCode), rootView!!)
        header(card)
        updateSourceDescriptionWithHighlight()
    }

    private fun updateSourceDescriptionWithHighlight() {
        if (translation) {
            viewArticleSubtitleContainer.visibility = View.VISIBLE
            viewArticleSubtitleAddedBy.visibility = View.GONE
            viewArticleSubtitleEdit.visibility = View.GONE
            viewArticleSubtitle.text = sourceDescription
        }
        updateContents()
    }

    fun isTranslation(): Boolean {
        return translation
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
        headerView.setCallback(callback)
    }

    private fun updateContents() {
        viewAddDescriptionButton.visibility = View.VISIBLE
        viewArticleTitle.text = summary!!.normalizedTitle
        callToActionText.text = if (translation) String.format(context.getString(R.string.add_translation), app.language().getAppLanguageCanonicalName(targetLangCode)) else context.getString(R.string.editactionfeed_add_description_button)
        showImageOrExtract()
    }

    private fun showImageOrExtract() {
        if (TextUtils.isEmpty(summary!!.thumbnailUrl)) {
            viewArticleImage.visibility = View.GONE
            viewArticleExtract.visibility = View.VISIBLE
            divider.visibility = View.VISIBLE
            viewArticleExtract.text = StringUtil.fromHtml(summary!!.extractHtml)
            viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            viewArticleImage.visibility = View.VISIBLE
            viewArticleExtract.visibility = View.GONE
            divider.visibility = View.GONE
            viewArticleImage.loadImage(Uri.parse(summary!!.thumbnailUrl))
        }
    }

    private fun prepareViews() {
        viewArticleExtract.text = ""
        viewArticleTitle.text = ""
        callToActionText.text = ""
        viewArticleImage.loadImage(null)
        headerView.visibility = View.GONE
        viewAddDescriptionButton.visibility = View.GONE
        cardItemProgressBar.visibility = View.GONE
        viewArticleSubtitleContainer.visibility = View.GONE
        viewArticleExtract.visibility = View.GONE
        divider.visibility = View.GONE
        suggestedEditsItemRootView.setPadding(0, 0, 0, 0)
        val param = cardView.layoutParams as FrameLayout.LayoutParams
        param.setMargins(0, 0, 0, 0)
        cardView.useCompatPadding = false
        cardView.setContentPadding(0, 0, 0, DimenUtil.roundedDpToPx(CARD_BOTTOM_PADDING))
        cardView.setOnClickListener {
            if (callback != null && card != null) {
                callback!!.onSuggestedEditsCardClick(summary!!.getPageTitle(WikiSite.forLanguageCode(if (translation) targetLangCode else sourceLangCode)), sourceDescription, sourceLangCode, this)
            }
        }
    }


    override fun onDetachedFromWindow() {
        disposables.clear()
        super.onDetachedFromWindow()
    }


    private fun header(card: SuggestedEditsCard) {
        headerView.visibility = View.VISIBLE
        headerView!!.setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_mode_edit_white_24dp)
                .setImageCircleColor(ResourceUtil.getThemedAttributeId(context, R.attr.material_theme_de_emphasised_color))
                .setLangCode(if (translation) card.wikiSite().languageCode() else "")
                .setCard(card)
                .setCallback(callback)
    }

    fun showAddedDescriptionView(addedDescription: String?) {
        if (!TextUtils.isEmpty(addedDescription)) {
            viewArticleSubtitleContainer.visibility = View.VISIBLE
            viewAddDescriptionButton.visibility = View.GONE
            viewArticleSubtitleAddedBy.visibility = View.VISIBLE
            viewArticleSubtitleEdit.visibility = View.VISIBLE
            viewArticleSubtitle.text = addedDescription
            if (translation) viewArticleSubtitleAddedBy.text = context.getString(R.string.editactionfeed_translated_by_you)
            else viewArticleSubtitleAddedBy.text = context.getString(R.string.editactionfeed_added_by_you)
        }
    }

}
