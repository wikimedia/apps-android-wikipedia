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
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import org.wikipedia.util.UiState
import java.time.LocalDate

class GamesHubViewModel() : ViewModel() {
    private val _onThisDayGameUiState = MutableStateFlow<UiState<Map<String, List<OnThisDayCardGameState>>>>(UiState.Loading)
    val onThisDayGameUiState: StateFlow<UiState<Map<String, List<OnThisDayCardGameState>>>> = _onThisDayGameUiState.asStateFlow()

    init {
        loadOnThisDayGamesPreviews()
    }

    fun loadOnThisDayGamesPreviews() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _onThisDayGameUiState.value = UiState.Error(throwable)
        }) {
            _onThisDayGameUiState.value = UiState.Loading
            // Get available languages for on this day game, and then get the today and last 3 days games by using the provider.
            val gamesMap = WikipediaApp.instance.languageState.appLanguageCodes
                .filter { OnThisDayGameViewModel.isLangSupported(it) }
                .map { lang ->
                    async {
                        val wikiSite = WikiSite.forLanguageCode(lang)
                        val games = (0..3).map { i ->
                            OnThisDayGameProvider.getGameState(wikiSite, LocalDate.now().minusDays(i.toLong()))
                        }
                        lang to games
                    }
                }
                .awaitAll()
                .toMap()
            _onThisDayGameUiState.value = UiState.Success(gamesMap)
        }
    }
}
