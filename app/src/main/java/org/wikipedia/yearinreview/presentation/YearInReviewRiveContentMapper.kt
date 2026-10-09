package org.wikipedia.yearinreview.presentation

import android.content.res.Resources
import android.view.View
import org.wikipedia.R
import org.wikipedia.topics.ArticleTopic
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.data.YearInReviewReadingStats
import java.text.NumberFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.concurrent.TimeUnit

/**
 * Maps each page to its Rive content, or null when the page doesn't use Rive.
 * A custom Rive slide still returns its content here, so its file gets loaded, and gets its own branch in the pager.
 */
object YearInReviewRiveContentMapper {
    fun map(page: YearInReviewPage, resources: Resources): RiveSlideContent? = when (page) {
        is YearInReviewPage.Cover -> RiveSlideContent(
            spec = allTemplatesSlideSpec("cover"),
            textProperties = mapOf(
                "coverTitle" to resources.getString(R.string.yir_intro_headline).uppercase(),
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
        is YearInReviewPage.Visits -> {
            val headline = resources.getString(R.string.yir_days_visited_headline, YearInReviewConfig.YEAR)
            val data = page.days.toString()
            val bodyCopy = page.peakMonth?.let {
                resources.getQuantityString(
                    R.plurals.yir_days_visited_supporting,
                    it.visitedDays,
                    Month.of(it.month).getDisplayName(TextStyle.FULL_STANDALONE, resources.configuration.locales[0]),
                    it.visitedDays
                )
            }.orEmpty()
            RiveSlideContent(
                spec = allTemplatesSlideSpec("frame2", instanceType = RiveInstanceType.Blank),
                textProperties = mapOf(
                    "headline" to headline,
                    "data" to data,
                    "bodyCopy" to bodyCopy
                ),
                accessibilityDescription = listOf(headline, data, bodyCopy).filter { it.isNotEmpty() }.joinToString("\n")
            )
        }
        is YearInReviewPage.TimeSpent -> RiveSlideContent(
            spec = allTemplatesSlideSpec("frame3"),
            textProperties = mapOf(
                "headline" to resources.getString(R.string.yir_minutes_read_headline),
                "data" to page.duration.toString(),
                "bodyCopy" to resources.getString(R.string.yir_minutes_read_supporting, getTimeSpentComment(resources, page.duration))
            )
        )
        is YearInReviewPage.ReadingStreak -> RiveSlideContent(
            spec = allTemplatesSlideSpec(if (page.isEmptyState) "frame4-empty" else "frame4"),
            textProperties = mapOf(
                "headline" to resources.getString(if (page.isEmptyState) R.string.yir_reading_streak_empty_headline else R.string.yir_reading_streak_headline),
                "data" to page.longestStreak.toString(),
                "bodyCopy" to if (page.isEmptyState) resources.getQuantityString(R.plurals.yir_reading_streak_empty_supporting, YearInReviewReadingStats.MIN_ARTICLES_READ, YearInReviewReadingStats.MIN_ARTICLES_READ)
                else resources.getString(R.string.yir_reading_streak_supporting, page.streakStartDate, page.streakEndDate)
            )
        )
        is YearInReviewPage.ReadingPattern -> RiveSlideContent(
            spec = allTemplatesSlideSpec(if (page.isEmptyState) "frame5-empty" else "frame5"),
            textProperties = mapOf(
                "headline" to resources.getString(if (page.isEmptyState) R.string.yir_reading_time_empty_headline else R.string.yir_reading_time_headline),
                "data" to page.favoriteTime,
                "bodyCopy" to if (page.isEmptyState) resources.getString(R.string.yir_reading_time_empty_supporting)
                else resources.getQuantityString(R.plurals.yir_reading_time_supporting, page.favoriteTimePercentage, page.favoriteTimePercentage)
            )
        )
        is YearInReviewPage.TopTopic -> RiveSlideContent(
            spec = allTemplatesSlideSpec(if (page.isEmptyState) "frame6-empty" else "frame6"),
            textProperties = mapOf(
                "headline" to resources.getString(if (page.isEmptyState) R.string.yir_top_topic_empty_headline else R.string.yir_top_topic_headline, YearInReviewConfig.YEAR),
                "data" to if (page.isEmptyState) "" else resources.getString(page.topic.msgKey).uppercase(),
                "bodyCopy" to if (page.isEmptyState) resources.getString(R.string.yir_top_topic_empty_supporting)
                else resources.getQuantityString(R.plurals.yir_top_topic_supporting, page.articles.size,
                    page.articles.first(), page.articles.last(), page.articles.size, getTopicComment(resources, page.topic))
            )
        )
        is YearInReviewPage.OtherTopTopics -> RiveSlideContent(
            spec = allTemplatesListSlideSpec(artboardName = "frame7"),
            textProperties = mapOf(
                "bodyText" to resources.getString(R.string.yir_runner_up_topics_headline, YearInReviewConfig.YEAR)
            ) + listRows(page.topicArticleCounts.map { (topic, count) ->
                resources.getString(topic.msgKey) to resources.getQuantityString(R.plurals.yir_runner_up_topics_article_count, count, count)
            }),
            numberProperties = listItemCount(page.topicArticleCounts.size)
        )
        is YearInReviewPage.BiggestReadingDay -> RiveSlideContent(
            spec = allTemplatesSlideSpec("frame8"),
            textProperties = mapOf(
                "headline" to resources.getString(R.string.yir_biggest_reading_day_headline),
                "data" to page.date,
                "bodyCopy" to resources.getQuantityString(R.plurals.yir_biggest_reading_day_supporting, page.minutesSpent, page.minutesSpent)
            )
        )
        is YearInReviewPage.BiggestReadingDayArticles -> RiveSlideContent(
            // v3 splits frame9 into a left-to-right and a right-to-left artboard, which share one instance
            spec = allTemplatesSlideSpec(
                artboardName = if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) "frame9-rightToLeft" else "frame9-leftToRight",
                viewModelName = "List",
                instanceType = RiveInstanceType.Named("frame9")
            ),
            textProperties = mapOf(
                "headline" to resources.getString(R.string.yir_articles_read_headline)
            ) + listRows(page.articles.map { it.title to it.description }),
            numberProperties = listItemCount(page.articles.size),
            imageUrls = listIcons(page.articles.map { it.thumbnailUrl })
        )
        is YearInReviewPage.Category -> RiveSlideContent(
            spec = allTemplatesSlideSpec("frame12"),
            textProperties = mapOf(
                "headline" to resources.getString(R.string.yir_niche_category_headline),
                "data" to page.categoryName,
                "bodyCopy" to resources.getString(R.string.yir_niche_category_supporting)
            )
        )
        is YearInReviewPage.RevisitedArticles -> if (page.isEmptyState) {
            RiveSlideContent(
                spec = allTemplatesListSlideSpec("frame13-empty"),
                textProperties = mapOf(
                    "headline" to resources.getString(R.string.yir_reread_articles_empty_headline),
                    "bodyText" to resources.getString(R.string.yir_reread_articles_empty_supporting)
                )
            )
        } else {
            RiveSlideContent(
                spec = allTemplatesListSlideSpec("frame13"),
                textProperties = mapOf(
                    "bodyText" to resources.getString(R.string.yir_reread_articles_headline)
                ) + listRows(page.articles.map { it.title to it.description }),
                imageUrls = listIcons(page.articles.map { it.thumbnailUrl })
            )
        }
        is YearInReviewPage.Geography -> if (page.isEmptyState) {
            RiveSlideContent(
                spec = allTemplatesListSlideSpec("frame14-empty"),
                textProperties = mapOf(
                    "headline" to resources.getString(R.string.yir_places_empty_headline),
                    "bodyText" to resources.getString(R.string.yir_places_empty_supporting, YearInReviewConfig.YEAR)
                )
            )
        } else {
            RiveSlideContent(
                spec = allTemplatesListSlideSpec("frame14"),
                textProperties = mapOf(
                    "bodyText" to resources.getString(R.string.yir_places_headline)
                ) + listRows(page.placeArticleCounts.map { (place, count) ->
                    place to resources.getQuantityString(R.plurals.yir_places_article_count, count, count)
                })
            )
        }
        is YearInReviewPage.SavedArticles -> if (page.isEmptyState) {
            RiveSlideContent(
                spec = allTemplatesListSlideSpec("frame15-empty"),
                textProperties = mapOf(
                    "headline" to resources.getString(R.string.yir_saved_articles_empty_headline),
                    "bodyText" to resources.getString(R.string.yir_saved_articles_empty_supporting)
                )
            )
        } else {
            RiveSlideContent(
                spec = allTemplatesListSlideSpec("frame15"),
                textProperties = mapOf(
                    "bodyText" to resources.getQuantityString(R.plurals.yir_saved_articles_headline, page.savedCount, page.savedCount)
                ) + listRows(page.articles.map { it.title to getViewCountText(resources, it.viewCount) }),
                imageUrls = listIcons(page.articles.map { it.thumbnailUrl })
            )
        }
        is YearInReviewPage.TotalEdits -> RiveSlideContent(
            spec = allTemplatesSlideSpec(if (page.isEmptyState) "frame16-empty" else "frame16"),
            textProperties = mapOf(
                "headline" to resources.getString(if (page.isEmptyState) R.string.yir_edits_empty_headline else R.string.yir_edits_headline),
                "data" to if (page.isEmptyState) "" else page.editCount.toString(),
                "bodyCopy" to resources.getString(if (page.isEmptyState) R.string.yir_edits_empty_supporting else R.string.yir_edits_supporting)
            )
        )
        is YearInReviewPage.EditedArticleViews -> RiveSlideContent(
            spec = allTemplatesSlideSpec("frame17"),
            textProperties = mapOf(
                "headline" to resources.getString(R.string.yir_edit_views_headline),
                "bodyCopy" to resources.getString(R.string.yir_edit_views_supporting, YearInReviewConfig.YEAR),
                "data" to formatNumber(resources, page.viewCount)
            )
        )
        is YearInReviewPage.MostViewedEditedArticles -> RiveSlideContent(
            spec = allTemplatesListSlideSpec("frame18"),
            textProperties = mapOf(
                "bodyText" to resources.getString(R.string.yir_most_viewed_edited_articles_headline)
            ) + listRows(page.articles.map { it.title to getViewCountText(resources, it.viewCount) }),
            imageUrls = listIcons(page.articles.map { it.thumbnailUrl })
        )
        is YearInReviewPage.ThankYou -> RiveSlideContent(
            spec = allTemplatesSlideSpec("end"),
            textProperties = mapOf(
                "coverTitle" to resources.getString(R.string.yir_contributor_thanks_headline).uppercase(),
                "bodyCopy" to resources.getString(R.string.yir_contributor_thanks_supporting, getDonorEditorString(resources, page.isDonor, page.isEditor)),
                "data" to ""
            )
        )
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
        "coverGlyphsFontSize" to 44f,
        "coverGlyphsLineHeight" to 48f,
        "dataNumberFontSize" to 72f,
        "dataNumbersLineHeight" to 76f,
        "headlineFontSize" to 24f,
        "headlineLineHeight" to 25.44f,
        "bodyCopyFontSize" to 16f,
        "bodyCopyLineHeight" to 20f
    )
)

// In all_templates.riv, each artboard has a same-named view model instance holding sample text
private fun allTemplatesSlideSpec(
    artboardName: String,
    viewModelName: String = "DataTemplate",
    instanceType: RiveInstanceType = RiveInstanceType.Named(artboardName)
) = RiveSlideSpec(
    resourceId = R.raw.all_templates_v3,
    artboardName = artboardName,
    viewModelName = viewModelName,
    instanceType = instanceType,
    globalViewModel = AllTemplatesGlobalProperties,
    isTopBarUiIconsWhite = "isUIWhite"
)

// Artboards that show up to three articles or topics, each with an icon, title and subtitle
private fun allTemplatesListSlideSpec(artboardName: String) = allTemplatesSlideSpec(artboardName, viewModelName = "List")

// Fills the List view model's numbered rows. Rows without an item are cleared, so the instance's sample text doesn't show.
private fun listRows(titlesAndSubtitles: List<Pair<String, String>>): Map<String, String> {
    return (1..LIST_ROW_COUNT).flatMap { row ->
        val (title, subtitle) = titlesAndSubtitles.getOrNull(row - 1) ?: ("" to "")
        listOf("articleTitle$row" to title, "subTitle$row" to subtitle)
    }.toMap()
}

// Rows without an image are cleared, so the instance's sample picture doesn't show
private fun listIcons(imageUrls: List<String?>): Map<String, String?> {
    return (1..LIST_ROW_COUNT).associate { row -> "icon$row" to imageUrls.getOrNull(row - 1) }
}

// frame7 and frame9 have this property so far.
private fun listItemCount(itemCount: Int): Map<String, Int> {
    return mapOf("numOfListItems" to itemCount.coerceAtMost(LIST_ROW_COUNT))
}

private const val LIST_ROW_COUNT = 3

// "1,234 views"
private fun getViewCountText(resources: Resources, viewCount: Long): String {
    return resources.getQuantityString(R.plurals.yir_article_view_count, viewCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), formatNumber(resources, viewCount))
}

