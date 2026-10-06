package org.wikipedia.yearinreview.presentation

import android.content.res.Resources
import org.wikipedia.R
import org.wikipedia.yearinreview.data.YearInReviewConfig
import java.time.Month
import java.time.format.TextStyle
import java.util.concurrent.TimeUnit

/**
 * Maps each page to its Rive content, or null when the page doesn't use Rive.
 * A custom Rive slide still returns its content here, so its file gets loaded, and gets its own branch in the pager.
 */
object YearInReviewRiveContentMapper {
    // TODO: the text properties are only for test, should be replaced with actual values later
    fun map(page: YearInReviewPage, resources: Resources): RiveSlideContent? = when (page) {
        is YearInReviewPage.Cover -> RiveSlideContent(
            spec = allTemplatesSlideSpec("cover"),
            textProperties = mapOf(
                "coverTitle" to resources.getString(R.string.yir_intro_headline),
                "bodyCopy" to resources.getQuantityString(R.plurals.yir_intro_supporting, page.daysSpent, page.daysSpent, YearInReviewConfig.YEAR)
            )
        )
        is YearInReviewPage.ArticlesRead -> RiveSlideContent(
            spec = allTemplatesSlideSpec(if (page.isEmptyState) "frame1-empty" else "frame1"),
            textProperties = mapOf(
                "headline" to resources.getString(if (page.isEmptyState) R.string.yir_article_count_empty_headline else R.string.yir_article_count_headline),
                "bodyCopy" to if (page.isEmptyState) resources.getString(R.string.yir_article_count_empty_supporting) else
                    resources.getQuantityString(R.plurals.yir_article_count_supporting_top, page.count, page.percentile, page.averageCount),
                "data" to page.count.toString()
            )
        )
        is YearInReviewPage.Visits -> RiveSlideContent(
            spec = allTemplatesSlideSpec("frame2"),
            textProperties = mapOf(
                "Headline" to resources.getString(R.string.yir_days_visited_headline, YearInReviewConfig.YEAR),
                "bodyCopy" to resources.getQuantityString(
                    R.plurals.yir_days_visited_supporting,
                    page.peakMonthVisitedDays,
                    Month.of(page.peakMonth).getDisplayName(TextStyle.FULL_STANDALONE, resources.configuration.locales[0]),
                    page.peakMonthVisitedDays
                ),
                "Data" to page.days.toString()
            )
        )
        is YearInReviewPage.TimeSpent -> RiveSlideContent(
            spec = allTemplatesSlideSpec("frame3"),
            textProperties = mapOf(
                "Headline" to resources.getString(R.string.yir_minutes_read_headline),
                "data" to page.duration.toString(),
                "bodyCopy" to resources.getString(R.string.yir_minutes_read_supporting, getTimeSpentComment(resources, page.duration))
            )
        )
        is YearInReviewPage.ReadingStreak -> RiveSlideContent(allTemplatesSlideSpec(if (page.isEmptyState) "frame4-empty" else "frame4"))
        is YearInReviewPage.ReadingPattern -> RiveSlideContent(allTemplatesSlideSpec(if (page.isEmptyState) "frame5-empty" else "frame5"))
        is YearInReviewPage.TopTopic -> RiveSlideContent(allTemplatesSlideSpec(if (page.isEmptyState) "frame6-empty" else "frame6"))
        is YearInReviewPage.OtherTopTopics -> RiveSlideContent(allTemplatesListSlideSpec("frame7"))
        is YearInReviewPage.BiggestReadingDay -> RiveSlideContent(allTemplatesSlideSpec("frame8"))
        is YearInReviewPage.BiggestReadingDayArticles -> RiveSlideContent(allTemplatesListSlideSpec("frame9"))
        is YearInReviewPage.Category -> RiveSlideContent(allTemplatesSlideSpec("frame13"))
        is YearInReviewPage.RevisitedArticles -> RiveSlideContent(allTemplatesListSlideSpec(if (page.isEmptyState) "frame12-empty" else "frame12"))
        is YearInReviewPage.Geography -> RiveSlideContent(allTemplatesListSlideSpec(if (page.isEmptyState) "frame14-empty" else "frame14"))
        is YearInReviewPage.SavedArticles -> RiveSlideContent(allTemplatesListSlideSpec(if (page.isEmptyState) "frame15-empty" else "frame15"))
        is YearInReviewPage.TotalEdits -> RiveSlideContent(allTemplatesSlideSpec(if (page.isEmptyState) "frame16-empty" else "frame16"))
        is YearInReviewPage.EditedArticleViews -> RiveSlideContent(allTemplatesSlideSpec("frame17"))
        is YearInReviewPage.MostViewedEditedArticles -> RiveSlideContent(allTemplatesListSlideSpec("frame18"))
        is YearInReviewPage.ThankYou -> RiveSlideContent(allTemplatesSlideSpec("end"))
        // The file has no collective artboards yet, so these reuse the personal artboard closest to each insight
        is YearInReviewPage.Collective -> when (page.insight) {
            YearInReviewCollectiveInsight.HOURS_READ -> RiveSlideContent(allTemplatesSlideSpec("frame3"))
            YearInReviewCollectiveInsight.LANGUAGES -> RiveSlideContent(allTemplatesSlideSpec("frame2"))
            YearInReviewCollectiveInsight.ARTICLES -> RiveSlideContent(allTemplatesSlideSpec("frame1"))
            YearInReviewCollectiveInsight.SAVED_ARTICLES -> RiveSlideContent(allTemplatesListSlideSpec("frame15"))
            YearInReviewCollectiveInsight.VOLUNTEER_EDITORS -> RiveSlideContent(allTemplatesSlideSpec("frame17"))
            YearInReviewCollectiveInsight.EDITS -> RiveSlideContent(allTemplatesSlideSpec("frame16"))
            YearInReviewCollectiveInsight.GLOBAL_REACH -> RiveSlideContent(allTemplatesListSlideSpec("frame14"))
        }
        is YearInReviewPage.LongestReadGuess,
        is YearInReviewPage.LongestReadReveal,
        is YearInReviewPage.RabbitHoleGuess,
        is YearInReviewPage.RabbitHoleReveal,
        is YearInReviewPage.YouMatter,
        is YearInReviewPage.End -> null
    }
}

