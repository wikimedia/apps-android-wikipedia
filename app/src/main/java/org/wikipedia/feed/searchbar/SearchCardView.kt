package org.wikipedia.feed.searchbar

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewSearchBarBinding
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.search.HybridSearchAbCTest
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

class SearchCardView(context: Context) : DefaultFeedCardView<SearchCard>(context) {
    interface Callback {
        fun onSearchRequested(view: View)
        fun onVoiceSearchRequested()
    }
    val binding = ViewSearchBarBinding.inflate(LayoutInflater.from(context), this, true)

    override var card: SearchCard? = null
        get() = super.card
        set(value) {
            field = value
            updateSearchHint()
        }

    init {
        binding.searchContainer.setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.background_color))
        FeedbackUtil.setButtonTooltip(binding.voiceSearchButton)

        binding.searchContainer.setOnClickListener { callback?.onSearchRequested(it) }
        binding.voiceSearchButton.setOnClickListener { callback?.onVoiceSearchRequested() }
        binding.voiceSearchButton.isVisible = WikipediaApp.instance.voiceRecognitionAvailable

        updateSearchHint()
    }

    private fun updateSearchHint() {
        if (Prefs.isHybridSearchOnboardingShown && HybridSearchAbCTest().isHybridSearchEnabled(WikipediaApp.instance.languageState.appLanguageCode)) {
            binding.searchIcon.contentDescription = context.getString(R.string.hybrid_search_search_hint)
            binding.searchTextView.text = context.getString(R.string.hybrid_search_search_hint)
        } else {
            binding.searchIcon.contentDescription = context.getString(R.string.search_hint)
            binding.searchTextView.text = context.getString(R.string.search_hint)
        }
    }
}
