package org.wikipedia.activitytab

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.categories.db.Category
import org.wikipedia.database.AppDatabase
import org.wikipedia.donate.DonationResult
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import org.wikipedia.settings.Prefs
import org.wikipedia.util.UiState
import java.time.LocalDate

class ActivityTabViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    var gameStatistics: OnThisDayGameViewModel.GameStatistics? = null
    var donationResults: List<DonationResult> = emptyList()

    var topCategories: List<Category> = emptyList()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = UiState.Error(throwable)
        }) {
            _uiState.value = UiState.Loading

            val currentDate = LocalDate.now()
            val languageCode = WikipediaApp.instance.wikiSite.languageCode

            // TODO: do something with game statistics
            gameStatistics = OnThisDayGameViewModel.getGameStatistics(languageCode)

            // TODO: do something with donation results
            donationResults = Prefs.donationResults

            // TODO: do something with top categories
            topCategories = AppDatabase.instance.categoryDao().getTopCategoriesByMonth(currentDate.year, currentDate.monthValue)

            _uiState.value = UiState.Success(Unit)
        }
    }
}
