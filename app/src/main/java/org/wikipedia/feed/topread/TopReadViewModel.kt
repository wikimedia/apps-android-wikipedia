package org.wikipedia.feed.topread

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import org.wikipedia.feed.model.TopReadCard

class TopReadViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val card = savedStateHandle.get<TopReadCard>(TopReadArticlesActivity.TOP_READ_CARD)!!
}