// With the locale's own digits and separators, e.g. 1,234 in English and १,२३४ in Nepali
private fun formatNumber(resources: Resources, number: Long): String {
    return NumberFormat.getInstance(resources.configuration.locales[0]).format(number)
}

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

private fun getTopicComment(resources: Resources, topic: ArticleTopic): String {
    // Listed explicitly, so the strings survive resource shrinking
    val resId = when (topic.topicId) {
        "architecture" -> R.string.yir_topic_comment_architecture
        "art" -> R.string.yir_topic_comment_art
        "comics-and-anime" -> R.string.yir_topic_comment_comics_and_anime
        "entertainment" -> R.string.yir_topic_comment_entertainment
        "fashion" -> R.string.yir_topic_comment_fashion
        "literature" -> R.string.yir_topic_comment_literature
        "music" -> R.string.yir_topic_comment_music
        "performing-arts" -> R.string.yir_topic_comment_performing_arts
        "sports" -> R.string.yir_topic_comment_sports
        "tv-and-film" -> R.string.yir_topic_comment_tv_and_film
        "video-games" -> R.string.yir_topic_comment_video_games
        "biography" -> R.string.yir_topic_comment_biography
        "women" -> R.string.yir_topic_comment_women
        "business-and-economics" -> R.string.yir_topic_comment_business_and_economics
        "education" -> R.string.yir_topic_comment_education
        "food-and-drink" -> R.string.yir_topic_comment_food_and_drink
        "history" -> R.string.yir_topic_comment_history
        "military-and-warfare" -> R.string.yir_topic_comment_military_and_warfare
        "philosophy-and-religion" -> R.string.yir_topic_comment_philosophy_and_religion
        "politics-and-government" -> R.string.yir_topic_comment_politics_and_government
        "society" -> R.string.yir_topic_comment_society
        "transportation" -> R.string.yir_topic_comment_transportation
        "biology" -> R.string.yir_topic_comment_biology
        "chemistry" -> R.string.yir_topic_comment_chemistry
        "computers-and-internet" -> R.string.yir_topic_comment_computers_and_internet
        "earth-and-environment" -> R.string.yir_topic_comment_earth_and_environment
        "engineering" -> R.string.yir_topic_comment_engineering
        "general-science" -> R.string.yir_topic_comment_general_science
        "mathematics" -> R.string.yir_topic_comment_mathematics
        "medicine-and-health" -> R.string.yir_topic_comment_medicine_and_health
        "physics" -> R.string.yir_topic_comment_physics
        "technology" -> R.string.yir_topic_comment_technology
        "africa" -> R.string.yir_topic_comment_africa
        "asia" -> R.string.yir_topic_comment_asia
        "central-america" -> R.string.yir_topic_comment_central_america
        "europe" -> R.string.yir_topic_comment_europe
        "north-america" -> R.string.yir_topic_comment_north_america
        "oceania" -> R.string.yir_topic_comment_oceania
        "south-america" -> R.string.yir_topic_comment_south_america
        else -> null
    }
    return resId?.let { resources.getString(it) }.orEmpty()
}

private fun getDonorEditorString(resources: Resources, isDonor: Boolean, isEditor: Boolean): String {
    return if (isDonor && isEditor) {
        resources.getString(R.string.year_in_review_slide_app_icon_donor_and_editor)
    } else if (isDonor) {
        resources.getString(R.string.year_in_review_slide_app_icon_donor)
    } else {
        resources.getString(R.string.year_in_review_slide_app_icon_editor)
    }
}
