package org.wikipedia.settings.dev.playground

import org.wikipedia.history.db.MonthVisitedDays
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.presentation.YearInReviewArticle
import org.wikipedia.yearinreview.presentation.YearInReviewArticleViewCount
import org.wikipedia.yearinreview.presentation.YearInReviewPage

// Fixed decks that skip the flow decider, so every slide can be seen before its data is built.
// A new page type needs a line here, or it can't be previewed.
object YearInReviewPlaygroundPreview {
    private val averageArticlesReadPerYear = YearInReviewConfig.fallbackConfig.averageArticlesReadPerYear

    // this excludes LongestReadGuess, LongestReadReveal, RabbitHoleGuess, and RabbitHoleReveal which are not included in the template
    val allSlides get() = listOf(
        YearInReviewPage.Cover(daysSpent = 45),
        YearInReviewPage.ArticlesRead(isEmptyState = false, count = 120, percentile = "5", averageCount = averageArticlesReadPerYear),
        YearInReviewPage.Visits(days = 45, peakMonth = MonthVisitedDays(month = 9, visitedDays = 20)),
        YearInReviewPage.TimeSpent(duration = 1500),
        YearInReviewPage.ReadingStreak(isEmptyState = false, longestStreak = 21, streakStartDate = "March 4", streakEndDate = "24"),
        YearInReviewPage.ReadingPattern(isEmptyState = false, favoriteTime = "AFTERNOON", favoriteTimePercentage = 68),
        YearInReviewPage.TopTopic(
            isEmptyState = false,
            topic = ArticleTopics.all.first { it.topicId == "music" },
            articles = listOf("Abbey Road") + (1..15).map { "Music article $it" } + "Woodstock"
        ),
        YearInReviewPage.OtherTopTopics(
            topicArticleCounts = listOf(
                ArticleTopics.all[0] to 14,
                ArticleTopics.all[5] to 12,
                ArticleTopics.all[7] to 8
            ).takeRandomCount().toMap()
        ),
        YearInReviewPage.BiggestReadingDay(date = "September 30", minutesSpent = 423),
        YearInReviewPage.BiggestReadingDayArticles(
            articles = listOf(
                YearInReviewArticle(
                    title = "Association football",
                    description = "Team sport played with a ball",
                    thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/42/Football_in_Bloomington%2C_Indiana%2C_1995.jpg/120px-Football_in_Bloomington%2C_Indiana%2C_1995.jpg"
                ),
                YearInReviewArticle(
                    title = "Computer",
                    description = "Programmable machine that processes data",
                    thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0c/ENIAC-changing_a_tube_%28cropped%29.jpg/120px-ENIAC-changing_a_tube_%28cropped%29.jpg"
                ),
                YearInReviewArticle(
                    title = "Amazon rainforest",
                    description = "Large rainforest in South America",
                    thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/56/Amazon17_%285641020319%29.jpg/120px-Amazon17_%285641020319%29.jpg"
                )
            ).takeRandomCount()
        ),
        YearInReviewPage.Category(categoryName = "LIGHTHOUSES IN SCOTLAND"),
        YearInReviewPage.RevisitedArticles(
            isEmptyState = false,
            articles = listOf(
                YearInReviewArticle(
                    title = "Moon",
                    description = "Natural satellite orbiting Earth",
                    thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/FullMoon2010.jpg/120px-FullMoon2010.jpg"
                ),
                YearInReviewArticle(
                    title = "Leonardo da Vinci",
                    description = "Italian polymath (1452–1519)",
                    thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/16/Francesco_Melzi_-_Portrait_of_Leonardo_%28colour_correction%29.png/120px-Francesco_Melzi_-_Portrait_of_Leonardo_%28colour_correction%29.png"
                ),
                YearInReviewArticle(
                    title = "Python (programming language)",
                    description = "General-purpose programming language",
                    thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c3/Python-logo-notext.svg/120px-Python-logo-notext.svg.png"
                )
            )
        ),
        YearInReviewPage.Geography(isEmptyState = false, placeArticleCounts = mapOf("Brazil" to 12)),
        YearInReviewPage.SavedArticles(
            isEmptyState = false,
            savedCount = 26,
            articles = listOf(
                YearInReviewArticleViewCount(
                    title = "Mount Everest",
                    viewCount = 230,
                    thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/15/Mt._Everest_from_Gokyo_Ri_November_5%2C_2012.jpg/120px-Mt._Everest_from_Gokyo_Ri_November_5%2C_2012.jpg"
                ),
                YearInReviewArticleViewCount(
                    title = "Black hole",
                    viewCount = 200,
                    thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4f/Black_hole_-_Messier_87_crop_max_res.jpg/120px-Black_hole_-_Messier_87_crop_max_res.jpg"
                ),
                YearInReviewArticleViewCount(
                    title = "Sourdough",
                    viewCount = 185,
                    thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3b/Home_made_sour_dough_bread.jpg/120px-Home_made_sour_dough_bread.jpg"
                )
            )
        ),
        YearInReviewPage.TotalEdits(isEmptyState = false, editCount = 357),
        YearInReviewPage.EditedArticleViews(viewCount = 52145),
        YearInReviewPage.MostViewedEditedArticles(
            articles = listOf(
                YearInReviewArticleViewCount(
                    title = "Olympic Games",
                    viewCount = 12_480,
                    thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Olympic_rings_without_rims.svg/120px-Olympic_rings_without_rims.svg.png"
                ),
                YearInReviewArticleViewCount(
                    title = "Climate change",
                    viewCount = 8_312,
                    thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Change_in_Average_Temperature_With_Fahrenheit.svg/120px-Change_in_Average_Temperature_With_Fahrenheit.svg.png"
                ),
                YearInReviewArticleViewCount(
                    title = "Honey bee",
                    viewCount = 1_509,
                    thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/11/The_Lone_Pollinator.jpg/120px-The_Lone_Pollinator.jpg"
                )
            )
        ),
        YearInReviewPage.ThankYou(isEditor = true, isDonor = true),
        YearInReviewPage.YouMatter(showLoginPrompt = false),
        YearInReviewPage.End
    )

    // Visits has no empty state: it's left out of the flow instead
    val emptyStates = listOf(
        YearInReviewPage.Cover(daysSpent = 12),
        YearInReviewPage.ArticlesRead(isEmptyState = true, count = 0, percentile = "", averageCount = averageArticlesReadPerYear),
        YearInReviewPage.ReadingStreak(isEmptyState = true, longestStreak = 0, streakStartDate = "", streakEndDate = ""),
        YearInReviewPage.ReadingPattern(isEmptyState = true, favoriteTime = "", favoriteTimePercentage = 0),
        YearInReviewPage.TopTopic(isEmptyState = true, topic = ArticleTopics.all.first(), articles = emptyList()),
        YearInReviewPage.RevisitedArticles(isEmptyState = true, articles = emptyList()),
        YearInReviewPage.Geography(isEmptyState = true, placeArticleCounts = emptyMap()),
        YearInReviewPage.SavedArticles(isEmptyState = true, savedCount = 0, articles = emptyList()),
        YearInReviewPage.TotalEdits(isEmptyState = true, editCount = 0),
        YearInReviewPage.End
    )

    // Between 1 and all of the items, so slides with a variable number of rows can be checked
    private fun <T> List<T>.takeRandomCount() = take((1..size).random())
}
