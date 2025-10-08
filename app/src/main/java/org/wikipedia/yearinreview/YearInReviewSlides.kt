package org.wikipedia.yearinreview

import android.content.Context
import org.wikipedia.R
import org.wikipedia.settings.Prefs
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
    val yearInReviewModel: YearInReviewModel
) {

    private val formatter: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    private fun spentReadingHoursScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir123
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "We spent over 2 billion hours reading",
            bodyText = "TBD"
        )
    }

    private fun spentReadingMinutesScreen(isEnglishWiki: Boolean, vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir99 + yir100 => need to check if it is en or not en
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "You spent 924 minutes reading 350 articles in 2025",
            bodyText = "TBD"
        )
    }

    private fun popularEnglishArticlesScreen(): YearInReviewScreenData.StandardScreen {

        val popularEnglishArticlesText = buildListWithNumbers(yearInReviewModel.enPopularArticles)

        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.getString(R.string.year_in_review_slide_popular_english_articles_headline),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_popular_english_articles_body,
                yearInReviewModel.enPopularArticles.size, yearInReviewModel.enPopularArticles.size, popularEnglishArticlesText)
        )
    }

    private fun appSavedArticlesScreen(): YearInReviewScreenData.StandardScreen {
        val quantity = yearInReviewModel.appArticlesSavedTimes.toInt()
        val formattedNumber = formatter.format(yearInReviewModel.appArticlesSavedTimes)
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_global_saved_articles_headline, quantity, formattedNumber),
            bodyText = context.getString(R.string.year_in_review_slide_global_saved_articles_body)
        )
    }

    private fun availableLanguagesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir123
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "Wikipedia was available in more than 300 languages",
            bodyText = "TBD"
        )
    }

    private fun viewedArticlesTimesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir125 + yir103
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "We have viewed Wikipedia articles more than 1 billion times",
            bodyText = "TBD"
        )
    }

    private fun readingPatternsScreen(): YearInReviewScreenData.StandardScreen? {
        if (yearInReviewModel.localReadingArticlesCount < YearInReviewViewModel.MIN_READING_PATTERNS_ARTICLES) {
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
        return YearInReviewScreenData.ReadingPatterns(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.getString(R.string.year_in_review_slide_reading_patterns_headline),
            favoriteTimeText = favoriteTimeText,
            favoriteDayText = favoriteDayText,
            favoriteMonthText = favoriteMonthText
        )
    }

    private fun topCategoriesScreen(): YearInReviewScreenData.StandardScreen? {
        if (yearInReviewModel.localTopCategories.isEmpty() || yearInReviewModel.localTopCategories.size < YearInReviewViewModel.MIN_TOP_CATEGORY) {
            return null
        }

        val topCategoriesText = buildListWithNumbers(yearInReviewModel.localTopCategories)

        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.getString(R.string.year_in_review_slide_top_categories_headline),
            bodyText = context.getString(R.string.year_in_review_slide_top_categories_body, currentYear, topCategoriesText)
        )
    }

    private fun topArticlesScreen(): YearInReviewScreenData.StandardScreen? {
        if (yearInReviewModel.localTopVisitedArticles.isEmpty()) {
            return null
        }

        val topArticlesText = buildListWithNumbers(yearInReviewModel.localTopVisitedArticles)
        val quantity = yearInReviewModel.localTopVisitedArticles.size

        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_top_articles_headline, quantity),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_top_articles_body, quantity, currentYear, topArticlesText)
        )
    }

    private fun geoWithArticlesScreen(vararg params: Int): YearInReviewScreenData.GeoScreen {
        // TODO: yir112
        return YearInReviewScreenData.GeoScreen(
            coordinates = mapOf("lat" to listOf(34, 56), "lon" to listOf(-123, 45)),
            headlineText = "Articles you read are closest to France",
            bodyText = "TBD"
        )
    }

    private fun localSavedArticlesScreen(): YearInReviewScreenData.StandardScreen {
        val localSavedArticlesSize = yearInReviewModel.localSavedArticlesCount
        if (localSavedArticlesSize < YearInReviewViewModel.MIN_SAVED_ARTICLES) {
            return appSavedArticlesScreen()
        }
        val localSavedFormattedNumber = formatter.format(localSavedArticlesSize)
        val appSavedArticlesSize = yearInReviewModel.appArticlesSavedTimes.toInt()
        val appSavedFormattedNumber = formatter.format(yearInReviewModel.appArticlesSavedTimes)
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_saved_articles_headline, localSavedArticlesSize, localSavedFormattedNumber),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_saved_articles_body,
                appSavedArticlesSize, yearInReviewModel.localSavedArticles[0], yearInReviewModel.localSavedArticles[1],
                yearInReviewModel.localSavedArticles[2], appSavedFormattedNumber)
        )
    }

    private fun editedTimesScreen(): YearInReviewScreenData.StandardScreen {
        val userEditsCount = yearInReviewModel.userEditsCount
        var formattedUserEditsNumber = formatter.format(yearInReviewModel.userEditsCount)
        if (userEditsCount > YearInReviewViewModel.MAX_EDITED_TIMES) {
            formattedUserEditsNumber = "${YearInReviewViewModel.MAX_EDITED_TIMES}+"
        }
        val globalEditsCount = yearInReviewModel.globalEditsCount.toInt()
        val formattedGlobalEditsNumber = formatter.format(yearInReviewModel.globalEditsCount)
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edited_times_headline, userEditsCount, formattedUserEditsNumber),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edited_times_body, globalEditsCount, formattedGlobalEditsNumber)
        )
    }

    private fun editsViewedTimesScreen(): YearInReviewScreenData.StandardScreen {
        val quantity = yearInReviewModel.userEditsViewedTimes.toInt()
        val formattedNumber = formatter.format(yearInReviewModel.userEditsViewedTimes)
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edits_viewed_times_headline, quantity, formattedNumber),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edits_viewed_times_body, quantity, formattedNumber)
        )
    }

    private fun appEditedTimesScreen(): YearInReviewScreenData.StandardScreen {
        val quantity = yearInReviewModel.globalEditsCount.toInt()
        val formattedNumber = formatter.format(yearInReviewModel.globalEditsCount)
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_app_edited_times_headline, quantity, formattedNumber),
            bodyText = context.getString(R.string.year_in_review_slide_app_edited_times_body)
        )
    }

    private fun editedPerMinuteScreen(): YearInReviewScreenData.StandardScreen {
        val formattedNumber = formatter.format(yearInReviewModel.globalEditsPerMinute)
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edited_per_minute_headline, yearInReviewModel.globalEditsPerMinute, formattedNumber),
            bodyText = context.getString(R.string.year_in_review_slide_edited_per_minute_body, context.getString(R.string.editing_learn_more_url))
        )
    }

    private fun englishEditedTimesScreen(): YearInReviewScreenData.StandardScreen {
        val globalEditsCount = yearInReviewModel.globalEditsCount.toInt()
        val formattedGlobalEditsNumber = formatter.format(yearInReviewModel.globalEditsCount)
        val englishEditsCount = yearInReviewModel.enEditsCount.toInt()
        val formattedEnglishEditsNumber = formatter.format(yearInReviewModel.enEditsCount)
        val bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_english_edited_times_body_first,
            globalEditsCount, formattedGlobalEditsNumber, formattedEnglishEditsNumber) + " " +
                context.resources.getQuantityString(R.plurals.year_in_review_slide_english_edited_times_body_second,
                    englishEditsCount, formattedEnglishEditsNumber)
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_english_edited_times_headline, globalEditsCount, formattedGlobalEditsNumber),
            bodyText = bodyText
        )
    }

    private fun addedBytesScreen(): YearInReviewScreenData.StandardScreen {
        val quantity = yearInReviewModel.enBytesAddedCount.toInt()
        val formattedNumber = formatter.format(yearInReviewModel.enBytesAddedCount)
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_bytes_added_headline, quantity, formattedNumber),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_bytes_added_body,
                quantity, currentYear, formattedNumber, context.getString(R.string.editing_learn_more_url))
        )
    }

    private fun highlightScreen(vararg params: Int): YearInReviewScreenData.HighlightsScreen {
        // TODO: yir122
        return YearInReviewScreenData.HighlightsScreen(
            highlights = listOf(
                "You read 350 articles",
                "You saved 25 articles",
                "You edited Wikipedia 150 times"
            ),
            headlineText = "2025 highlights"
        )
    }

    // TODO: add parameters for numbers
    private fun editorRoutes(): List<YearInReviewScreenData> {
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

    private fun unlockedIconRoute(): List<YearInReviewScreenData> {
        val isIconUnlocked = yearInReviewModel.userEditsCount > 0 || Prefs.donationResults.isNotEmpty()
        return if (isIconUnlocked) {
            listOf(
                YearInReviewScreenData.CustomIconScreen(
                    headlineText = R.string.year_in_review_app_icon_title_unlocked,
                    bodyText = context.getString(R.string.year_in_review_app_icon_body_unlocked, YearInReviewViewModel.YIR_YEAR),
                )
            )
        } else {
            listOf(
                YearInReviewScreenData.CustomIconScreen(
                    headlineText = R.string.year_in_review_app_icon_title_unlock,
                    bodyText = context.getString(R.string.year_in_review_app_icon_body_unlock, YearInReviewViewModel.YIR_YEAR, YearInReviewViewModel.YIR_YEAR + 1,
                        context.getString(R.string.editing_learn_more_url), context.getString(R.string.apps_about_wmf_url)),
                    showDonateButton = true
                )
            )
        }
    }

    private fun nonLoggedInEnglishGeneralSlides(): List<YearInReviewScreenData> {
        // TODO: Show a bunch of generic slides for English users - non-logged in.
        return (listOf(
            spentReadingHoursScreen(1),
            popularEnglishArticlesScreen(),
            appSavedArticlesScreen()
        ) + editorRoutes() + unlockedIconRoute() + highlightScreen()).filterNotNull()
    }

    private fun nonLoggedInGeneralSlides(): List<YearInReviewScreenData> {
        // TODO: Show a bunch of generic slides for non-English users - non-logged in.
        return (listOf(
            availableLanguagesScreen(),
            viewedArticlesTimesScreen(),
            appSavedArticlesScreen()
        ) + editorRoutes() + unlockedIconRoute() + highlightScreen()).filterNotNull()
    }

    private fun loggedInEnglishSlides(): List<YearInReviewScreenData> {
        // TODO: Show a bunch of generic slides for logged in English users.
        return (listOf(
            spentReadingMinutesScreen(true),
            popularEnglishArticlesScreen(),
            readingPatternsScreen(),
            topCategoriesScreen(),
            topArticlesScreen(),
            geoWithArticlesScreen(),
            localSavedArticlesScreen()
        ) + editorRoutes() + unlockedIconRoute() + highlightScreen()).filterNotNull()
    }

    private fun loggedInGeneralSlides(): List<YearInReviewScreenData> {
        // TODO: Show a bunch of generic slides for logged in users.
        return (listOf(
            spentReadingMinutesScreen(false),
            viewedArticlesTimesScreen(),
            topArticlesScreen(),
            readingPatternsScreen(),
            topCategoriesScreen(),
            geoWithArticlesScreen(),
            localSavedArticlesScreen()
        ) + editorRoutes() + unlockedIconRoute() + highlightScreen()).filterNotNull()
    }

    private fun buildListWithNumbers(items: List<String>): String {
        var outputText = "<br />"
        items.forEachIndexed { index, it ->
            outputText += "${index + 1}. $it<br />"
        }
        return outputText
    }

    // TODO: send all required data to this function
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
