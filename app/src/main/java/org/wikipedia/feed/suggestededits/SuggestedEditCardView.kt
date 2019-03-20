package org.wikipedia.feed.suggestededits


import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.widget.FrameLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_add_title_descriptions_item.view.*
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


    init {
        View.inflate(getContext(), R.layout.fragment_add_title_descriptions_item, this)
    }

    override fun setCard(card: SuggestedEditCard) {
        super.setCard(card)
        setLayoutDirectionByWikiSite(card.wikiSite(), rootView!!)
        hideViews()
        getArticleWithMissingDescription()
        header(card)
    }

    private var summary: RbPageSummary? = null

    private fun getArticleWithMissingDescription() {
        disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(WikipediaApp.getInstance().language().appLanguageCode))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pageSummary ->
                    summary = pageSummary
                    updateContents()
                }, { this.setErrorState(it) }))

    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        /* cardItemErrorView.setError(t)
         cardItemErrorView.visibility = View.VISIBLE
         cardItemProgressBar.visibility = View.GONE
         cardItemContainer.visibility = View.GONE*/
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
        headerView.setCallback(callback)
    }

    private fun updateContents() {
        viewArticleTitle.text = summary!!.normalizedTitle
        viewArticleImage.loadImage(Uri.parse(summary!!.thumbnailUrl))
    }

    private fun hideViews() {
        viewArticleExtract.visibility = View.GONE
        divider.visibility = View.GONE
        suggestedEditsRootView.setPadding(0, 0, 0, 0)
        val param = cardView.layoutParams as FrameLayout.LayoutParams
        param.setMargins(0, 0, 0, 0)
        cardView.useCompatPadding = false
        cardView.setContentPadding(0, 0, 0, DimenUtil.roundedDpToPx(CARD_BOTTOM_PADDING))
        cardView.setOnClickListener {
            if (callback != null && card != null) {
                callback!!.onSuggestedEditsCardClick(summary!!.getPageTitle(WikiSite.forLanguageCode(WikipediaApp.getInstance().language().appLanguageCode)), this)
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
                .setLangCode(card.wikiSite().languageCode())
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
