package org.wikipedia.yearinreview

import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil

object YearInReviewSlides {

    private val isEnglishWiki = WikipediaApp.instance.appOrSystemLanguageCode == "en"

    fun spentReadingHoursScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir123
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "We spent over 2 billion hours reading",
            bodyText = "TBD"
        )
    }

    fun spentReadingMinutesScreen(isEnglishWiki: Boolean, vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir99 + yir100 => need to check if it is en or not en
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "You spent 924 minutes reading 350 articles in 2025",
            bodyText = "TBD"
        )
    }

    fun popularArticlesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir127 + 104
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "English Wikipedia’s most popular articles",
            bodyText = "TBD"
        )
    }

    fun globalSavedArticlesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir126
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "We had over 37 million saved articles",
            bodyText = "TBD"
        )
    }

    fun availableLanguagesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir123
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "Wikipedia was available in more than 300 languages",
            bodyText = "TBD"
        )
    }

    fun viewedArticlesTimesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir125 + yir103
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "We have viewed Wikipedia articles more than 1 billion times",
            bodyText = "TBD"
        )
    }

    fun readingPatternsScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir106 + yir107
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "You have clear reading patterns",
            bodyText = "TBD"
        )
    }

    fun interestingCategoriesScreen(isEnglishWiki: Boolean, vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir108 + yir110 => confirm the difference.
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "Your most interesting categories",
            bodyText = "TBD"
        )
    }

    fun topArticlesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir109 + yir105
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "Your top articles",
            bodyText = "TBD"
        )
    }

    fun geoWithArticlesScreen(vararg params: Int): YearInReviewScreenData.GeoScreen {
        // TODO: yir112
        return YearInReviewScreenData.GeoScreen(
            coordinates = mapOf("lat" to listOf(34, 56), "lon" to listOf(-123, 45)),
            headlineText = "Articles you read are closest to France",
            bodyText = "TBD"
        )
    }

    fun localSavedArticlesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir113
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "You saved 25 articles",
            bodyText = "TBD"
        )
    }

    fun editedTimesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir114
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "You edited Wikipedia 150 times",
            bodyText = "TBD"
        )
    }

    fun editedViewsScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir115
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "Your edits have been viewed more than 14,791 times recently",
            bodyText = "TBD"
        )
    }

    fun editorsEditsScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir116
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "Editors on the official Wikipedia apps made more than 452,257 edits",
            bodyText = "TBD"
        )
    }

    fun editedPerMinuteScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir117
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "Wikipedia was edited 342 times per minute",
            bodyText = "TBD"
        )
    }

    fun editorsChangesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir118
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "Editors made nearly 82 million changes this year",
            bodyText = "TBD"
        )
    }

    fun addedBytesScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
        // TODO: yir119
        return YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_puzzle_pieces,
            staticImageResource = R.drawable.year_in_review_puzzle_pieces,
            headlineText = "Over 3 billion bytes added",
            bodyText = "TBD"
        )
    }

    fun newIconUnlockedScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
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

    fun unlockCustomIconScreen(vararg params: Int): YearInReviewScreenData.StandardScreen {
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

    fun highlightScreen(isLoggedIn: Boolean, isEnglishWiki: Boolean, vararg params: Int): YearInReviewScreenData.HighlightsScreen {
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
    fun editorRoutes(isEditor: Boolean): List<YearInReviewScreenData> {
        return when {
            isEditor -> listOf(
                editedTimesScreen(),
                editedViewsScreen()
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

    fun unlockedIconRoute(isIconUnlocked: Boolean): List<YearInReviewScreenData> {
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

    fun nonLoggedInEnglishGeneralSlides(): List<YearInReviewScreenData> {
        // TODO: Show a bunch of generic slides for English users - non-logged in.
        val isEditor = false // TODO: determine if the user is an editor
        val isIconUnlocked = false // TODO: determine if the user is an editor
        return listOf(
            spentReadingHoursScreen(1),
            popularArticlesScreen(),
            globalSavedArticlesScreen()
        ) + editorRoutes(isEditor) + unlockedIconRoute(isIconUnlocked) + highlightScreen(
            isLoggedIn = false,
            isEnglishWiki = true
        )
    }

    fun nonLoggedInGeneralSlides(): List<YearInReviewScreenData> {
        // TODO: Show a bunch of generic slides for non-English users - non-logged in.
        val isEditor = false // TODO: determine if the user is an editor
        val isIconUnlocked = false // TODO: determine if the user is an editor
        return listOf(
            availableLanguagesScreen(),
            viewedArticlesTimesScreen(),
            globalSavedArticlesScreen()
        ) + editorRoutes(isEditor) + unlockedIconRoute(isIconUnlocked) + highlightScreen(
            isLoggedIn = false,
            isEnglishWiki = false
        )
    }

    fun loggedInEnglishSlides(): List<YearInReviewScreenData> {
        // TODO: Show a bunch of generic slides for logged in English users.
        val isEditor = false // TODO: determine if the user is an editor
        val isIconUnlocked = false // TODO: determine if the user is an editor
        return listOf(
            spentReadingMinutesScreen(true),
            viewedArticlesTimesScreen(),
            readingPatternsScreen(),
            interestingCategoriesScreen(true),
            topArticlesScreen(),
            geoWithArticlesScreen(),
            localSavedArticlesScreen()
        ) + editorRoutes(isEditor) + unlockedIconRoute(isIconUnlocked) + highlightScreen(
            isLoggedIn = true,
            isEnglishWiki = true
        )
    }

    fun loggedInGeneralSlides(): List<YearInReviewScreenData> {
        // TODO: Show a bunch of generic slides for logged in users.
        val isEditor = false // TODO: determine if the user is an editor
        val isIconUnlocked = false // TODO: determine if the user is an editor
        return listOf(
            spentReadingMinutesScreen(false),
            popularArticlesScreen(),
            topArticlesScreen(),
            readingPatternsScreen(),
            interestingCategoriesScreen(false),
            geoWithArticlesScreen(),
            localSavedArticlesScreen()
        ) + editorRoutes(isEditor) + unlockedIconRoute(isIconUnlocked) + highlightScreen(
            isLoggedIn = true,
            isEnglishWiki = true
        )
    }

    // TODO: send all required data to this function
    fun finalSlides(): List<YearInReviewScreenData> {
        return when {
            AccountUtil.isLoggedIn && isEnglishWiki -> {
                loggedInEnglishSlides()
            }

            AccountUtil.isLoggedIn && !isEnglishWiki -> {
                loggedInGeneralSlides()
            }

            !AccountUtil.isLoggedIn && isEnglishWiki -> {
                nonLoggedInEnglishGeneralSlides()
            }

            else -> nonLoggedInGeneralSlides()
        }
    }
}
