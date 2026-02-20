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
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.database.AppDatabase
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
import kotlin.math.min

class OnThisDayGameViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _gameState = MutableLiveData<Resource<GameState>>()
    val gameState: LiveData<Resource<GameState>> get() = _gameState

    val invokeSource = savedStateHandle.get<Constants.InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE)!!
    val wikiSite = savedStateHandle.get<WikiSite>(Constants.ARG_WIKISITE)!!

    // TODO: initialize the state earlier in the loading process, so that the state is nonnull
    // when the ViewModel is created, instead of only after the first loadGameState() call.
    private lateinit var currentState: GameState
    private var currentGameId: Int? = null

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
            migrateInProgressGameFromPrefsToDatabase()

            if (useDateFromState && !overrideDate) {
                currentDate = determineLastPlayedGameDate()
            }
            isArchiveGame = currentDate.isBefore(LocalDate.now())
            // load game state from database
            val gameHistory = AppDatabase.instance.dailyGameHistoryDao().findGameHistoryByDate(
                gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                language = wikiSite.languageCode,
                year = currentDate.year,
                month = currentMonth,
                day = currentDay
            )

            currentGameId = gameHistory?.id

            events.clear()
            events.addAll(OnThisDayGameProvider.getGameEvents(wikiSite, currentDate))

            currentState = buildGameState(gameHistory)
            savedPages.clear()
            getArticlesMentioned().forEach { pageSummary ->
                val inAnyList = AppDatabase.instance.readingListPageDao().findPageInAnyList(pageSummary.getPageTitle(wikiSite)) != null
                if (inAnyList) {
                    savedPages.add(pageSummary)
                }
            }
            publishGameState(gameHistory)
        }
    }

    private suspend fun determineLastPlayedGameDate(): LocalDate {
        val lastPlayedInfoMap = JsonUtil.decodeFromString<Map<String, LastPlayedInfo>>(Prefs.otdLastPlayedDate)
            ?: return LocalDate.now()
        val lastPlayedInfo = lastPlayedInfoMap[wikiSite.languageCode] ?: return LocalDate.now()
        return try {
            val sessionDate = LocalDate.parse(lastPlayedInfo.sessionDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val gamePlayDate = LocalDate.parse(lastPlayedInfo.gamePlayDate, DateTimeFormatter.ISO_LOCAL_DATE)

            if (sessionDate == LocalDate.now()) {
                val lastGame = AppDatabase.instance.dailyGameHistoryDao().findGameHistoryByDate(
                    gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                    language = wikiSite.languageCode,
                    year = gamePlayDate.year,
                    month = gamePlayDate.monthValue,
                    day = gamePlayDate.dayOfMonth
                )
                if (lastGame?.status == DailyGameHistory.GAME_IN_PROGRESS) {
                    gamePlayDate
                } else {
                    LocalDate.now()
                }
            } else {
                LocalDate.now()
            }
        } catch (_: Exception) {
           LocalDate.now()
        }
    }

    private fun buildGameState(gameHistory: DailyGameHistory?): GameState {
        return if (gameHistory != null) {
            val questionIndex = min(gameHistory.currentQuestionIndex, Prefs.otdGameQuestionsPerDay - 1)
            GameState(
                currentQuestionIndex = gameHistory.currentQuestionIndex,
                answerState = JsonUtil.decodeFromString<List<Boolean>>(gameHistory.gameData) ?: listOf(),
                currentQuestionState = composeQuestionState(questionIndex),
                status = gameHistory.status
            )
        } else {
            GameState(currentQuestionState = composeQuestionState(0))
        }
    }

    private suspend fun publishGameState(gameHistory: DailyGameHistory?) {
        if (currentState.status == DailyGameHistory.GAME_IN_PROGRESS) {
            if (currentState.currentQuestionIndex == 0) {
                // we're just starting the current game.
                _gameState.postValue(GameStarted(currentState))
            } else {
                // middle of the game
                _gameState.postValue(CurrentQuestion(currentState))
            }
        } else if (currentState.status == DailyGameHistory.GAME_COMPLETED) {
            currentState = currentState.copy(
                answerState = JsonUtil.decodeFromString<List<Boolean>>(gameHistory?.gameData) ?: listOf(),
                currentQuestionIndex = currentState.totalQuestions
            )
            _gameState.postValue(GameEnded(currentState, getGameStatistics(wikiSite.languageCode)))
        }
    }

    fun submitCurrentResponse(selectedYear: Int) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            _gameState.postValue(Resource.Error(throwable))
        }) {
            val nextQuestionIndex = currentState.currentQuestionIndex + 1
            // more questions or final question reached
            if (currentState.currentQuestionState.goToNext) {
                WikiGamesEvent.submit("next_click", "game_play", slideName = getCurrentScreenName(), isArchive = isArchiveGame)
                // game ended
                if (nextQuestionIndex >= currentState.totalQuestions) {
                    currentState = currentState.copy(currentQuestionIndex = nextQuestionIndex)
                    saveGameProgress(status = DailyGameHistory.GAME_COMPLETED, nextQuestionIndex = nextQuestionIndex)
                    _gameState.postValue(GameEnded(currentState, getGameStatistics(wikiSite.languageCode)))
                } else {
                    // moves to next question
                    currentState = currentState.copy(
                        currentQuestionState = composeQuestionState(nextQuestionIndex),
                        currentQuestionIndex = nextQuestionIndex
                    )
                    _gameState.postValue(CurrentQuestion(currentState))
                }
            } else {
                // when user wants to evaluate the answer
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
                saveGameProgress(status = DailyGameHistory.GAME_IN_PROGRESS, nextQuestionIndex = nextQuestionIndex)
            }
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

    private fun saveGameProgress(status: Int, nextQuestionIndex: Int) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                L.e(throwable)
            }
        ) {
            val dailyGameHistory = DailyGameHistory(
                id = currentGameId ?: 0,
                gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                language = wikiSite.languageCode,
                year = currentDate.year,
                month = currentDate.monthValue,
                day = currentDate.dayOfMonth,
                score = currentState.answerState.count { it },
                playType = if (isArchiveGame) PlayTypes.PLAYED_ON_ARCHIVE.ordinal else PlayTypes.PLAYED_ON_SAME_DAY.ordinal,
                gameData = JsonUtil.encodeToString(currentState.answerState),
                currentQuestionIndex = nextQuestionIndex,
                status = status
            )
            val resultId = AppDatabase.instance.dailyGameHistoryDao().upsert(dailyGameHistory).toInt()
            currentGameId = if (resultId > 0) resultId else currentGameId

            val lastPlayedDate = JsonUtil.decodeFromString<Map<String, LastPlayedInfo>>(Prefs.otdLastPlayedDate)?.toMutableMap() ?: mutableMapOf()
            lastPlayedDate[wikiSite.languageCode] = LastPlayedInfo(
                gamePlayDate = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                sessionDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
            Prefs.otdLastPlayedDate = JsonUtil.encodeToString(lastPlayedDate).orEmpty()
        }
    }

    private fun composeQuestionState(index: Int): QuestionState {
        return QuestionState(events[index * 2], events[index * 2 + 1], currentMonth, currentDay)
    }

    fun relaunchForDate(date: LocalDate) {
        currentDate = date
        loadGameState(useDateFromState = false)
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
                            score = answers.count { it },
                            playType = PlayTypes.PLAYED_ON_SAME_DAY.ordinal,
                            gameData = JsonUtil.encodeToString(answers),
                            currentQuestionIndex = currentState.currentQuestionIndex
                        )
                        dailyGameHistories.add(gameHistory)
                    }
                }
            }
        }
        AppDatabase.instance.dailyGameHistoryDao().insertAll(dailyGameHistories)
        Prefs.otdGameHistory = ""
    }

    // TODO: remove this in May, 2026
    private suspend fun migrateInProgressGameFromPrefsToDatabase() {
        if (Prefs.otdGameState.isEmpty()) {
            return
        }

        val totalState = JsonUtil.decodeFromString<TotalGameState>(Prefs.otdGameState) ?: return
        totalState.langToState.forEach { (lang, gameState) ->
            if (gameState.currentQuestionIndex < Prefs.otdGameQuestionsPerDay) {
                val gamePlayDate = LocalDate.parse(gameState.gamePlayDate, DateTimeFormatter.ISO_LOCAL_DATE)
                val lastActiveDate = LocalDate.parse(gameState.lastActiveDate, DateTimeFormatter.ISO_LOCAL_DATE)
                val dailyGameHistory = DailyGameHistory(
                    gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                    language = lang,
                    year = gamePlayDate.year,
                    month = gamePlayDate.monthValue,
                    day = gamePlayDate.dayOfMonth,
                    score = gameState.answerState.count { it },
                    playType = if (gamePlayDate.isBefore(LocalDate.now())) {
                        PlayTypes.PLAYED_ON_ARCHIVE.ordinal
                    } else {
                        PlayTypes.PLAYED_ON_SAME_DAY.ordinal
                    },
                    gameData = JsonUtil.encodeToString(gameState.answerState),
                    currentQuestionIndex = gameState.currentQuestionIndex,
                    status = DailyGameHistory.GAME_IN_PROGRESS
                )
                AppDatabase.instance.dailyGameHistoryDao().upsert(dailyGameHistory)

                val lastPlayedMap = JsonUtil.decodeFromString<Map<String, LastPlayedInfo>>(Prefs.otdLastPlayedDate)?.toMutableMap() ?: mutableMapOf()
                lastPlayedMap[lang] = LastPlayedInfo(
                    gamePlayDate = gamePlayDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    sessionDate = lastActiveDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                )
                Prefs.otdLastPlayedDate = JsonUtil.encodeToString(lastPlayedMap).orEmpty()
                Prefs.otdGameState = ""
            }
        }
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
        var status: Int = DailyGameHistory.GAME_IN_PROGRESS,
        var gamePlayDate: String = "",
        var lastActiveDate: String = ""
    )

    @Serializable
    data class GameStatistics(
        val totalGamesPlayed: Int = 0,
        val averageScore: Double? = null,
        val currentStreak: Int = 0,
        val bestStreak: Int = 0
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

    @Serializable
    data class LastPlayedInfo(
        val gamePlayDate: String,
        val sessionDate: String
    )

    class CurrentQuestion(val data: GameState) : Resource<GameState>()
    class CurrentQuestionCorrect(val data: GameState) : Resource<GameState>()
    class CurrentQuestionIncorrect(val data: GameState) : Resource<GameState>()
    class GameStarted(val data: GameState) : Resource<GameState>()
    class GameEnded(val data: GameState, val gameStatistics: GameStatistics) : Resource<GameState>()

    companion object {
        const val MAX_QUESTIONS = 5
        const val EXTRA_DATE = "date"

        val LANG_CODES_SUPPORTED = listOf("en", "de", "fr", "es", "pt", "ru", "ar", "tr", "zh").flatMap { langCode ->
            WikipediaApp.instance.languageState.getLanguageVariants(langCode) ?: listOf(langCode)
        }

        fun isLangSupported(lang: String): Boolean {
            return LANG_CODES_SUPPORTED.contains(lang)
        }

        fun dateReleasedForLang(lang: String): LocalDate {
            return if (lang == "de" || ReleaseUtil.isPreBetaRelease) LocalDate.of(2025, 2, 20) else LocalDate.of(2025, 5, 21)
        }

        suspend fun getGameStatistics(languageCode: String): GameStatistics {
            return withContext(Dispatchers.IO) {
                val totalGamesPlayed = async {
                    AppDatabase.instance.dailyGameHistoryDao().getTotalGamesPlayed(
                        gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                        language = languageCode
                    )
                }
                val averageScore = async {
                    AppDatabase.instance.dailyGameHistoryDao().getAverageScore(
                        gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                        language = languageCode
                    )
                }
                val currentStreak = async {
                    AppDatabase.instance.dailyGameHistoryDao().getCurrentStreak(
                        gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                        language = languageCode
                    )
                }

                val bestStreak = async {
                    AppDatabase.instance.dailyGameHistoryDao().getBestStreak(
                        gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                        language = languageCode
                    )
                }

                GameStatistics(
                    totalGamesPlayed = totalGamesPlayed.await(),
                    averageScore = averageScore.await(),
                    currentStreak = currentStreak.await(),
                    bestStreak = bestStreak.await()
                )
            }
        }
    }
}
