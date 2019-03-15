package org.wikipedia.feed.suggestededits


import android.content.Context
import android.net.Uri
import android.view.View
import butterknife.OnClick
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_add_title_descriptions_item.view.*
import org.wikipedia.R
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ItemTouchHelperSwipeAdapter

class SuggestedEditCardView(context: Context) : DefaultFeedCardView<SuggestedEditCard>(context), ItemTouchHelperSwipeAdapter.SwipeableView {

    private val disposables = CompositeDisposable()

    init {
        View.inflate(getContext(), R.layout.fragment_add_title_descriptions_item, this)
    }

    override fun setCard(card: SuggestedEditCard) {
        super.setCard(card)
        setLayoutDirectionByWikiSite(card.wikiSite(), rootView!!)
        header(card)
    }


    override fun onDetachedFromWindow() {
        disposables.clear()
        super.onDetachedFromWindow()
    }

    @OnClick(R.id.cardView)
    internal fun onCardClick() {
        if (callback != null && card != null) {

        }
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
    }


    private fun header(card: SuggestedEditCard) {
        headerView.visibility = View.VISIBLE
        headerView!!.setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_mode_edit_white_24dp)
                .setImageCircleColor(ResourceUtil.getThemedAttributeId(context, R.attr.page_toolbar_icon_color))
                .setLangCode(card.wikiSite().languageCode())
                .setCard(card)
                .setCallback(callback)
    }

    private fun image(uri: Uri?) {

    }

}
