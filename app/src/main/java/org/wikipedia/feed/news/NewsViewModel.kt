package org.wikipedia.feed.news

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import org.wikipedia.Constants
import org.wikipedia.dataclient.WikiSite

class NewsViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val item = savedStateHandle.get<NewsItem>(NewsActivity.EXTRA_NEWS_ITEM)!!
    val wiki = savedStateHandle.get<WikiSite>(Constants.ARG_WIKISITE)!!
}