private val AllTemplatesGlobalProperties = RiveGlobalViewModel(
    name = "GlobalProperties",
    textSizes = mapOf(
        "headlineFontSize" to 24f,
        "headlineLineHeight" to 25.44f,
        "bodyCopyFontSize" to 16f,
        "bodyCopyLineHeight" to 20f
    )
)

// In all_templates.riv, each artboard has a matching state machine and a same-named view model instance holding sample text
private fun allTemplatesSlideSpec(artboardName: String, viewModelName: String = "DataTemplate") = RiveSlideSpec(
    resourceId = R.raw.all_templates_v2,
    artboardName = artboardName,
    stateMachineName = "$artboardName-statemachine",
    viewModelName = viewModelName,
    instanceType = RiveInstanceType.Named(artboardName),
    globalViewModel = AllTemplatesGlobalProperties,
    isTopBarUiIconsWhite = "isUIWhite"
)

// Artboards that show up to three articles or topics, each with an icon, title and subtitle
private fun allTemplatesListSlideSpec(artboardName: String) = allTemplatesSlideSpec(artboardName, viewModelName = "List")

private fun getTimeSpentComment(resources: Resources, minutes: Long): String {
    return when {
        minutes >= 2800 -> resources.getQuantityString(R.plurals.yir_time_spent_comment_2800, TimeUnit.MINUTES.toDays(minutes).toInt())
        minutes >= 1441 -> resources.getString(R.string.yir_time_spent_comment_1441)
        minutes >= 540 -> resources.getString(R.string.yir_time_spent_comment_540)
        minutes >= 121 -> resources.getString(R.string.yir_time_spent_comment_121)
        minutes >= 93 -> resources.getString(R.string.yir_time_spent_comment_93)
        minutes >= 31 -> resources.getString(R.string.yir_time_spent_comment_31)
        minutes >= 19 -> resources.getString(R.string.yir_time_spent_comment_19)
        minutes >= 13 -> resources.getString(R.string.yir_time_spent_comment_13)
        minutes >= 9 -> resources.getString(R.string.yir_time_spent_comment_9)
        minutes >= 1 -> resources.getString(R.string.yir_time_spent_comment_1)
        else -> ""
    }
}
