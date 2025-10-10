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

    private fun englishReadingHoursScreen(): YearInReviewScreenData.StandardScreen {
        val hoursQuantity = yearInReviewModel.enReadingTimePerHour.toInt()
        val yearsQuantity = (hoursQuantity / 8760)
        val bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_english_reading_hours_body_first,
            hoursQuantity, yearInReviewModel.enReadingTimePerHour) + " " +
                context.resources.getQuantityString(R.plurals.year_in_review_slide_english_reading_hours_body_second,
                    yearsQuantity, yearsQuantity, currentYear)
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_english_reading_hours_headline, hoursQuantity, yearInReviewModel.enReadingTimePerHour),
            bodyText = bodyText
        )
    }

    private fun spentReadingMinutesScreen(): YearInReviewScreenData.StandardScreen {
        if (yearInReviewModel.localReadingArticlesCount < YearInReviewViewModel.MIN_READING_ARTICLES ||
            yearInReviewModel.localReadingTimePerMinute < YearInReviewViewModel.MIN_READING_MINUTES) {
            return if (isEnglishWiki) {
                englishReadingHoursScreen()
            } else {
                availableLanguagesScreen()
            }
        }
        val minutesQuantity = yearInReviewModel.localReadingTimePerMinute.toInt()
        val hoursQuantity = yearInReviewModel.enReadingTimePerHour.toInt()
        val yearsQuantity = (hoursQuantity / 8760)
        val availableLanguages = yearInReviewModel.availableLanguages
        val articlesQuantity = yearInReviewModel.globalTotalArticles.toInt()

        val headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_headline_first,
            minutesQuantity, yearInReviewModel.localReadingTimePerMinute) + " " +
                context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_headline_second,
                    yearInReviewModel.localReadingArticlesCount, yearInReviewModel.localReadingArticlesCount, currentYear)

        // build reading rank
        var rankingText = when (yearInReviewModel.localReadingArticlesCount) {
            in 336..1233 -> "50%"
            in 1234..2455 -> "40%"
            in 2456..4566 -> "30%"
            in 4567..8900 -> "20%"
            in 8901..12344 -> "10%"
            in 12345..23455 -> "5%"
            in 23456..43739 -> "1%"
            in 43740..Int.MAX_VALUE -> "0.01%"
            else -> null
        }
        var bodyText = ""
        rankingText?.let {
            rankingText = "<b>$it</b>"
            bodyText += context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_body_top,
                yearInReviewModel.globalAverageReadingArticlesCount, rankingText, yearInReviewModel.globalAverageReadingArticlesCount) + "<br /><br />"
        }

        bodyText += if (isEnglishWiki) {
            context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_body_english_first,
                hoursQuantity, yearInReviewModel.enReadingTimePerHour) + " " +
                    context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_body_english_second,
                        yearsQuantity, yearsQuantity, currentYear)
        } else {
            context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_body_global_first,
                articlesQuantity, yearInReviewModel.globalTotalArticles) + " " +
                    context.resources.getQuantityString(R.plurals.year_in_review_slide_spent_minutes_reading_body_global_second,
                        availableLanguages, availableLanguages, currentYear)
        }

        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = headlineText,
            bodyText = bodyText
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
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_global_saved_articles_headline, quantity, yearInReviewModel.appArticlesSavedTimes),
            bodyText = context.getString(R.string.year_in_review_slide_global_saved_articles_body)
        )
    }

    private fun availableLanguagesScreen(): YearInReviewScreenData.StandardScreen {
        val availableLanguages = yearInReviewModel.availableLanguages
        val articlesQuantity = yearInReviewModel.globalTotalArticles.toInt()
        val bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_available_languages_body_first,
            articlesQuantity, yearInReviewModel.globalTotalArticles) + " " +
                context.resources.getQuantityString(R.plurals.year_in_review_slide_available_languages_body_second,
                    availableLanguages, availableLanguages, currentYear)
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_available_languages_headline, availableLanguages, availableLanguages),
            bodyText = bodyText
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
        if (yearInReviewModel.localSavedArticlesCount < YearInReviewViewModel.MIN_SAVED_ARTICLES) {
            return appSavedArticlesScreen()
        }
        val appSavedArticlesSize = yearInReviewModel.appArticlesSavedTimes.toInt()
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_saved_articles_headline, yearInReviewModel.localSavedArticlesCount, yearInReviewModel.localSavedArticlesCount),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_saved_articles_body,
                appSavedArticlesSize, yearInReviewModel.localSavedArticles[0], yearInReviewModel.localSavedArticles[1],
                yearInReviewModel.localSavedArticles[2], yearInReviewModel.appArticlesSavedTimes)
        )
    }

    private fun editedTimesScreen(): YearInReviewScreenData.StandardScreen {
        val userEditsCount = yearInReviewModel.userEditsCount
        var formattedUserEditsNumber = NumberFormat.getNumberInstance(Locale.getDefault()).format(yearInReviewModel.userEditsCount)
        if (userEditsCount > YearInReviewViewModel.MAX_EDITED_TIMES) {
            formattedUserEditsNumber = "${YearInReviewViewModel.MAX_EDITED_TIMES}+"
        }
        val globalEditsCount = yearInReviewModel.globalEditsCount.toInt()
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edited_times_headline, userEditsCount, formattedUserEditsNumber),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edited_times_body, globalEditsCount, yearInReviewModel.globalEditsCount)
        )
    }

    private fun editsViewedTimesScreen(): YearInReviewScreenData.StandardScreen {
        val quantity = yearInReviewModel.userEditsViewedTimes.toInt()
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edits_viewed_times_headline, quantity, yearInReviewModel.userEditsViewedTimes),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edits_viewed_times_body, quantity, yearInReviewModel.userEditsViewedTimes)
        )
    }

    private fun appEditedTimesScreen(): YearInReviewScreenData.StandardScreen {
        val quantity = yearInReviewModel.globalEditsCount.toInt()
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_app_edited_times_headline, quantity, yearInReviewModel.globalEditsCount),
            bodyText = context.getString(R.string.year_in_review_slide_app_edited_times_body)
        )
    }

    private fun editedPerMinuteScreen(): YearInReviewScreenData.StandardScreen {
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_edited_per_minute_headline, yearInReviewModel.globalEditsPerMinute, yearInReviewModel.globalEditsPerMinute),
            bodyText = context.getString(R.string.year_in_review_slide_edited_per_minute_body, context.getString(R.string.editing_learn_more_url))
        )
    }

    private fun englishEditedTimesScreen(): YearInReviewScreenData.StandardScreen {
        val globalEditsCount = yearInReviewModel.globalEditsCount.toInt()
        val englishEditsCount = yearInReviewModel.enEditsCount.toInt()
        val bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_english_edited_times_body_first,
            globalEditsCount, yearInReviewModel.globalEditsCount, yearInReviewModel.enEditsCount) + " " +
                context.resources.getQuantityString(R.plurals.year_in_review_slide_english_edited_times_body_second,
                    englishEditsCount, yearInReviewModel.enEditsCount)
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_english_edited_times_headline, globalEditsCount, yearInReviewModel.globalEditsCount),
            bodyText = bodyText
        )
    }

    private fun addedBytesScreen(): YearInReviewScreenData.StandardScreen {
        val quantity = yearInReviewModel.enBytesAddedCount.toInt()
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_bytes_added_headline, quantity, yearInReviewModel.enBytesAddedCount),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_bytes_added_body,
                quantity, currentYear, yearInReviewModel.enBytesAddedCount, context.getString(R.string.editing_learn_more_url))
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
            englishReadingHoursScreen(),
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
            spentReadingMinutesScreen(),
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
            spentReadingMinutesScreen(),
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
