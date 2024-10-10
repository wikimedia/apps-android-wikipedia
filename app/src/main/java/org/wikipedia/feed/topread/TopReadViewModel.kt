package org.wikipedia.feed.topread

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class TopReadViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val card = savedStateHandle.get<TopReadListCard>(TopReadArticlesActivity.TOP_READ_CARD)!!
}
