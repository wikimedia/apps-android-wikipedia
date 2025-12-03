package org.wikipedia.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.model.Card

class FeedViewModel : ViewModel() {

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isEmpty = MutableStateFlow(false)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()

    private val coordinator = FeedCoordinator(viewModelScope, WikipediaApp.instance)

    init {
        FeedContentType.restoreState()
        setupCoordinator()
    }

    private fun setupCoordinator() {
        coordinator.setFeedUpdateListener(object : FeedCoordinatorBase.FeedUpdateListener {
            override fun insert(card: Card, pos: Int) {
                _cards.value = coordinator.cards.toList()
                _isLoading.value = false
                updateEmptyState()
            }

            override fun remove(card: Card, pos: Int) {
                _cards.value = coordinator.cards.toList()
                _isLoading.value = false
                updateEmptyState()
            }

            override fun finished(shouldUpdatePreviousCard: Boolean) {
                _cards.value = coordinator.cards.toList()
                _isLoading.value = false
                updateEmptyState()
            }
        })
    }

    private fun updateEmptyState() {
        _isEmpty.value = coordinator.cards.size < 2
    }

    fun loadInitialFeed() {
        _isLoading.value = true
        coordinator.more(WikipediaApp.instance.wikiSite)
    }

    fun loadMore() {
        _isLoading.value = true
        coordinator.incrementAge()
        coordinator.more(WikipediaApp.instance.wikiSite)
    }

    fun refresh() {
        _isLoading.value = true
        _isEmpty.value = false
        coordinator.reset()
        _cards.value = emptyList()
        WikipediaApp.instance.resetWikiSite()
        coordinator.more(WikipediaApp.instance.wikiSite)
    }

    fun dismissCard(card: Card): Int {
        return coordinator.dismissCard(card)
    }

    fun undoDismissCard(card: Card, position: Int) {
        coordinator.undoDismissCard(card, position)
        _cards.value = coordinator.cards.toList()
    }

    fun updateHiddenCards() {
        coordinator.updateHiddenCards()
        _cards.value = coordinator.cards.toList()
    }

    fun requestOfflineCard() {
        coordinator.requestOfflineCard()
        _cards.value = coordinator.cards.toList()
    }

    fun removeOfflineCard() {
        coordinator.removeOfflineCard()
        _cards.value = coordinator.cards.toList()
    }

    fun getCoordinator(): FeedCoordinator = coordinator

    override fun onCleared() {
        super.onCleared()
        coordinator.setFeedUpdateListener(null)
        coordinator.reset()
    }
}
