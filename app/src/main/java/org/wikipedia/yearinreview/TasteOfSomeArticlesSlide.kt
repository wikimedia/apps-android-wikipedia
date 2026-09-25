package org.wikipedia.yearinreview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.rive.GetBitmapFun
import app.rive.Result
import app.rive.RiveFile
import org.wikipedia.R

val BiggestReadingDayArticlesRiveSpec = RiveSlideSpec(
    resourceId = R.raw.all_templates,
    artboardName = "frame9",
    stateMachineName = "frame9-statemachine",
    viewModelName = RIVE_LIST_VIEW_MODEL,
    instanceType = RiveInstanceType.Named("frame9"),
    globalViewModel = YearInReviewRiveGlobalProperties
)

@Composable
fun TasteOfSomeArticlesSlide(
    riveFileResult: Result<RiveFile>,
    articles: List<YearInReviewArticle>,
    slideId: String,
    screenshotGetters: MutableMap<String, GetBitmapFun>,
    playing: Boolean,
    onRiveError: (Throwable) -> Unit,
    modifier: Modifier = Modifier
) {
    val headline = stringResource(R.string.year_in_review_biggest_reading_day_articles_headline)
    val rows = articles.map { article ->
        RiveListRow(
            title = article.title,
            subtitle = article.description,
            imageUrl = article.thumbnailUrl
        )
    }
    val textProperties = mapOf(RIVE_LIST_PROPERTY_HEADLINE to headline) + rows.toRiveListTextProperties()
    YearInReviewRiveSlide(
        riveFileResult = riveFileResult,
        slideId = slideId,
        screenshotGetters = screenshotGetters,
        spec = BiggestReadingDayArticlesRiveSpec,
        textProperties = textProperties,
        imageUrls = rows.toRiveListImageUrls(),
        accessibilityDescription = textProperties.values.filter { it.isNotEmpty() }.joinToString(" "),
        playing = playing,
        modifier = modifier,
        onRiveError = onRiveError
    )
}
