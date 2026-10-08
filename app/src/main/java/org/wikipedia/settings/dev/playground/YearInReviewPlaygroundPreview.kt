package org.wikipedia.settings.dev.playground

import org.wikipedia.history.db.MonthVisitedDays
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.presentation.YearInReviewPage

// Fixed decks that skip the flow decider, so every slide can be seen before its data is built.
// A new page type needs a line here, or it can't be previewed.
object YearInReviewPlaygroundPreview {
    private val averageArticlesReadPerYear = YearInReviewConfig.fallbackConfig.averageArticlesReadPerYear

    val allSlides = listOf(
        YearInReviewPage.Cover(daysSpent = 45),
        YearInReviewPage.ArticlesRead(isEmptyState = false, count = 120, percentile = "5", averageCount = averageArticlesReadPerYear),
        YearInReviewPage.Visits(days = 45, peakMonth = MonthVisitedDays(month = 9, visitedDays = 20)),
        YearInReviewPage.TimeSpent(duration = 1500),
        YearInReviewPage.ReadingStreak(isEmptyState = false),
        YearInReviewPage.ReadingPattern(isEmptyState = false),
        YearInReviewPage.TopTopic(isEmptyState = false),
        YearInReviewPage.OtherTopTopics,
        YearInReviewPage.BiggestReadingDay,
        YearInReviewPage.BiggestReadingDayArticles,
        YearInReviewPage.LongestReadGuess,
        YearInReviewPage.LongestReadReveal,
        YearInReviewPage.RabbitHoleGuess,
        YearInReviewPage.RabbitHoleReveal,
        YearInReviewPage.Category,
        YearInReviewPage.RevisitedArticles(isEmptyState = false),
        YearInReviewPage.Geography(isEmptyState = false),
        YearInReviewPage.SavedArticles(isEmptyState = false),
        YearInReviewPage.TotalEdits(isEmptyState = false),
        YearInReviewPage.EditedArticleViews,
        YearInReviewPage.MostViewedEditedArticles,
        YearInReviewPage.ThankYou(isEditor = true, isDonor = true),
        YearInReviewPage.YouMatter(showLoginPrompt = false),
        YearInReviewPage.End
    )

    // Visits has no empty state: it's left out of the flow instead
    val emptyStates = listOf(
        YearInReviewPage.Cover(daysSpent = 12),
        YearInReviewPage.ArticlesRead(isEmptyState = true, count = 0, percentile = "", averageCount = averageArticlesReadPerYear),
        YearInReviewPage.ReadingStreak(isEmptyState = true),
        YearInReviewPage.ReadingPattern(isEmptyState = true),
        YearInReviewPage.TopTopic(isEmptyState = true),
        YearInReviewPage.RevisitedArticles(isEmptyState = true),
        YearInReviewPage.Geography(isEmptyState = true),
        YearInReviewPage.SavedArticles(isEmptyState = true),
        YearInReviewPage.TotalEdits(isEmptyState = true),
        YearInReviewPage.End
    )
}
