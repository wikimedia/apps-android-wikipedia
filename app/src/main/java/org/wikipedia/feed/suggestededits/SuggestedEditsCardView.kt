package org.wikipedia.feed.suggestededits

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.wikipedia.databinding.ViewSuggestedEditsCardBinding
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.views.PositionAwareFragmentStateAdapter

class SuggestedEditsCardView(context: Context) : DefaultFeedCardView<SuggestedEditsCard>(context),
        SuggestedEditsFeedClient.Callback, CardFooterView.Callback {
    interface Callback {
        fun onSeCardFooterClicked()
    }

    private val binding = ViewSuggestedEditsCardBinding.inflate(LayoutInflater.from(context), this, true)
    private var card: SuggestedEditsCard? = null

    override fun setCard(card: SuggestedEditsCard) {
        if (card == getCard()) {
            return
        }
        super.setCard(card)
        this.card = card
        header(card)
        updateContents()
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
        binding.headerView.setCallback(callback)
    }

    private fun updateContents() {
        setUpPagerWithSECards()
        binding.cardFooter.setFooterActionText(card!!.footerActionText(), null)
        binding.cardFooter.callback = this
    }

    private fun setUpPagerWithSECards() {
        binding.seCardsPager.adapter = SECardsPagerAdapter(context as AppCompatActivity, card)
        binding.seCardsPager.offscreenPageLimit = 3
        TabLayoutMediator(binding.seCardsIndicatorLayout, binding.seCardsPager) { _: TabLayout.Tab, _: Int -> }.attach()
    }

    class SECardsPagerAdapter(activity: AppCompatActivity?, card: SuggestedEditsCard?) : PositionAwareFragmentStateAdapter(activity!!) {
        private val seCardTypeList = ArrayList<DescriptionEditActivity.Action>()
        private var card: SuggestedEditsCard? = null

        init {
            this.card = card
            seCardTypeList.add(ADD_DESCRIPTION)
            seCardTypeList.add(ADD_CAPTION)
            seCardTypeList.add(ADD_IMAGE_TAGS)
        }

        override fun getItemCount(): Int {
            return seCardTypeList.size
        }

        override fun createFragment(position: Int): Fragment {
            return SuggestedEditsCardItemFragment.newInstance(card!!.age, seCardTypeList[position])
        }
    }

    private fun header(card: SuggestedEditsCard) {
        binding.headerView.setTitle(card.title())
                .setLangCode("")
                .setCard(card)
                .setCallback(getCallback())
    }

    override fun updateCardContent(card: SuggestedEditsCard) {
        setCard(card)
    }

    override fun onFooterClicked() {
        getCallback()?.onSeCardFooterClicked()
    }
}
