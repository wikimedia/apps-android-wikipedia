package org.wikipedia.feed.suggestededits

import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.view_suggested_edit_card.view.headerView
import kotlinx.android.synthetic.main.view_suggested_edits_cards.view.*
import org.wikipedia.R
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.views.PositionAwareFragmentStateAdapter

class SuggestedEditsCardView(context: Context) : DefaultFeedCardView<SuggestedEditsCard>(context), SuggestedEditsFeedClient.Callback {
    interface Callback {
        fun onSuggestedEditsCardClick(view: SuggestedEditsCardView)
    }

    private var card: SuggestedEditsCard? = null
    private var view: View

    init {
        view = inflate(getContext(), R.layout.view_suggested_edits_cards, this)
    }

    override fun setCard(card: SuggestedEditsCard) {
        super.setCard(card)
        this.card = card
        header(card)
        updateContents()
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
        headerView.setCallback(callback)
    }

    private fun updateContents() {
        setUpPagerWithSECards()
    }

    private fun setUpPagerWithSECards() {
        seCardsPager.adapter = SECardsPagerAdapter(view.context as AppCompatActivity)
        TabLayoutMediator(seCardsIndicatorLayout, seCardsPager)
        { tab: TabLayout.Tab, position: Int -> tab.view.isClickable = false }.attach()
    }

    class SECardsPagerAdapter(activity: AppCompatActivity?) : PositionAwareFragmentStateAdapter(activity!!) {
        private val seCardTypeList = ArrayList<DescriptionEditActivity.Action>()

        init {
            seCardTypeList.add(ADD_DESCRIPTION)
            seCardTypeList.add(ADD_CAPTION)
            seCardTypeList.add(ADD_IMAGE_TAGS)
        }

        override fun getItemCount(): Int {
            return seCardTypeList.size
        }

        override fun createFragment(position: Int): Fragment {
            return SuggestedEditsCardItemFragment.newInstance("", "")
        }
    }

    private fun header(card: SuggestedEditsCard) {
        headerView!!.setTitle(card.title())
                .setLangCode("")
                .setCard(card)
                .setCallback(callback)
    }

    override fun updateCardContent(card: SuggestedEditsCard) {
        setCard(card)
    }
}
