package org.wikipedia.yearinreview.presentation

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

    data class Visits(val days: Int, val peakMonth: Int, val peakMonthVisitedDays: Int) : YearInReviewPage {
        override val id = "visits"
    }

    data class TimeSpent(val duration: Long) : YearInReviewPage {
        override val id = "time_spent"
    }

    data class ReadingStreak(val isEmptyState: Boolean) : YearInReviewPage {
        override val id get() = if (isEmptyState) "reading_streak_empty" else "reading_streak"
    }

    data class ReadingPattern(val isEmptyState: Boolean) : YearInReviewPage {
        override val id get() = if (isEmptyState) "reading_pattern_empty" else "reading_pattern"
    }

    data class TopTopic(val isEmptyState: Boolean) : YearInReviewPage {
        override val id get() = if (isEmptyState) "top_topic_empty" else "top_topic"
    }

    data object OtherTopTopics : YearInReviewPage {
        override val id = "other_top_topics"
    }

    data object BiggestReadingDay : YearInReviewPage {
        override val id = "biggest_reading_day"
    }

    data object BiggestReadingDayArticles : YearInReviewPage {
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

    data object Category : YearInReviewPage {
        override val id = "category"
    }

    data class RevisitedArticles(val isEmptyState: Boolean) : YearInReviewPage {
        override val id get() = if (isEmptyState) "revisited_articles_empty" else "revisited_articles"
    }

    data class Geography(val isEmptyState: Boolean) : YearInReviewPage {
        override val id get() = if (isEmptyState) "geography_empty" else "geography"
    }

    data class SavedArticles(val isEmptyState: Boolean) : YearInReviewPage {
        override val id get() = if (isEmptyState) "saved_articles_empty" else "saved_articles"
    }

    data class TotalEdits(val isEmptyState: Boolean) : YearInReviewPage {
        override val id get() = if (isEmptyState) "total_edits_empty" else "total_edits"
    }

    data object EditedArticleViews : YearInReviewPage {
        override val id = "edited_article_views"
    }

    data object MostViewedEditedArticles : YearInReviewPage {
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
