package org.wikipedia.feed.suggestededits


import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_add_title_descriptions_item.view.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.editactionfeed.provider.MissingDescriptionProvider
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ItemTouchHelperSwipeAdapter

class SuggestedEditCardView(context: Context) : DefaultFeedCardView<SuggestedEditCard>(context), ItemTouchHelperSwipeAdapter.SwipeableView {
    interface Callback {
        fun onSuggestedEditsCardClick(@NonNull pageTitle: PageTitle, @NonNull view: SuggestedEditCardView)
    }

    private val disposables = CompositeDisposable()
    val CARD_BOTTOM_PADDING = 16.0f
    var translation: Boolean = false
    private var sourceDescription: String = ""
    private val app = WikipediaApp.getInstance()


    init {
        View.inflate(getContext(), R.layout.fragment_add_title_descriptions_item, this)
    }

    override fun setCard(card: SuggestedEditCard) {
        super.setCard(card)
        translation = card.isTranslation()
        setLayoutDirectionByWikiSite(card.wikiSite(), rootView!!)
        hideViews()
        getArticleWithMissingDescription()
        header(card)
    }

    private var summary: RbPageSummary? = null

    private fun getArticleWithMissingDescription() {
        if (translation) {
            disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(app.language().appLanguageCodes.get(0)), app.language().appLanguageCodes.get(1), true)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pair ->
                        sourceDescription = StringUtils.defaultString(pair.first)
                        summary = pair.second
                        updateSourceDescriptionWithHighlight()
                    }, { this.setErrorState(it) })!!)

        } else {
            disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(app.language().appLanguageCode))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pageSummary ->
                        summary = pageSummary
                        updateContents()
                    }, { this.setErrorState(it) }))
        }

    }

    private fun updateSourceDescriptionWithHighlight() {
        viewArticleSubtitleContainer.visibility = View.VISIBLE
        viewArticleSubtitleAddedBy.visibility = View.GONE
        viewArticleSubtitleEdit.visibility = View.GONE
        viewArticleSubtitle.text = sourceDescription
        updateContents()

    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
         cardItemErrorView.setError(t)
         cardItemErrorView.visibility = View.VISIBLE
         cardItemProgressBar.visibility = View.GONE
         cardItemContainer.visibility = View.GONE
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
        headerView.setCallback(callback)
    }

    private fun updateContents() {
        viewAddDescriptionButton.visibility = View.VISIBLE

        viewArticleTitle.text = summary!!.normalizedTitle
        if(summary!!.thumbnailUrl!=null)
        viewArticleImage.loadImage(Uri.parse(summary!!.thumbnailUrl))
        callToActionText.text = if (translation) String.format(context.getString(R.string.add_translation), app.language().getAppLanguageCanonicalName(app.language().appLanguageCodes.get(1))) else context.getString(R.string.editactionfeed_add_description_button)

    }

    private fun hideViews() {
        viewArticleExtract.text = ""
        viewArticleTitle.text = ""
        callToActionText.text=""
        viewArticleImage.loadImage(null)
        headerView.visibility = View.GONE
        viewAddDescriptionButton.visibility = View.GONE
        viewArticleSubtitleContainer.visibility = View.GONE
        viewArticleExtract.visibility = View.GONE
        divider.visibility = View.GONE
        suggestedEditsRootView.setPadding(0, 0, 0, 0)
        val param = cardView.layoutParams as FrameLayout.LayoutParams
        param.setMargins(0, 0, 0, 0)
        cardView.useCompatPadding = false
        cardView.setContentPadding(0, 0, 0, DimenUtil.roundedDpToPx(CARD_BOTTOM_PADDING))
        cardView.setOnClickListener {
            if (callback != null && card != null) {
                callback!!.onSuggestedEditsCardClick(summary!!.getPageTitle(WikiSite.forLanguageCode(if (translation) app.language().appLanguageCodes.get(1) else app.language().appLanguageCode)), this)
            }
        }
    }


    override fun onDetachedFromWindow() {
        disposables.clear()
        super.onDetachedFromWindow()
    }


    private fun header(card: SuggestedEditCard) {
        headerView.visibility = View.VISIBLE
        headerView!!.setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_mode_edit_white_24dp)
                .setImageCircleColor(ResourceUtil.getThemedAttributeId(context, R.attr.material_theme_de_emphasised_color))
                .setLangCode(if(translation)card.wikiSite().languageCode() else "")
                .setCard(card)
                .setCallback(callback)
    }

    fun showAddedDescriptionView(addedDescription: String?) {
        if (!TextUtils.isEmpty(addedDescription)) {
            viewArticleSubtitleContainer.visibility = View.VISIBLE
            viewAddDescriptionButton.visibility = View.GONE
            viewArticleSubtitle.text = addedDescription
        }
    }
}
