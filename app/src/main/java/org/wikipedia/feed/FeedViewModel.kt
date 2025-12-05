package org.wikipedia.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.model.Card

class FeedViewModel : ViewModel() {
    private val _isEmpty = MutableStateFlow(false)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()

    private val coordinator = FeedCoordinator(viewModelScope, WikipediaApp.instance)

    init {
        FeedContentType.restoreState()
    }

    private val _reloadTrigger = MutableStateFlow(0)

    // Paging 3 integration: expose a Flow\<PagingData\<Card\>\> that reacts to `refresh()`.
    val pagingData: Flow<PagingData<Card>> = _reloadTrigger.flatMapLatest {
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { FeedPagingSource() }
        ).flow
    }.cachedIn(viewModelScope)

    // Keep existing coordinator helpers if needed; otherwise they can be removed
    fun dismissCard(card: Card): Int = coordinator.dismissCard(card)
    fun undoDismissCard(card: Card, position: Int) {
        coordinator.undoDismissCard(card, position)
    }
    fun updateHiddenCards() {
        coordinator.updateHiddenCards()
    }
    fun requestOfflineCard() {
        coordinator.requestOfflineCard()
    }
    fun removeOfflineCard() {
        coordinator.removeOfflineCard()
    }

    fun refresh() {
        _reloadTrigger.value += 1
    }

    fun loadMore() {
        refresh()
    }

    override fun onCleared() {
        super.onCleared()
        coordinator.setFeedUpdateListener(null)
        coordinator.reset()
    }
}
