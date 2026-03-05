package org.wikipedia.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.wikigames.OnThisDayCardGameState
import org.wikipedia.games.onthisday.OnThisDayGameProvider
import org.wikipedia.util.UiState
import java.time.LocalDate

class GamesHubViewModel : ViewModel() {
    private val _onThisDayGameUiState = MutableStateFlow<UiState<List<OnThisDayCardGameState>>>(UiState.Loading)
    val onThisDayGameUiState: StateFlow<UiState<List<OnThisDayCardGameState>>> = _onThisDayGameUiState.asStateFlow()

    val appLanguageCodes = MutableStateFlow(WikipediaApp.instance.languageState.appLanguageCodes.toList())

    fun loadOnThisDayGamesPreviews(langCode: String) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _onThisDayGameUiState.value = UiState.Error(throwable)
        }) {
            _onThisDayGameUiState.value = UiState.Loading
            if (!WikiGames.WHICH_CAME_FIRST.isLangSupported(langCode)) {
                _onThisDayGameUiState.value = UiState.Success(emptyList())
                return@launch
            }
            // Get available languages for on this day game, and then get the today and last 3 days games by using the provider.
            val gamesList = (0..3).map { i ->
                async {
                    val wikiSite = WikiSite.forLanguageCode(langCode)
                    OnThisDayGameProvider.getGameState(wikiSite, LocalDate.now().minusDays(i.toLong()), i != 0)
                }
            }.awaitAll()
            _onThisDayGameUiState.value = UiState.Success(gamesList)
        }
    }

    fun refreshLanguageCodes() {
        appLanguageCodes.value = WikipediaApp.instance.languageState.appLanguageCodes.toList()
    }
}
