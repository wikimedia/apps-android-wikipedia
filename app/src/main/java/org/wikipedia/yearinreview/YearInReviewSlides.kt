package org.wikipedia.yearinreview

import android.content.Context
import org.wikipedia.R
import java.text.NumberFormat
import java.util.Locale

class YearInReviewSlides(
    val context: Context,
    val currentYear: Int,
    val isEditor: Boolean,
    val isLoggedIn: Boolean,
    val isEnglishWiki: Boolean,
    val isIconUnlocked: Boolean,
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

    private fun readingPatternsScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir106 + yir107
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "You have clear reading patterns",
            bodyText = "TBD"
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
        val localSavedArticlesSize = yearInReviewModel.localSavedArticles.size
        if (localSavedArticlesSize < YearInReviewViewModel.MIN_SAVED_ARTICLES) {
            return appSavedArticlesScreen()
        }
        val localSavedFormattedNumber = formatter.format(yearInReviewModel.localSavedArticles.size)
        val appSavedArticlesSize = yearInReviewModel.appArticlesSavedTimes.toInt()
        val appSavedFormattedNumber = formatter.format(yearInReviewModel.appArticlesSavedTimes)
        val randomArticles = yearInReviewModel.localSavedArticles.shuffled().take(YearInReviewViewModel.MIN_SAVED_ARTICLES)
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            staticImageResource = R.drawable.year_in_review_puzzle_pieces, // TODO: tbd
            headlineText = context.resources.getQuantityString(R.plurals.year_in_review_slide_saved_articles_headline, localSavedArticlesSize, localSavedFormattedNumber),
            bodyText = context.resources.getQuantityString(R.plurals.year_in_review_slide_saved_articles_body,
                appSavedArticlesSize, randomArticles[0], randomArticles[1], randomArticles[2], appSavedFormattedNumber)
        )
    }

    private fun editedTimesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir114
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "You edited Wikipedia 150 times",
            bodyText = "TBD"
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

    private fun editorsEditsScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir116
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "Editors on the official Wikipedia apps made more than 452,257 edits",
            bodyText = "TBD"
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

    private fun editorsChangesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir118
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "Editors made nearly 82 million changes this year",
            bodyText = "TBD"
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

    private fun newIconUnlockedScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir121
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "New icon unlocked",
            bodyText = "TBD",
            unlockIcon = UnlockIconConfig(
                isUnlocked = true
            )
        )
    }

    private fun unlockCustomIconScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir122
        return YearInReviewScreenData.StandardScreen(
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
                    editorsEditsScreen(),
                    editedPerMinuteScreen()
                )
            }
            else -> {
                listOf(
                    editorsChangesScreen(),
                    addedBytesScreen()
                )
            }
        }
    }

    private fun unlockedIconRoute(): List<YearInReviewScreenData> {
        return if (isIconUnlocked) {
            listOf(
                newIconUnlockedScreen()
            )
        } else {
            listOf(
                unlockCustomIconScreen()
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
