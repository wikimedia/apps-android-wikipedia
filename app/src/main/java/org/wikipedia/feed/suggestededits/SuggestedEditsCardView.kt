package org.wikipedia.feed.suggestededits

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewSuggestedEditsCardBinding
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.util.L10nUtil
import org.wikipedia.views.PositionAwareFragmentStateAdapter

class SuggestedEditsCardView(context: Context) : DefaultFeedCardView<SuggestedEditsCard>(context), CardFooterView.Callback {
    interface Callback {
        fun onSeCardFooterClicked()
    }

    private val binding = ViewSuggestedEditsCardBinding.inflate(LayoutInflater.from(context), this, true)

    override var card: SuggestedEditsCard? = null
        set(value) {
            field = value
            value?.let {
                header(it)
                updateContents(it)
            }
        }

    override var callback: FeedAdapter.Callback? = null
        set(value) {
            field = value
            binding.headerView.setCallback(value)
        }

    override fun onFooterClicked() {
        callback?.onSeCardFooterClicked()
    }

    private fun updateContents(card: SuggestedEditsCard) {
        setUpPagerWithSECards(card)
        binding.cardFooter.setFooterActionText(card.footerActionText(), null)
        binding.cardFooter.callback = this
    }

    private fun setUpPagerWithSECards(card: SuggestedEditsCard) {
        binding.seCardsPager.adapter = SECardsPagerAdapter(context as AppCompatActivity, card)
        binding.seCardsPager.offscreenPageLimit = 3
        L10nUtil.setConditionalLayoutDirection(binding.seCardsPager, WikipediaApp.instance.wikiSite.languageCode)
        L10nUtil.setConditionalLayoutDirection(binding.seCardsIndicatorLayout, WikipediaApp.instance.wikiSite.languageCode)
        TabLayoutMediator(binding.seCardsIndicatorLayout, binding.seCardsPager) { _, _ -> }.attach()
    }

    private fun header(card: SuggestedEditsCard) {
        binding.headerView.setTitle(card.title())
            .setCard(card)
            .setLangCode(null)
            .setCallback(callback)
    }

    class SECardsPagerAdapter(activity: AppCompatActivity, private val card: SuggestedEditsCard) : PositionAwareFragmentStateAdapter(activity) {
        override fun getItemCount(): Int {
            return 3 // description, caption, image tags
        }

        override fun createFragment(position: Int): Fragment {
            return SuggestedEditsCardItemFragment.newInstance(card.age, card.cardTypes[position])
        }
    }
}
