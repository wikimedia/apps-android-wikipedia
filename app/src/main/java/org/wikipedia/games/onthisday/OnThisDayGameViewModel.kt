package org.wikipedia.games.onthisday

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.wikipedia.Constants
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.games.PlayTypes
import org.wikipedia.games.WikiGames
import org.wikipedia.games.db.DailyGameHistory
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.log.L
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class OnThisDayGameViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _gameState = MutableLiveData<Resource<GameState>>()
    val gameState: LiveData<Resource<GameState>> get() = _gameState

    val invokeSource = savedStateHandle.get<Constants.InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE)!!
    val wikiSite = savedStateHandle.get<WikiSite>(Constants.ARG_WIKISITE)!!

    // TODO: initialize the state earlier in the loading process, so that the state is nonnull
    // when the ViewModel is created, instead of only after the first loadGameState() call.
    private lateinit var currentState: GameState

    private val overrideDate = savedStateHandle.contains(EXTRA_DATE)
    var currentDate = if (overrideDate) LocalDate.ofInstant(Instant.ofEpochSecond(savedStateHandle.get<Long>(EXTRA_DATE)!!), ZoneOffset.UTC) else LocalDate.now()
    val currentMonth get() = currentDate.monthValue
    val currentDay get() = currentDate.dayOfMonth
    var isArchiveGame = false

    private val events = mutableListOf<OnThisDay.Event>()
    val savedPages = mutableListOf<PageSummary>()

    init {
        loadGameState()
    }

    fun loadGameState(useDateFromState: Boolean = true) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            _gameState.postValue(Resource.Error(throwable))
        }) {
            _gameState.postValue(Resource.Loading())

            // Migrate from Prefs.otdGameHistory to use database
            // TODO: remove this in May, 2026
            migrateGameHistoryFromPrefsToDatabase()

            val totalState = JsonUtil.decodeFromString<TotalGameState>(Prefs.otdGameState) ?: TotalGameState()
            if (useDateFromState && !overrideDate) {
                val languageState = totalState.langToState[wikiSite.languageCode]
                val lastActiveDate = try {
                    LocalDate.parse(languageState?.lastActiveDate.orEmpty(), DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (e: Exception) {
                    LocalDate.now()
                }
                if (LocalDate.now().isAfter(lastActiveDate)) {
                    // Reset to today's game, if we're coming back from an archived game, or if coming back on a future day.
                    persistState(removeCurrentState = true)
                    currentDate = LocalDate.now()
                } else {
                    languageState?.let { langState ->
                        currentDate = when {
                            // if today's or archive game is completed reset to today's date
                            langState.currentQuestionIndex >= langState.totalQuestions -> LocalDate.now()
                            langState.gamePlayDate.isNotEmpty() -> LocalDate.parse(langState.gamePlayDate, DateTimeFormatter.ISO_LOCAL_DATE)
                            else -> LocalDate.now()
                        }
                    }
                }
            }

            isArchiveGame = currentDate.isBefore(LocalDate.now())

            // Check again if this game exists in the database, i.e. if this day has already been finished.
            val dbGameResults = AppDatabase.instance.dailyGameHistoryDao().findGameHistoryByDate(
                gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                language = wikiSite.languageCode,
                year = currentDate.year,
                month = currentMonth,
                day = currentDay
            )

            val eventsFromApi = ServiceFactory.getRest(wikiSite).getOnThisDay(currentMonth, currentDay).events

            // Here is the logic for arranging the events:
            // First we filter out any events that actually mention a year in the text, since those might give away the answer.
            val yearRegex = Regex(".*\\b\\d{1,4}\\b.*")
            val allEvents = eventsFromApi.filter {
                it.year > 0 && it.year <= currentDate.year && !it.text.matches(yearRegex)
            }.distinctBy { it.year }.toMutableList()

            // Shuffle the events, but seed the random number generator with the current month and day so that the order is consistent for the same day.
            allEvents.shuffle(Random(currentMonth * 100 + currentDay))

            events.clear()
            // Take an event from the list, and find another event that is within a certain range
            for (i in 0 until Prefs.otdGameQuestionsPerDay) {
                val event1 = allEvents.removeAt(0)
                var event2: OnThisDay.Event? = null
                var yearSpread = max((390 - (0.19043 * event1.year)).toInt(), 5)
                event2 = allEvents.find { abs(event1.year - it.year) <= yearSpread }
                if (event2 == null) {
                    var minDiff = Int.MAX_VALUE
                    for (event in allEvents) {
                        val diff = abs(event1.year - event.year)
                        if (diff < minDiff) {
                            minDiff = diff
                            event2 = event
                        }
                    }
                }
                event2?.let {
                    events.add(event1)
                    events.add(event2)
                    allEvents.remove(event2)
                }
            }

            totalState.langToState[wikiSite.languageCode]?.let {
                currentState = it
            } ?: run {
                currentState = GameState(currentQuestionState = composeQuestionState(0))
            }

            savedPages.clear()
            getArticlesMentioned().forEach { pageSummary ->
                val inAnyList = AppDatabase.instance.readingListPageDao().findPageInAnyList(pageSummary.getPageTitle(wikiSite)) != null
                if (inAnyList) {
                    savedPages.add(pageSummary)
                }
            }

            if (dbGameResults != null) {
                // If this game exists in the database, meaning it's already been finished, then
                // compose a "finished" state that will trigger a GameEnded event.
                currentState = currentState.copy(
                    answerState = JsonUtil.decodeFromString<List<Boolean>>(dbGameResults.gameData) ?: listOf(),
                    currentQuestionState = composeQuestionState(0),
                    currentQuestionIndex = currentState.totalQuestions
                )
                _gameState.postValue(GameEnded(currentState, getGameStatistics()))
            } else if (currentState.currentQuestionState.month == currentMonth && currentState.currentQuestionState.day == currentDay && currentState.currentQuestionIndex == 0 && !currentState.currentQuestionState.goToNext) {
                // we're just starting the current game.
                _gameState.postValue(GameStarted(currentState))
            } else if (currentState.currentQuestionState.month == currentMonth && currentState.currentQuestionState.day == currentDay &&
                currentState.currentQuestionIndex >= currentState.totalQuestions) {
                // we're already done for today.
                _gameState.postValue(GameEnded(currentState, getGameStatistics()))
            } else if (currentState.currentQuestionState.month != currentMonth || currentState.currentQuestionState.day != currentDay) {
                // the date in our current state doesn't match the requested date, so start a new game.
                currentState = currentState.copy(currentQuestionState = composeQuestionState(0), currentQuestionIndex = 0, answerState = List(MAX_QUESTIONS) { false })
                _gameState.postValue(GameStarted(currentState))
            } else {
                // we're in the middle of a game.
                if (currentState.currentQuestionState.goToNext) {
                    // the user must have exited the activity before going to the next question,
                    // so we can fake submitting the current question.
                    submitCurrentResponse(0)
                } else {
                    // we're truly in the middle of a game, and in the middle of the current question.
                    _gameState.postValue(CurrentQuestion(currentState))
                }
            }

            persistState()
        }
    }

    fun submitCurrentResponse(selectedYear: Int) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            _gameState.postValue(Resource.Error(throwable))
        }) {
            if (currentState.currentQuestionState.goToNext) {
                WikiGamesEvent.submit("next_click", "game_play", slideName = getCurrentScreenName(), isArchive = isArchiveGame)

                val nextQuestionIndex = currentState.currentQuestionIndex + 1

                if (nextQuestionIndex >= currentState.totalQuestions) {
                    currentState = currentState.copy(currentQuestionIndex = nextQuestionIndex)

                    var shouldInsert = false
                    // Get game data from the database OR create a blank object.
                    val gameHistory = withContext(Dispatchers.IO) {
                        AppDatabase.instance.dailyGameHistoryDao().findGameHistoryByDate(
                            gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                            language = wikiSite.languageCode,
                            year = currentDate.year,
                            month = currentDate.monthValue,
                            day = currentDate.dayOfMonth
                        ) ?: run {
                            shouldInsert = true
                            DailyGameHistory(
                                gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                                language = wikiSite.languageCode,
                                year = currentDate.year,
                                month = currentDate.monthValue,
                                day = currentDate.dayOfMonth,
                                score = 0,
                                playType = if (isArchiveGame) PlayTypes.PLAYED_ON_ARCHIVE.ordinal else PlayTypes.PLAYED_ON_SAME_DAY.ordinal,
                                gameData = null
                            )
                        }
                    }

                    gameHistory.apply {
                        score = currentState.answerState.count { it }
                        gameData = JsonUtil.encodeToString(currentState.answerState)
                    }

                    // Update or insert the game history in the database.
                    withContext(Dispatchers.IO) {
                        if (shouldInsert) {
                            AppDatabase.instance.dailyGameHistoryDao().insert(gameHistory)
                        } else {
                            AppDatabase.instance.dailyGameHistoryDao().update(gameHistory)
                        }
                    }
                    _gameState.postValue(GameEnded(currentState, getGameStatistics()))
                } else {
                    currentState = currentState.copy(
                        currentQuestionState = composeQuestionState(nextQuestionIndex),
                        currentQuestionIndex = nextQuestionIndex
                    )
                    _gameState.postValue(CurrentQuestion(currentState))
                }
            } else {
                WikiGamesEvent.submit("select_click", "game_play", slideName = getCurrentScreenName(), isArchive = isArchiveGame)

                currentState = currentState.copy(
                    currentQuestionState = currentState.currentQuestionState.copy(
                        yearSelected = selectedYear,
                        goToNext = true
                    )
                )

                val isCorrect = selectedYear == min(
                    currentState.currentQuestionState.event1.year,
                    currentState.currentQuestionState.event2.year
                )

                currentState = currentState.copy(
                    answerState = currentState.answerState.toMutableList()
                        .apply { set(currentState.currentQuestionIndex, isCorrect) }
                )

                if (isCorrect) {
                    _gameState.postValue(CurrentQuestionCorrect(currentState))
                } else {
                    _gameState.postValue(CurrentQuestionIncorrect(currentState))
                }
            }
            persistState()
        }
    }

    fun getCurrentScreenName(): String {
        return if (_gameState.value is GameEnded) {
            "game_end"
        } else if (_gameState.value == null || ::currentState.isInitialized.not() || _gameState.value is Resource.Loading) {
            "game_loading"
        } else {
            "game_play_" + (currentState.currentQuestionIndex + 1)
        }
    }

    fun getArticlesMentioned(): List<PageSummary> {
        val articles = mutableListOf<PageSummary>()
        events.forEach { event ->
            articles.addAll(event.pages)
        }
        return articles.distinctBy { it.apiTitle }
    }

    fun getEventByPageTitle(pageTitle: String): OnThisDay.Event {
        return events.find { it.pages.any { pageSummary -> pageSummary.apiTitle == pageTitle || pageSummary.displayTitle == pageTitle } } ?: events[0]
    }

    fun getQuestionCorrectByPageTitle(pageTitle: String): Boolean {
        val index = events.indexOfFirst { it.pages.any { pageSummary -> pageSummary.apiTitle == pageTitle || pageSummary.displayTitle == pageTitle } }
        return currentState.answerState[if (index >= 0) index / 2 else 0]
    }

    fun getThumbnailUrlForEvent(event: OnThisDay.Event): String? {
        return event.pages.firstOrNull { !it.thumbnailUrl.isNullOrEmpty() }?.thumbnailUrl
    }

    suspend fun getDataForArchiveCalendar(gameName: Int = WikiGames.WHICH_CAME_FIRST.ordinal, language: String): Map<Long, Int> {
        val history = AppDatabase.instance.dailyGameHistoryDao().getGameHistory(gameName, language)
        val map = history.associate {
            val scoreKey = DateDecorator.getDateKey(it.year, it.month, it.day)
           scoreKey to it.score
        }
        return map
    }

    fun getCurrentGameState(): GameState {
        return currentState
    }

    private fun composeQuestionState(index: Int): QuestionState {
        return QuestionState(events[index * 2], events[index * 2 + 1], currentMonth, currentDay)
    }

    private fun persistState(removeCurrentState: Boolean = false) {
        val totalState = JsonUtil.decodeFromString<TotalGameState>(Prefs.otdGameState) ?: TotalGameState()
        val langToState = totalState.langToState.toMutableMap()
        if (removeCurrentState) {
            langToState.remove(wikiSite.languageCode)
        } else {
            currentState.gamePlayDate = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            currentState.lastActiveDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            langToState[wikiSite.languageCode] = currentState
        }
        Prefs.otdGameState = JsonUtil.encodeToString(TotalGameState(langToState)).orEmpty()
    }

    fun relaunchForDate(date: LocalDate) {
        currentDate = date
        loadGameState(useDateFromState = false)
    }

    private suspend fun getGameStatistics(): GameStatistics {
        return withContext(Dispatchers.IO) {
            val totalGamesPlayed = async {
                AppDatabase.instance.dailyGameHistoryDao().getTotalGamesPlayed(
                    gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                    language = wikiSite.languageCode
                )
            }
            val averageScore = async {
                AppDatabase.instance.dailyGameHistoryDao().getAverageScore(
                    gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                    language = wikiSite.languageCode
                )
            }
            val currentStreak = async {
                AppDatabase.instance.dailyGameHistoryDao().getCurrentStreak(
                    gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                    language = wikiSite.languageCode
                )
            }

            GameStatistics(
                totalGamesPlayed.await(),
                averageScore.await(),
                currentStreak.await()
            )
        }
    }

    // TODO: remove this in May, 2026
    private suspend fun migrateGameHistoryFromPrefsToDatabase() {
        if (Prefs.otdGameHistory.isEmpty()) {
            return
        }
        val totalHistory = JsonUtil.decodeFromString<TotalGameHistory>(Prefs.otdGameHistory)?.langToHistory ?: return
        val dailyGameHistories = mutableListOf<DailyGameHistory>()
        totalHistory.forEach { (lang, gameHistory) ->
            gameHistory.history.forEach { (year, monthMap) ->
                monthMap.forEach { (month, dayMap) ->
                    dayMap.forEach { (day, answers) ->
                        val gameHistory = DailyGameHistory(
                            gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                            language = lang,
                            year = year,
                            month = month,
                            day = day,
                            score = answers.count { it }.toInt(),
                            playType = PlayTypes.PLAYED_ON_SAME_DAY.ordinal,
                            gameData = JsonUtil.encodeToString(answers)
                        )
                        dailyGameHistories.add(gameHistory)
                    }
                }
            }
        }
        AppDatabase.instance.dailyGameHistoryDao().insertAll(dailyGameHistories)
        Prefs.otdGameHistory = ""
    }

    @Serializable
    data class TotalGameHistory(
        val langToHistory: Map<String, GameHistory> = emptyMap()
    )

    @Serializable
    data class GameHistory(
        // map of:   year: month: day: list of answers
        val history: Map<Int, Map<Int, Map<Int, List<Boolean>>>> = emptyMap()
    )

    @Serializable
    data class TotalGameState(
        val langToState: Map<String, GameState> = emptyMap(),
    )

    @Serializable
    data class GameState(
        val totalQuestions: Int = Prefs.otdGameQuestionsPerDay,
        val currentQuestionIndex: Int = 0,

        // history of today's answers (correct vs incorrect)
        val answerState: List<Boolean> = List(MAX_QUESTIONS) { false },

        val currentQuestionState: QuestionState,

        var gamePlayDate: String = "",
        var lastActiveDate: String = ""
    )

    @Serializable
    data class GameStatistics(
        val totalGamesPlayed: Int = 0,
        val averageScore: Double? = null,
        val currentStreak: Int = 0
    )

    @Serializable
    data class QuestionState(
        val event1: OnThisDay.Event,
        val event2: OnThisDay.Event,
        val month: Int = 0,
        val day: Int = 0,
        val yearSelected: Int? = null,
        val goToNext: Boolean = false
    )

    class CurrentQuestion(val data: GameState) : Resource<GameState>()
    class CurrentQuestionCorrect(val data: GameState) : Resource<GameState>()
    class CurrentQuestionIncorrect(val data: GameState) : Resource<GameState>()
    class GameStarted(val data: GameState) : Resource<GameState>()
    class GameEnded(val data: GameState, val gameStatistics: GameStatistics) : Resource<GameState>()

    companion object {
        const val MAX_QUESTIONS = 5
        const val EXTRA_DATE = "date"

        val LANG_CODES_SUPPORTED = listOf("en", "de", "fr", "es", "pt", "ru", "ar", "tr", "zh")

        fun isLangSupported(lang: String): Boolean {
            return LANG_CODES_SUPPORTED.contains(lang)
        }

        fun dateReleasedForLang(lang: String): LocalDate {
            return if (lang == "de" || ReleaseUtil.isPreBetaRelease) LocalDate.of(2025, 2, 20) else LocalDate.of(2025, 5, 21)
        }
    }
}
