package org.wikipedia.yearinreview

import android.content.Context
import android.graphics.drawable.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.asDrawable
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.allowHardware
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.history.db.HistoryEntryWithImage
import org.wikipedia.theme.Theme
import org.wikipedia.yearinreview.YearInReviewScreenData.CustomIconScreen

fun Modifier.yearInReviewHeaderBackground(): Modifier {
    return this.background(
        brush = Brush.linearGradient(
            colorStops = arrayOf(
                0.125f to Color(0xFF171717),
                0.225f to Color(0xFF003F45),
                0.285f to Color(0xFF00807A),
                0.440f to Color(0xFF2AECA6),
                0.525f to Color(0xFF86FFAC),
                0.650f to Color(0xFFFFFFFF)
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, Float.POSITIVE_INFINITY)
        )
    )
}

sealed class YearInReviewScreenData(
    val allowDonate: Boolean = true,
    val showDonateInToolbar: Boolean = true
) {

    @Composable
    open fun BottomButton(context: Context, onButtonClick: () -> Unit) {
    }

    open class StandardScreen(
        allowDonate: Boolean = true,
        val imageResource: Int = 0,
        val imageModifier: Modifier = Modifier.size(200.dp),
        val headlineText: Any? = null,
        val bodyText: Any? = null,
        val slideName: String,
        showDonateInToolbar: Boolean = true
    ) : YearInReviewScreenData(allowDonate, showDonateInToolbar) {

        @Composable
        open fun Header(context: Context,
                        screenCaptureMode: Boolean,
                        isImageResourceLoaded: ((Boolean) -> Unit)? = null,
                        aspectRatio: Float) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .yearInReviewHeaderBackground(),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageResource)
                            .allowHardware(false)
                            .build(),
                        loading = { LoadingIndicator() },
                        success = {
                            val drawable = it.result.image.asDrawable(context.resources)
                            val animatable = drawable as? Animatable
                            animatable?.let { animation ->
                                if (screenCaptureMode) {
                                    animation.stop()
                                } else if (!animation.isRunning) {
                                    animation.start()
                                }
                            }
                            SubcomposeAsyncImageContent()
                        },
                        onSuccess = { isImageResourceLoaded?.invoke(true) },
                        contentDescription = stringResource(R.string.year_in_review_screendeck_image_content_description),
                        modifier = imageModifier
                    )
                }
            }
        }
    }

    data class HighlightItem(
        val title: String,
        val singleValue: String? = null,
        val items: List<String> = emptyList(),
        val highlightColor: Color = ComposeColors.Gray700
    )

    data class HighlightsScreen(
        val highlights: List<HighlightItem>,
        val slideName: String
    ) : YearInReviewScreenData()

    class GeoScreen(
        allowDonate: Boolean = true,
        val largestClusterTopLeft: Pair<Double, Double>,
        val largestClusterBottomRight: Pair<Double, Double>,
        val pagesWithCoordinates: List<HistoryEntryWithImage>,
        val headlineText: String? = null,
        val bodyText: String? = null,
        val slideName: String
    ) : YearInReviewScreenData(allowDonate)

    class ReadingPatterns(
        allowDonate: Boolean = true,
        imageResource: Int = 0,
        headlineText: Any? = null,
        bodyText: Any? = null,
        slideName: String,
        val favoriteTimeText: String,
        val favoriteDayText: String,
        val favoriteMonthText: String,
    ) : StandardScreen(
        allowDonate,
        imageResource = imageResource,
        headlineText = headlineText,
        bodyText = bodyText,
        slideName = slideName
    )

    class CustomIconScreen(
        allowDonate: Boolean = true,
        headlineText: Any? = null,
        bodyText: Any? = null,
        slideName: String,
        val showDonateButton: Boolean = false
    ) : StandardScreen(
        allowDonate = allowDonate,
        imageResource = R.drawable.launcher_foreground_yir25,
        headlineText = headlineText,
        bodyText = bodyText,
        slideName = slideName,
        showDonateInToolbar = !showDonateButton
    ) {
        @Composable
        override fun BottomButton(context: Context, onButtonClick: () -> Unit) {
            if (showDonateButton) {
                Button(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.progressiveColor
                    ),
                    onClick = onButtonClick,
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_heart_24),
                        tint = Color.White,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(start = 6.dp),
                        text = stringResource(R.string.year_in_review_donate),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CustomIconScreenHeaderPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        Box(
            modifier = Modifier.size(400.dp, 300.dp)
        ) {
            CustomIconScreen(
                slideName = "test"
            ).Header(
                context = LocalContext.current,
                screenCaptureMode = false,
                aspectRatio = 1f
            )
        }
    }
}

@Preview
@Composable
private fun CustomIconScreenButtonPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        Box(
            modifier = Modifier.size(400.dp, 200.dp)
        ) {
            CustomIconScreen(
                allowDonate = true,
                showDonateButton = true,
                slideName = "test"
            ).BottomButton(
                context = LocalContext.current,
                onButtonClick = {}
            )
        }
    }
}
