package org.wikipedia.yearinreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UiState
import org.wikipedia.util.log.L
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class YearInReviewViewModel() : ViewModel() {
    private val currentYear = LocalDate.now().year
    private val startTime: Instant = LocalDateTime.of(currentYear, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC)
    private val endTime: Instant = LocalDateTime.of(currentYear, 12, 31, 23, 59, 59).toInstant(ZoneOffset.UTC)
    private val startTimeInMillis = startTime.toEpochMilli()
    private val endTimeInMillis = endTime.toEpochMilli()
    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
        _uiScreenListState.value = UiState.Success(
            data = listOf(nonEnglishCollectiveReadCountData, nonEnglishCollectiveEditCountData, nonEnglishCollectiveReadCountData, nonEnglishCollectiveEditCountData)
        )
    }
    private var _uiScreenListState = MutableStateFlow<UiState<List<YearInReviewScreenData>>>(UiState.Loading)
    val uiScreenListState = _uiScreenListState.asStateFlow()

    private var _canShowSurvey: Boolean = false
    var canShowSurvey: Boolean
        get() = _canShowSurvey && !Prefs.yirSurveyShown
        set(value) {
            _canShowSurvey = value
        }

    init {
        fetchPersonalizedData()
    }

    fun fetchPersonalizedData() {
        viewModelScope.launch(handler) {

            _uiScreenListState.value = UiState.Loading

            // TODO: handle remote config to show numbers, maybe grab generic content from the config.
            val remoteConfig = RemoteConfig.config

            // TODO: content TBD
            val savedArticlesCountJob = async {
                val savedArticlesCount = AppDatabase.instance.readingListPageDao().getDistinctEntriesCountBetween(startTimeInMillis, endTimeInMillis)
                if (savedArticlesCount >= MINIMUM_SAVED_ARTICLE_COUNT) {
                    val savedArticlesTitles = AppDatabase.instance.readingListPageDao().getLastestArticleTitles(MINIMUM_SAVED_ARTICLE_COUNT)
                        .map { StringUtil.fromHtml(it).toString() }
                    YearInReviewScreenData.StandardScreen(
                        animatedImageResource = R.drawable.wyir_block_5_resize,
                        staticImageResource = R.drawable.personal_slide_01,
                        headlineText = WikipediaApp.instance.resources.getQuantityString(
                            R.plurals.year_in_review_read_count_headline,
                            savedArticlesCount,
                            savedArticlesCount
                        ),
                        bodyText = WikipediaApp.instance.resources.getQuantityString(
                            R.plurals.year_in_review_read_count_bodytext,
                            savedArticlesCount,
                            savedArticlesCount,
                            savedArticlesTitles[0],
                            savedArticlesTitles[1],
                            savedArticlesTitles[2]
                        )
                    )
                } else {
                    nonEnglishCollectiveReadCountData
                }
            }

            val readCountJob = async {
                val readCount = AppDatabase.instance.historyEntryDao().getDistinctEntriesCountBetween(startTimeInMillis, endTimeInMillis)
                if (readCount >= MINIMUM_READ_COUNT) {
                    val readCountApiTitles = AppDatabase.instance.historyEntryDao().getLastestArticleTitles(MINIMUM_READ_COUNT)
                        .map { StringUtil.fromHtml(it).toString() }
                    YearInReviewScreenData.StandardScreen(
                        animatedImageResource = R.drawable.wyir_block_5_resize,
                        staticImageResource = R.drawable.personal_slide_01,
                        headlineText = WikipediaApp.instance.resources.getQuantityString(
                            R.plurals.year_in_review_read_count_headline,
                            readCount,
                            readCount
                        ),
                        bodyText = WikipediaApp.instance.resources.getQuantityString(
                            R.plurals.year_in_review_read_count_bodytext,
                            readCount,
                            readCount,
                            readCountApiTitles[0],
                            readCountApiTitles[1],
                            readCountApiTitles[2]
                        )
                    )
                } else {
                    nonEnglishCollectiveReadCountData
                }
            }

            // TODO: think about the actual data to show.

            val impactDataJob = async {
                if (AccountUtil.isLoggedIn) {
                    val wikiSite = WikipediaApp.instance.wikiSite
                    val now = Instant.now().epochSecond
                    val impact: GrowthUserImpact
                    val impactLastResponseBodyMap = Prefs.impactLastResponseBody.toMutableMap()
                    val impactResponse = impactLastResponseBodyMap[wikiSite.languageCode]
                    if (impactResponse.isNullOrEmpty() || abs(now - Prefs.impactLastQueryTime) > TimeUnit.HOURS.toSeconds(
                            12
                        )
                    ) {
                        val userId =
                            ServiceFactory.get(wikiSite).getUserInfo().query?.userInfo?.id!!
                        impact = ServiceFactory.getCoreRest(wikiSite).getUserImpact(userId)
                        impactLastResponseBodyMap[wikiSite.languageCode] =
                            JsonUtil.encodeToString(impact).orEmpty()
                        Prefs.impactLastResponseBody = impactLastResponseBodyMap
                        Prefs.impactLastQueryTime = now
                    } else {
                        impact = JsonUtil.decodeFromString(impactResponse)!!
                    }

                    val pagesResponse = ServiceFactory.get(wikiSite).getInfoByPageIdsOrTitles(
                        titles = impact.topViewedArticles.keys.joinToString(separator = "|")
                    )

                    // Transform the response to a map of PageTitle to ArticleViews
                    val pageMap = pagesResponse.query?.pages?.associate { page ->
                        val pageTitle = PageTitle(
                            text = page.title,
                            wiki = wikiSite,
                            thumbUrl = page.thumbUrl(),
                            description = page.description,
                            displayText = page.displayTitle(wikiSite.languageCode)
                        )
                        pageTitle to impact.topViewedArticles[pageTitle.text]!!
                    } ?: emptyMap()

                    impact.topViewedArticlesWithPageTitle = pageMap
                    val editCount = impact.totalUserEditCount
                    if (editCount >= MINIMUM_EDIT_COUNT) {
                        YearInReviewScreenData.StandardScreen(
                            animatedImageResource = R.drawable.wyir_bytes,
                            staticImageResource = R.drawable.english_slide_05,
                            headlineText = WikipediaApp.instance.resources.getQuantityString(
                                R.plurals.year_in_review_edit_count_headline,
                                editCount,
                                editCount
                            ),
                            bodyText = WikipediaApp.instance.resources.getQuantityString(
                                R.plurals.year_in_review_edit_count_bodytext,
                                editCount,
                                editCount
                            )
                        )
                    } else {
                        // TODO: show generic content
                        nonEnglishCollectiveEditCountData
                    }
                } else {
                    // TODO: show non-logged in user content
                    nonEnglishCollectiveEditCountData
                }
            }

            // TODO: make sure return enough slides here
            _uiScreenListState.value = UiState.Success(
                data = listOf(readCountJob.await(), savedArticlesCountJob.await(), impactDataJob.await())
            )
        }
    }

    suspend fun readingStats(): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            val readCount = AppDatabase.instance.historyEntryDao().getDistinctEntriesCountBetween(startTimeInMillis, endTimeInMillis)
            if (readCount >= MINIMUM_READ_COUNT) {
                val readCountApiTitles = AppDatabase.instance.historyEntryDao().getLastestArticleTitles(MINIMUM_READ_COUNT)
                    .map { StringUtil.fromHtml(it).toString() }
                YearInReviewScreenData.StandardScreen(
                    animatedImageResource = R.drawable.wyir_block_5_resize,
                    staticImageResource = R.drawable.personal_slide_01,
                    headlineText = WikipediaApp.instance.resources.getQuantityString(
                        R.plurals.year_in_review_read_count_headline,
                        readCount,
                        readCount
                    ),
                    bodyText = WikipediaApp.instance.resources.getQuantityString(
                        R.plurals.year_in_review_read_count_bodytext,
                        readCount,
                        readCount,
                        readCountApiTitles[0],
                        readCountApiTitles[1],
                        readCountApiTitles[2]
                    )
                )
            } else {
                nonEnglishCollectiveReadCountData
            }
        }
    }

    suspend fun nonLoggedInEnglishGeneralSlides(): List<YearInReviewScreenData.StandardScreen> {
        return withContext(Dispatchers.IO) {
            // TODO: Show a bunch of generic slides for English users - non-logged in.
            listOf(
                spentReadingHoursScreen(1),
                spentReadingHoursScreen(1)
            )
        }
    }

    suspend fun nonLoggedInGeneralSlides(): List<YearInReviewScreenData.StandardScreen> {
        return withContext(Dispatchers.IO) {
            // TODO: Show a bunch of generic slides for non-English users - non-logged in.
            listOf(
                spentReadingHoursScreen(1),
                spentReadingHoursScreen(1)
            )
        }
    }

    suspend fun loggedInEnglishSlides(): List<YearInReviewScreenData.StandardScreen> {
        return withContext(Dispatchers.IO) {
            // TODO: Show a bunch of generic slides for logged in English users.
            listOf(
                spentReadingHoursScreen(1),
                spentReadingHoursScreen(1)
            )
        }
    }

    suspend fun loggedInGeneralSlides(): List<YearInReviewScreenData.StandardScreen> {
        return withContext(Dispatchers.IO) {
            // TODO: Show a bunch of generic slides for logged in users.
            listOf(
                spentReadingHoursScreen(1),
                spentReadingHoursScreen(1)
            )
        }
    }

    suspend fun spentReadingHoursScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir123
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "We spent over 2 billion hours reading",
                bodyText = "TBD"
            )
        }
    }

    suspend fun spentReadingMinutesScreen(isEnglishWiki: Boolean, vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir99 + yir100 => need to check if it is en or not en
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "You spent 924 minutes reading 350 articles in 2025",
                bodyText = "TBD"
            )
        }
    }

    suspend fun popularArticlesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir127 + 104
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "English Wikipedia’s most popular articles",
                bodyText = "TBD"
            )
        }
    }

    suspend fun globalSavedArticlesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir126
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "We had over 37 million saved articles",
                bodyText = "TBD"
            )
        }
    }

    suspend fun availableLanguagesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir123
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "Wikipedia was available in more than 300 languages",
                bodyText = "TBD"
            )
        }
    }

    suspend fun viewedArticlesTimesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir125 + yir103
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "We have viewed Wikipedia articles more than 1 billion times",
                bodyText = "TBD"
            )
        }
    }

    suspend fun readingPatternsScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir106 + yir107
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "You have clear reading patterns",
                bodyText = "TBD"
            )
        }
    }

    suspend fun interestingCategoriesScreen(isEnglishWiki: Boolean, params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir108 + yir110 => confirm the difference.
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "Your most interesting categories",
                bodyText = "TBD"
            )
        }
    }

    suspend fun topArticlesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir109 + yir105
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "Your top articles",
                bodyText = "TBD"
            )
        }
    }

    suspend fun geoWithArticlesScreen(vararg params: Int): YearInReviewScreenData.GeoScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir112
            YearInReviewScreenData.GeoScreen(
                coordinates = mapOf("lat" to listOf(34, 56), "lon" to listOf(-123, 45)),
                headlineText = "Articles you read are closest to France",
                bodyText = "TBD"
            )
        }
    }

    suspend fun localSavedArticlesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir113
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "You saved 25 articles",
                bodyText = "TBD"
            )
        }
    }

    suspend fun editedTimesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir114
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "You edited Wikipedia 150 times",
                bodyText = "TBD"
            )
        }
    }

    suspend fun editedViewsScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir115
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "Your edits have been viewed more than 14,791 times recently",
                bodyText = "TBD"
            )
        }
    }

    suspend fun editorsEditsScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir116
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "Editors on the official Wikipedia apps made more than 452,257 edits",
                bodyText = "TBD"
            )
        }
    }

    suspend fun editedPerMinuteScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir117
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "Wikipedia was edited 342 times per minute",
                bodyText = "TBD"
            )
        }
    }

    suspend fun editorsChangesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir118
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "Editors made nearly 82 million changes this year",
                bodyText = "TBD"
            )
        }
    }

    suspend fun addedBytesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir119
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "Over 3 billion bytes added",
                bodyText = "TBD"
            )
        }
    }

    suspend fun newIconUnlockedScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir121
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "New icon unlocked",
                bodyText = "TBD",
                unlockIcon = UnlockIconConfig(
                    isUnlocked = true
                )
            )
        }
    }

    suspend fun unlockCustomIconScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        return withContext(Dispatchers.IO) {
            // TODO: yir122
            YearInReviewScreenData.StandardScreen(
                animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
                staticImageResource = R.drawable.year_in_review_puzzle_pieces,
                headlineText = "Unlock your custom contributor icon",
                bodyText = "TBD",
                bottomButton = ButtonConfig(
                    text = "Donate",
                    onClick = { /* TODO: handle click */ }
                )
            )
        }
    }

    companion object {
        private const val MINIMUM_READ_COUNT = 3
        private const val MINIMUM_SAVED_ARTICLE_COUNT = 3
        private const val MINIMUM_EDIT_COUNT = 1

        val nonEnglishCollectiveReadCountData = YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.wyir_puzzle_3,
            staticImageResource = R.drawable.non_english_slide_01,
            headlineText = R.string.year_in_review_non_english_collective_readcount_headline,
            bodyText = R.string.year_in_review_non_english_collective_readcount_bodytext,
        )

        val nonEnglishCollectiveEditCountData = YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.wyir_puzzle_2_v5,
            staticImageResource = R.drawable.english_slide_01_and_non_english_slide_05,
            headlineText = R.string.year_in_review_non_english_collective_editcount_headline,
            bodyText = R.string.year_in_review_non_english_collective_editcount_bodytext,
        )
    }
}
