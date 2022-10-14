package org.wikipedia.feed.news

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.wikipedia.dataclient.WikiSite

class NewsViewModel(bundle: Bundle) : ViewModel() {

    val item = bundle.getParcelable<NewsItem>(NewsActivity.EXTRA_NEWS_ITEM)!!
    val wiki = bundle.getParcelable<WikiSite>(NewsActivity.EXTRA_WIKI)!!

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewsViewModel(bundle) as T
        }
    }
}
