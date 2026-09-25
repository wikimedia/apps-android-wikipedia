package org.wikipedia.yearinreview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.rive.GetBitmapFun
import app.rive.Result
import app.rive.RiveFile
import org.wikipedia.R

val TopicRunnersUpRiveSpec = RiveSlideSpec(
    resourceId = R.raw.all_templates,
    artboardName = "frame7",
    stateMachineName = "frame7-statemachine",
    viewModelName = RIVE_LIST_VIEW_MODEL,
    instanceType = RiveInstanceType.Named("frame7"),
    globalViewModel = YearInReviewRiveGlobalProperties
)

@Composable
fun YearInReviewTopicRunnersUpSlide(
    riveFileResult: Result<RiveFile>,
    year: Int,
    topics: List<YearInReviewTopic>,
    slideId: String,
    screenshotGetters: MutableMap<String, GetBitmapFun>,
    playing: Boolean,
    onRiveError: (Throwable) -> Unit,
    modifier: Modifier = Modifier
) {
    val bodyText = stringResource(R.string.year_in_review_topic_runners_up_body, year)
    val rows = topics.map { topic ->
        RiveListRow(
            title = topic.name,
            subtitle = pluralStringResource(
                R.plurals.year_in_review_topic_runners_up_article_count,
                topic.articleCount,
                rememberLocalizedNumber(topic.articleCount)
            )
        )
    }
    val textProperties = mapOf(RIVE_LIST_PROPERTY_BODY_TEXT to bodyText) + rows.toRiveListTextProperties()
    YearInReviewRiveSlide(
        riveFileResult = riveFileResult,
        slideId = slideId,
        screenshotGetters = screenshotGetters,
        spec = TopicRunnersUpRiveSpec,
        textProperties = textProperties,
        // TODO: pass the topic icons with toRiveListImageUrls() once topics have them; until then the designer's sample icons show
        accessibilityDescription = textProperties.values.filter { it.isNotEmpty() }.joinToString(" "),
        playing = playing,
        modifier = modifier,
        onRiveError = onRiveError
    )
}
