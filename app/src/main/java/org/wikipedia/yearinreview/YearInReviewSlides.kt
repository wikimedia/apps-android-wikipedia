package org.wikipedia.yearinreview

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.history.db.HistoryEntryWithImage
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.yearinreview.YearInReviewScreenData.CustomIconScreen
import org.wikipedia.yearinreview.YearInReviewScreenData.HighlightItem
import org.wikipedia.yearinreview.YearInReviewScreenData.HighlightsScreen
import org.wikipedia.yearinreview.YearInReviewScreenData.ReadingPatterns
import org.wikipedia.yearinreview.YearInReviewScreenData.StandardScreen
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

class YearInReviewSlides(
    val context: Context,
    val currentYear: Int,
    val isEditor: Boolean,
    val isLoggedIn: Boolean,
    val isEnglishWiki: Boolean,
    val isFundraisingAllowed: Boolean,
    val config: RemoteConfig.RemoteConfigYearInReview,
    val pagesWithCoordinates: List<HistoryEntryWithImage>,
    val yearInReviewModel: YearInReviewModel
) {
    private val numberFormatter = NumberFormat.getNumberInstance()

    private fun englishReadingHoursScreen(): StandardScreen {
        val bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_english_reading_hours_body_first,
            config.hoursReadEN.toInt(), config.hoursReadEN) + " " +
                context.resources.getQuantityString(R.plurals.year_in_review_slide_english_reading_hours_body_second,
                    config.yearsReadEN, config.yearsReadEN, currentYear)
        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_clock,
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_english_reading_hours_headline, config.hoursReadEN.toInt(), config.hoursReadEN),
            bodyText = bodyText
        )
    }

    private fun spentReadingMinutesScreen(): StandardScreen {
        if (yearInReviewModel.localReadingArticlesCount < YearInReviewViewModel.MIN_READING_ARTICLES ||
            yearInReviewModel.totalReadingTimeMinutes < YearInReviewViewModel.MIN_READING_MINUTES) {
            return if (isEnglishWiki) {
                englishReadingHoursScreen()
            } else {
                availableLanguagesScreen()
            }
        }

        val headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_headline_first,
            yearInReviewModel.totalReadingTimeMinutes.toInt(), yearInReviewModel.totalReadingTimeMinutes) + " " +
                context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_headline_second,
                    yearInReviewModel.localReadingArticlesCount, yearInReviewModel.localReadingArticlesCount, currentYear)

        val rankPercentage = config.topReadPercentages.find {
            yearInReviewModel.localReadingArticlesCount > it.min && yearInReviewModel.localReadingArticlesCount <= it.max
        }?.identifier

        var bodyText = ""
        rankPercentage?.let {
            bodyText += context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_body_top,
                config.averageArticlesReadPerYear, "<b>$it</b>", config.averageArticlesReadPerYear) + "<br /><br />"
        }

        bodyText += if (isEnglishWiki) {
            context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_body_english_first,
                config.hoursReadEN.toInt(), config.hoursReadEN) + " " +
                    context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_body_english_second,
                        config.yearsReadEN, config.yearsReadEN, currentYear)
        } else {
            context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_body_global_first,
                config.articles.toInt(), config.articles) + " " +
                    context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_body_global_second,
                        config.languages, config.languages, currentYear)
        }

        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_walk,
            imageModifier = Modifier.fillMaxSize(),
            headlineText = headlineText,
            bodyText = bodyText
        )
    }

    private fun popularEnglishArticlesScreen(): StandardScreen {

        val popularEnglishArticlesText = buildListWithNumbers(config.topReadEN)
        val popularEnglishArticlesBlogUrl = context.getString(R.string.year_in_review_popular_english_articles_blog_url)

        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_browser,
            headlineText = context.getString(R.string.year_in_review_slide_popular_english_articles_headline),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_popular_english_articles_body,
                config.topReadEN.size, config.topReadEN.size, popularEnglishArticlesText, popularEnglishArticlesBlogUrl)
        )
    }

    private fun appSavedArticlesScreen(): StandardScreen {
        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_cloud,
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_global_saved_articles_headline, config.savedArticlesApps.toInt(), config.savedArticlesApps),
            bodyText = context.getString(R.string.year_in_review_slide_global_saved_articles_body)
        )
    }

    private fun availableLanguagesScreen(): StandardScreen {
        val bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_available_languages_body_first,
            config.articles.toInt(), config.articles) + " " +
                context.resources.getQuantityString(R.plurals.year_in_review_slide_available_languages_body_second,
                    config.languages, config.languages, currentYear)
        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_stone,
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_available_languages_headline, config.languages, config.languages),
            bodyText = bodyText
        )
    }

    private fun viewedArticlesTimesScreen(): StandardScreen {
        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_browser,
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_viewed_articles_times_headline, config.viewsApps.toInt(), config.viewsApps),
            bodyText = context.getString(R.string.year_in_review_slide_viewed_articles_times_body)
        )
    }

    private fun readingPatternsScreen(): StandardScreen? {
        if (yearInReviewModel.localReadingArticlesCount < YearInReviewViewModel.MIN_READING_ARTICLES) {
            return null
        }
        val favoriteTimeText = when (yearInReviewModel.favoriteTimeToRead) {
            in 0..5 -> context.getString(R.string.year_in_review_slide_reading_pattern_late_night)
            in 5..12 -> context.getString(R.string.year_in_review_slide_reading_pattern_morning)
            in 12..13 -> context.getString(R.string.year_in_review_slide_reading_pattern_midday)
            in 13..17 -> context.getString(R.string.year_in_review_slide_reading_pattern_afternoon)
            in 17..21 -> context.getString(R.string.year_in_review_slide_reading_pattern_evening)
            else -> context.getString(R.string.year_in_review_slide_reading_pattern_night)
        }
        val favoriteDayText = DayOfWeek.of(yearInReviewModel.favoriteDayToRead)
            .getDisplayName(TextStyle.FULL, Locale.getDefault())
        val favoriteMonthText = Month.of(yearInReviewModel.favoriteMonthDidMostReading)
            .getDisplayName(TextStyle.FULL, Locale.getDefault())
        return ReadingPatterns(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_clock,
            headlineText = context.getString(R.string.year_in_review_slide_reading_patterns_headline),
            favoriteTimeText = favoriteTimeText,
            favoriteDayText = favoriteDayText,
            favoriteMonthText = favoriteMonthText
        )
    }

    private fun topCategoriesScreen(): StandardScreen? {
        if (yearInReviewModel.localTopCategories.isEmpty() || yearInReviewModel.localTopCategories.size < YearInReviewViewModel.MIN_TOP_CATEGORY) {
            return null
        }

        val topCategoriesText = buildListWithNumbers(yearInReviewModel.localTopCategories)

        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_farmer,
            headlineText = context.getString(R.string.year_in_review_slide_top_categories_headline),
            bodyText = context.getString(R.string.year_in_review_slide_top_categories_body, currentYear, topCategoriesText)
        )
    }

    private fun topArticlesScreen(): StandardScreen? {
        if (yearInReviewModel.localTopVisitedArticles.isEmpty()) {
            return null
        }

        val topArticlesText = buildListWithNumbers(yearInReviewModel.localTopVisitedArticles)
        val quantity = yearInReviewModel.localTopVisitedArticles.size

        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_sundial,
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_top_articles_headline, quantity),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_top_articles_body, quantity, currentYear, topArticlesText)
        )
    }

    private fun geoWithArticlesScreen(): YearInReviewScreenData.GeoScreen? {
        if (yearInReviewModel.largestClusterCountryName.isEmpty() || yearInReviewModel.largestClusterArticles.size < YearInReviewViewModel.MIN_ARTICLES_PER_MAP_CLUSTER) {
            return null
        }
        return YearInReviewScreenData.GeoScreen(
            allowDonate = isFundraisingAllowed,
            largestClusterTopLeft = yearInReviewModel.largestClusterTopLeft,
            largestClusterBottomRight = yearInReviewModel.largestClusterBottomRight,
            pagesWithCoordinates = pagesWithCoordinates,
            headlineText = context.resources.getString(R.string.year_in_review_slide_geo_headline, yearInReviewModel.largestClusterCountryName),
            bodyText = context.resources.getString(R.string.year_in_review_slide_geo_body, yearInReviewModel.largestClusterCountryName, yearInReviewModel.largestClusterArticles[0], yearInReviewModel.largestClusterArticles[1])
        )
    }

    private fun localSavedArticlesScreen(): StandardScreen {
        if (yearInReviewModel.localSavedArticlesCount < YearInReviewViewModel.MIN_SAVED_ARTICLES) {
            return appSavedArticlesScreen()
        }
        val appSavedArticlesSize = config.savedArticlesApps.toInt()
        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_cloud,
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_saved_articles_headline, yearInReviewModel.localSavedArticlesCount, yearInReviewModel.localSavedArticlesCount),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_saved_articles_body,
                appSavedArticlesSize, yearInReviewModel.localSavedArticles[0], yearInReviewModel.localSavedArticles[1],
                yearInReviewModel.localSavedArticles[2], config.savedArticlesApps)
        )
    }

    private fun editedTimesScreen(): StandardScreen {
        val userEditsCount = yearInReviewModel.userEditsCount
        var formattedUserEditsNumber = NumberFormat.getNumberInstance(Locale.getDefault()).format(yearInReviewModel.userEditsCount)
        if (userEditsCount > YearInReviewViewModel.MAX_EDITED_TIMES) {
            formattedUserEditsNumber = "${YearInReviewViewModel.MAX_EDITED_TIMES}+"
        }
        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_worker,
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edited_times_headline, userEditsCount, formattedUserEditsNumber),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edited_times_body, config.edits.toInt(), config.edits)
        )
    }

    private fun editsViewedTimesScreen(): StandardScreen? {
        if (yearInReviewModel.userEditsViewedTimes <= 0L) {
            return null
        }
        val quantity = yearInReviewModel.userEditsViewedTimes.toInt()
        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_pencil,
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edits_viewed_times_headline, quantity, yearInReviewModel.userEditsViewedTimes),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edits_viewed_times_body, quantity, yearInReviewModel.userEditsViewedTimes)
        )
    }

    private fun appEditedTimesScreen(): StandardScreen {
        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_worker,
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_app_edited_times_headline, config.edits.toInt(), config.edits),
            bodyText = context.getString(R.string.year_in_review_slide_app_edited_times_body)
        )
    }

    private fun editedPerMinuteScreen(): StandardScreen {
        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_bytes,
            imageModifier = Modifier.fillMaxSize(),
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edited_per_minute_headline, config.editsPerMinute, config.editsPerMinute),
            bodyText = context.getString(R.string.year_in_review_slide_edited_per_minute_body, context.getString(R.string.editing_learn_more_url))
        )
    }

    private fun englishEditedTimesScreen(): StandardScreen {
        val bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_english_edited_times_body_first,
            config.edits.toInt(), config.edits, config.editsEN) + " " +
                context.resources.getQuantityString(R.plurals.year_in_review_slide_english_edited_times_body_second,
                    config.editsEN.toInt(), config.editsEN)
        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_worker,
            imageModifier = Modifier.fillMaxSize(),
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_english_edited_times_headline, config.edits.toInt(), config.edits),
            bodyText = bodyText
        )
    }

    private fun addedBytesScreen(): StandardScreen {
        return StandardScreen(
            allowDonate = isFundraisingAllowed,
            imageResource = R.drawable.yir_puzzle_bytes,
            imageModifier = Modifier.fillMaxSize(),
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_bytes_added_headline, config.bytesAddedEN.toInt(), config.bytesAddedEN),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_bytes_added_body,
                config.bytesAddedEN.toInt(), currentYear, config.bytesAddedEN, context.getString(R.string.editing_learn_more_url))
        )
    }

    private fun loggedInHighlightScreen(): HighlightsScreen {
        return HighlightsScreen(
            highlights = buildList {
                if (yearInReviewModel.localTopVisitedArticles.isNotEmpty()) {
                    val topVisitedArticles = yearInReviewModel.localTopVisitedArticles.take(3)
                    add(
                        HighlightItem(
                            title = context.resources.getQuantityString(R.plurals.year_in_review_highlights_logged_in_most_read_article_title, topVisitedArticles.size),
                            items = topVisitedArticles,
                            highlightColor = ComposeColors.Blue600
                        )
                    )
                }
                add(
                    HighlightItem(
                        title = context.resources.getQuantityString(R.plurals.year_in_review_highlights_logged_in_minutes_read_title, yearInReviewModel.totalReadingTimeMinutes.toInt()),
                        singleValue = numberFormatter.format(yearInReviewModel.totalReadingTimeMinutes)
                    )
                )
                add(
                    HighlightItem(
                        title = context.resources.getString(R.string.year_in_review_highlights_logged_in_favorite_day_title),
                        singleValue = DayOfWeek.of(yearInReviewModel.favoriteDayToRead)
                            .getDisplayName(TextStyle.FULL, Locale.getDefault())
                    )
                )
                add(
                    HighlightItem(
                        title = context.resources.getQuantityString(R.plurals.year_in_review_highlights_logged_in_articles_read_title, yearInReviewModel.localReadingArticlesCount),
                        singleValue = numberFormatter.format(yearInReviewModel.localReadingArticlesCount)
                    )
                )
                val topCategories = yearInReviewModel.localTopCategories.take(3)
                add(
                    HighlightItem(
                        title = context.resources.getQuantityString(R.plurals.year_in_review_highlights_logged_in_articles_interested_categories_title, topCategories.size),
                        items = topCategories
                    )
                )
                if (isEditor) {
                    add(
                        HighlightItem(
                            title = context.resources.getQuantityString(R.plurals.year_in_review_highlights_logged_in_articles_edited_articles_title, yearInReviewModel.userEditsCount),
                            singleValue = numberFormatter.format(yearInReviewModel.userEditsCount)
                        )
                    )
                }
            },
            screenshotLink = context.getString(R.string.year_in_highlights_screenshot_url,
                YearInReviewViewModel.YIR_2025_PATH)
        )
    }

    private fun enWikiLoggedOutHighlightsScreen(): HighlightsScreen {
        return HighlightsScreen(
            highlights = listOf(
                HighlightItem(
                    title = context.resources.getQuantityString(R.plurals.year_in_review_highlights_logged_out_en_most_popular_title, config.topReadEN.size),
                    items = config.topReadEN,
                    highlightColor = ComposeColors.Blue600
                ),
                HighlightItem(
                    title = context.resources.getQuantityString(R.plurals.year_in_review_highlights_logged_out_en_hours_spent_title, config.hoursReadEN.toInt()),
                    singleValue = numberFormatter.format(config.hoursReadEN)
                ),
                HighlightItem(
                    title = context.resources.getQuantityString(R.plurals.year_in_review_highlights_logged_out_en_edits_title, config.editsEN.toInt()),
                    singleValue = numberFormatter.format(config.editsEN)
                )
            ),
            screenshotLink = context.getString(R.string.year_in_highlights_screenshot_url,
                YearInReviewViewModel.ARTICLES_2025_PATH)
        )
    }

    private fun nonEnWikiLoggedOutHighlightsScreen(): HighlightsScreen {
        return HighlightsScreen(
            highlights = listOf(
                HighlightItem(
                    title = context.resources.getQuantityString(R.plurals.year_in_review_highlights_logged_out_non_en_articles_title, config.viewsApps.toInt()),
                    singleValue = numberFormatter.format(config.viewsApps)
                ),
                HighlightItem(
                    title = context.resources.getQuantityString(R.plurals.year_in_review_highlights_logged_out_non_en_edits_title, config.editsApps.toInt()),
                    singleValue = numberFormatter.format(config.editsApps)
                ),
                HighlightItem(
                    title = context.resources.getString(R.string.year_in_review_highlights_logged_out_non_en_wikipedia_edited_title),
                    singleValue = context.resources.getQuantityString(R.plurals.year_in_review_highlights_logged_out_non_en_wikipedia_per_minute_label, config.editsPerMinute, config.editsPerMinute)
                )
            ),
            screenshotLink = context.getString(R.string.year_in_highlights_screenshot_url,
                YearInReviewViewModel.YIR_2025_PATH)
        )
    }

    private fun editorRoutes(): List<YearInReviewScreenData?> {
        return when {
            isEditor -> listOf(
                editedTimesScreen(),
                editsViewedTimesScreen()
            )
            !isEditor && !isEnglishWiki -> {
                listOf(
                    appEditedTimesScreen(),
                    editedPerMinuteScreen()
                )
            }
            else -> {
                listOf(
                    englishEditedTimesScreen(),
                    addedBytesScreen()
                )
            }
        }
    }

    private fun unlockedIconRoute(): YearInReviewScreenData? {
        return if (yearInReviewModel.isCustomIconUnlocked) {
            val contributorType = if (yearInReviewModel.userEditsCount > 0 && Prefs.donationResults.isNotEmpty()) {
                context.getString(R.string.year_in_review_slide_app_icon_donor_and_editor)
            } else if (yearInReviewModel.userEditsCount > 0) {
                context.getString(R.string.year_in_review_slide_app_icon_editor)
            } else {
                context.getString(R.string.year_in_review_slide_app_icon_donor)
            }
            CustomIconScreen(
                isFundraisingAllowed,
                headlineText = R.string.year_in_review_slide_app_icon_title_unlocked,
                bodyText = context.getString(R.string.year_in_review_slide_app_icon_body_unlocked, contributorType, YearInReviewViewModel.YIR_YEAR)
            )
        } else if (isFundraisingAllowed) {
            CustomIconScreen(
                allowDonate = true,
                headlineText = R.string.year_in_review_slide_app_icon_title_unlock,
                bodyText = context.getString(R.string.year_in_review_slide_app_icon_body_unlock, YearInReviewViewModel.YIR_YEAR, YearInReviewViewModel.YIR_YEAR + 1,
                    context.getString(R.string.editing_learn_more_url), context.getString(R.string.apps_about_wmf_url)),
                showDonateButton = true
            )
        } else null
    }

    private fun nonLoggedInEnglishGeneralSlides(): List<YearInReviewScreenData> {
        return (listOf(
            englishReadingHoursScreen(),
            popularEnglishArticlesScreen(),
            appSavedArticlesScreen()
        ) + editorRoutes() + unlockedIconRoute() + enWikiLoggedOutHighlightsScreen()).filterNotNull()
    }

    private fun nonLoggedInGeneralSlides(): List<YearInReviewScreenData> {
        return (listOf(
            availableLanguagesScreen(),
            viewedArticlesTimesScreen(),
            appSavedArticlesScreen()
        ) + editorRoutes() + unlockedIconRoute() + nonEnWikiLoggedOutHighlightsScreen()).filterNotNull()
    }

    private fun loggedInEnglishSlides(): List<YearInReviewScreenData> {
        return (listOf(
            spentReadingMinutesScreen(),
            popularEnglishArticlesScreen(),
            topArticlesScreen(),
            readingPatternsScreen(),
            topCategoriesScreen(),
            geoWithArticlesScreen(),
            localSavedArticlesScreen()
        ) + editorRoutes() + unlockedIconRoute() + loggedInHighlightScreen()).filterNotNull()
    }

    private fun loggedInGeneralSlides(): List<YearInReviewScreenData> {
        return (listOf(
            spentReadingMinutesScreen(),
            viewedArticlesTimesScreen(),
            topArticlesScreen(),
            readingPatternsScreen(),
            topCategoriesScreen(),
            geoWithArticlesScreen(),
            localSavedArticlesScreen()
        ) + editorRoutes() + unlockedIconRoute() + loggedInHighlightScreen()).filterNotNull()
    }

    private fun buildListWithNumbers(items: List<String>): String {
        var outputText = "<br /><br />"
        items.forEachIndexed { index, it ->
            outputText += "${index + 1}. $it<br />"
        }
        return outputText
    }

    fun finalSlides(): List<YearInReviewScreenData> {
        return when {
            isLoggedIn && isEnglishWiki -> {
                loggedInEnglishSlides()
            }

            isLoggedIn && !isEnglishWiki -> {
                loggedInGeneralSlides()
            }

            !isLoggedIn && isEnglishWiki -> {
                nonLoggedInEnglishGeneralSlides()
            }

            else -> nonLoggedInGeneralSlides()
        }
    }
}
