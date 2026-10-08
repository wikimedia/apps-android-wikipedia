package org.wikipedia.yearinreview.presentation

import org.wikipedia.history.db.MonthVisitedDays
import org.wikipedia.topics.ArticleTopic

data class YearInReviewArticle(
    val title: String,
    val description: String,
    val thumbnailUrl: String?
)

data class YearInReviewArticleViewCount(
    val title: String,
    val viewCount: Long,
    val thumbnailUrl: String?
)

// Declared in the order the collective flow shows them. Values come from the remote config.
enum class YearInReviewCollectiveInsight {
    HOURS_READ,
    LANGUAGES,
    ARTICLES,
    SAVED_ARTICLES,
    VOLUNTEER_EDITORS,
    EDITS,
    GLOBAL_REACH
}

sealed interface YearInReviewPage {
    val id: String
    val useDarkStatusBarIcons: Boolean get() = false

    data class Cover(val daysSpent: Int) : YearInReviewPage {
        override val id = "cover"
    }

    data class ArticlesRead(val isEmptyState: Boolean, val count: Int, val percentile: String, val averageCount: Int) : YearInReviewPage {
        override val id get() = if (isEmptyState) "total_articles_empty" else "total_articles"
    }

    data class Visits(val days: Int, val peakMonth: MonthVisitedDays?) : YearInReviewPage {
        override val id = "visits"
    }

    data class TimeSpent(val duration: Long) : YearInReviewPage {
        override val id = "time_spent"
    }

    data class ReadingStreak(val isEmptyState: Boolean, val longestStreak: Int, val streakStartDate: String, val streakEndDate: String) : YearInReviewPage {
        override val id get() = if (isEmptyState) "reading_streak_empty" else "reading_streak"
    }

    data class ReadingPattern(val isEmptyState: Boolean, val favoriteTime: String, val favoriteTimePercentage: Int) : YearInReviewPage {
        override val id get() = if (isEmptyState) "reading_pattern_empty" else "reading_pattern"
    }

    data class TopTopic(val isEmptyState: Boolean, val topic: ArticleTopic, val articles: List<String>) : YearInReviewPage {
        override val id get() = if (isEmptyState) "top_topic_empty" else "top_topic"
    }

    data class OtherTopTopics(val topicArticleCounts: Map<ArticleTopic, Int>) : YearInReviewPage {
        override val id = "other_top_topics"
    }

    data class BiggestReadingDay(val date: String, val minutesSpent: Int) : YearInReviewPage {
        override val id = "biggest_reading_day"
    }

    data class BiggestReadingDayArticles(val articles: List<YearInReviewArticle>) : YearInReviewPage {
        override val id = "biggest_reading_day_articles"
    }

    data object LongestReadGuess : YearInReviewPage {
        override val id = "longest_read_guess"
    }

    data object LongestReadReveal : YearInReviewPage {
        override val id = "longest_read_reveal"
    }

    data object RabbitHoleGuess : YearInReviewPage {
        override val id = "rabbit_hole_guess"
    }

    data object RabbitHoleReveal : YearInReviewPage {
        override val id = "rabbit_hole_reveal"
    }

    data class Category(val categoryName: String) : YearInReviewPage {
        override val id = "category"
    }

    data class RevisitedArticles(val isEmptyState: Boolean, val articles: List<YearInReviewArticle>) : YearInReviewPage {
        override val id get() = if (isEmptyState) "revisited_articles_empty" else "revisited_articles"
    }

    data class Geography(val isEmptyState: Boolean, val placeArticleCounts: Map<String, Int>) : YearInReviewPage {
        override val id get() = if (isEmptyState) "geography_empty" else "geography"
    }

    data class SavedArticles(val isEmptyState: Boolean, val savedCount: Int, val articles: List<YearInReviewArticleViewCount>) : YearInReviewPage {
        override val id get() = if (isEmptyState) "saved_articles_empty" else "saved_articles"
    }

    data class TotalEdits(val isEmptyState: Boolean, val editCount: Int) : YearInReviewPage {
        override val id get() = if (isEmptyState) "total_edits_empty" else "total_edits"
    }

    data class EditedArticleViews(val viewCount: Long) : YearInReviewPage {
        override val id = "edited_article_views"
    }

    data class MostViewedEditedArticles(val articles: List<YearInReviewArticleViewCount>) : YearInReviewPage {
        override val id = "most_viewed_edited_articles"
    }

    data class Collective(val insight: YearInReviewCollectiveInsight) : YearInReviewPage {
        override val id get() = "collective_" + insight.name.lowercase()
    }

    data class ThankYou(val isEditor: Boolean, val isDonor: Boolean) : YearInReviewPage {
        override val id = "thank_you"
    }

    data class YouMatter(val showLoginPrompt: Boolean) : YearInReviewPage {
        override val id = "you_matter"
    }

    data object End : YearInReviewPage {
        override val id = "end"
    }
}
