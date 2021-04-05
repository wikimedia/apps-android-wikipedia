package org.wikipedia.feed.searchbar

import android.content.Context
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import org.wikipedia.R
import org.wikipedia.databinding.ViewSearchBarBinding
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

class SearchCardView(context: Context) : DefaultFeedCardView<SearchCard>(context) {
    interface Callback {
        fun onSearchRequested(view: View)
        fun onVoiceSearchRequested()
    }

    init {
        val binding = ViewSearchBarBinding.inflate(LayoutInflater.from(context), this, true)
        binding.searchContainer.setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.color_group_22))
        FeedbackUtil.setButtonLongPressToast(binding.voiceSearchButton)

        binding.searchContainer.setOnClickListener { callback?.onSearchRequested(it) }
        binding.voiceSearchButton.setOnClickListener { callback?.onVoiceSearchRequested() }
        binding.voiceSearchButton.visibility = if (SpeechRecognizer.isRecognitionAvailable(context)) VISIBLE else GONE
    }
}
