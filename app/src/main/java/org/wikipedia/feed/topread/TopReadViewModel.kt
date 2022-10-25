package org.wikipedia.feed.topread

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TopReadViewModel(bundle: Bundle) : ViewModel() {

    val card = bundle.getParcelable<TopReadListCard>(TopReadArticlesActivity.TOP_READ_CARD)!!

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TopReadViewModel(bundle) as T
        }
    }
}
