package org.wikipedia.settings.dev.playground

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.db.MonthVisitedDays
import org.wikipedia.page.PageTitle
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.data.YearInReviewTimeOfDay
import org.wikipedia.yearinreview.presentation.YearInReviewPage

// Fixed decks that skip the flow decider, so every slide can be seen before its data is built.
// A new page type needs a line here, or it can't be previewed.
object YearInReviewPlaygroundPreview {
    private val averageArticlesReadPerYear = YearInReviewConfig.fallbackConfig.averageArticlesReadPerYear

    // Shared with the Personalized test data, so its Top topic and Revisited articles slides aren't empty
    val sampleTopTopic = ArticleTopics.all.first { it.topicId == "music" }
    val sampleTopTopicArticles = listOf("Abbey Road") + (1..15).map { "Music article $it" } + "Woodstock"
    val sampleArticles = listOf(
        PageTitle(
            text = "Moon",
            wiki = WikiSite.preview(),
            thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/FullMoon2010.jpg/120px-FullMoon2010.jpg",
            description = "Natural satellite orbiting Earth",
            displayText = "Moon"
        ),
        PageTitle(
            text = "Leonardo da Vinci",
            wiki = WikiSite.preview(),
            thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/16/Francesco_Melzi_-_Portrait_of_Leonardo_%28colour_correction%29.png/120px-Francesco_Melzi_-_Portrait_of_Leonardo_%28colour_correction%29.png",
            description = "Italian polymath (1452–1519)",
            displayText = "Leonardo da Vinci"
        ),
        PageTitle(
            text = "Python (programming language)",
            wiki = WikiSite.preview(),
            thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c3/Python-logo-notext.svg/120px-Python-logo-notext.svg.png",
            description = "General-purpose programming language",
            displayText = "Python (programming language)"
        )
    )

    // this excludes LongestReadGuess and RabbitHoleGuess which are not included in the template
    val allSlides get() = listOf(
        YearInReviewPage.ArticlesRead(isEmptyState = false, count = 120, percentile = "5", averageCount = averageArticlesReadPerYear),
        YearInReviewPage.Visits(days = 45, peakMonth = MonthVisitedDays(month = 9, visitedDays = 20)),
        YearInReviewPage.TimeSpent(duration = 1500),
        YearInReviewPage.ReadingStreak(isEmptyState = false, longestStreak = 21, streakStartDate = "March 4", streakEndDate = "24"),
        YearInReviewPage.ReadingPattern(isEmptyState = false, favoriteTime = YearInReviewTimeOfDay.AFTERNOON, favoriteTimePercentage = 68),
        YearInReviewPage.TopTopic(isEmptyState = false, topic = sampleTopTopic, totalArticles = 123, sampleArticles = sampleTopTopicArticles),
        YearInReviewPage.OtherTopTopics(
            topicArticleCounts = listOf(
                ArticleTopics.all[0].topicId to 14,
                ArticleTopics.all[5].topicId to 12,
                ArticleTopics.all[7].topicId to 8
            ).takeRandomCount().toMap()
        ),
        YearInReviewPage.BiggestReadingDay(date = "September 30", minutesSpent = 423),
        YearInReviewPage.BiggestReadingDayArticles(
            articles = listOf(
                PageTitle(
                    text = "Association football",
                    wiki = WikiSite.preview(),
                    thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/42/Football_in_Bloomington%2C_Indiana%2C_1995.jpg/120px-Football_in_Bloomington%2C_Indiana%2C_1995.jpg",
                    description = "Team sport played with a ball",
                    displayText = "Association football"
                ),
                PageTitle(
                    text = "Computer",
                    wiki = WikiSite.preview(),
                    thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0c/ENIAC-changing_a_tube_%28cropped%29.jpg/120px-ENIAC-changing_a_tube_%28cropped%29.jpg",
                    description = "Programmable machine that processes data",
                    displayText = "Computer"
                ),
                PageTitle(
                    text = "Amazon rainforest",
                    wiki = WikiSite.preview(),
                    thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/56/Amazon17_%285641020319%29.jpg/120px-Amazon17_%285641020319%29.jpg",
                    description = "Large rainforest in South America",
                    displayText = "Amazon rainforest"
                )
            ).takeRandomCount()
        ),
        YearInReviewPage.Category(categoryName = "LIGHTHOUSES IN SCOTLAND"),
        YearInReviewPage.RevisitedArticles(isEmptyState = false, articles = sampleArticles.map { it to 5 }),
        YearInReviewPage.Geography(isEmptyState = false, countryCode = "BR", countryName = "Brazil", articleCount = 12),
        YearInReviewPage.SavedArticles(
            isEmptyState = false,
            savedCount = 26,
            articles = listOf(
                PageTitle(
                    text = "Mount Everest",
                    wiki = WikiSite.preview(),
                    thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/15/Mt._Everest_from_Gokyo_Ri_November_5%2C_2012.jpg/120px-Mt._Everest_from_Gokyo_Ri_November_5%2C_2012.jpg",
                    description = "Highest mountain in the world",
                    displayText = "Mount Everest"
                ),
                PageTitle(
                    text = "Black hole",
                    wiki = WikiSite.preview(),
                    thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4f/Black_hole_-_Messier_87_crop_max_res.jpg/120px-Black_hole_-_Messier_87_crop_max_res.jpg",
                    description = "Region of space having a gravitational field so intense that no matter or radiation can escape",
                    displayText = "Black hole"
                ),
                PageTitle(
                    text = "Sourdough",
                    wiki = WikiSite.preview(),
                    thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3b/Home_made_sour_dough_bread.jpg/120px-Home_made_sour_dough_bread.jpg",
                    description = "Type of bread",
                    displayText = "Sourdough"
                )
            )
        ),
        YearInReviewPage.TotalEdits(isEmptyState = false, editCount = 357),
        YearInReviewPage.EditedArticleViews(viewCount = 52145),
        YearInReviewPage.MostViewedEditedArticles(articles = sampleArticles.map { it to 100000 }),
        YearInReviewPage.ThankYou(isEditor = true, isDonor = true),
        YearInReviewPage.YouMatter(showLoginPrompt = false),
        YearInReviewPage.End(isDonor = true)
    )

    // Visits has no empty state: it's left out of the flow instead
    val emptyStates = listOf(
        YearInReviewPage.ArticlesRead(isEmptyState = true, count = 0, percentile = "", averageCount = averageArticlesReadPerYear),
        YearInReviewPage.ReadingStreak(isEmptyState = true, longestStreak = 0, streakStartDate = "", streakEndDate = ""),
        YearInReviewPage.ReadingPattern(isEmptyState = true, favoriteTime = null, favoriteTimePercentage = 0),
        YearInReviewPage.TopTopic(isEmptyState = true, topic = ArticleTopics.all.first(), totalArticles = 0, sampleArticles = emptyList()),
        YearInReviewPage.RevisitedArticles(isEmptyState = true, articles = emptyList()),
        YearInReviewPage.Geography(isEmptyState = true, countryCode = null, countryName = null, articleCount = 0),
        YearInReviewPage.SavedArticles(isEmptyState = true, savedCount = 0, articles = emptyList()),
        YearInReviewPage.TotalEdits(isEmptyState = true, editCount = 0),
        YearInReviewPage.End(isDonor = false)
    )

    // Between 1 and all of the items, so slides with a variable number of rows can be checked
    private fun <T> List<T>.takeRandomCount() = take((1..size).random())
}
