package org.wikipedia.yearinreview

import android.content.Context
import android.graphics.drawable.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.history.db.HistoryEntryWithImage
import org.wikipedia.theme.Theme
import org.wikipedia.yearinreview.YearInReviewScreenData.CustomIconScreen

sealed class YearInReviewScreenData(
    val allowDonate: Boolean = true,
    val showDonateInToolbar: Boolean = true
) {

    @Composable
    open fun BottomButton(context: Context, onButtonClick: () -> Unit) {
    }

    open class StandardScreen(
        allowDonate: Boolean = true,
        val animatedImageResource: Int = 0,
        val headlineText: Any? = null,
        val bodyText: Any? = null,
        showDonateInToolbar: Boolean = true
    ) : YearInReviewScreenData(allowDonate, showDonateInToolbar) {

        open val imageModifier = Modifier.fillMaxSize()

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
                        .headerBackground(),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(animatedImageResource)
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

        open fun Modifier.headerBackground(): Modifier {
            return this.background(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.265f to Color(0xFF0D0D0D),
                        0.385f to Color(0xFF092D60),
                        0.515f to Color(0xFF1171C8),
                        0.585f to Color(0xFF3DB2FF),
                        0.775f to Color(0xFFD3F1F3)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                )
            )
        }
    }

    class HighlightsScreen(
        allowDonate: Boolean = true,
        val highlights: List<String>,
        val headlineText: String? = null
    ) : YearInReviewScreenData(allowDonate)

    class GeoScreen(
        allowDonate: Boolean = true,
        val largestClusterLatitude: Double,
        val largestClusterLongitude: Double,
        val largestClusterTopLeft: Pair<Double, Double>,
        val largestClusterBottomRight: Pair<Double, Double>,
        val pagesWithCoordinates: List<HistoryEntryWithImage>,
        val headlineText: String? = null,
        val bodyText: String? = null
    ) : YearInReviewScreenData(allowDonate)

    class ReadingPatterns(
        allowDonate: Boolean = true,
        animatedImageResource: Int = 0,
        headlineText: Any? = null,
        bodyText: Any? = null,
        val favoriteTimeText: String,
        val favoriteDayText: String,
        val favoriteMonthText: String
    ) : StandardScreen(
        allowDonate,
        animatedImageResource = animatedImageResource,
        headlineText = headlineText,
        bodyText = bodyText,
    )

    class CustomIconScreen(
        allowDonate: Boolean = true,
        headlineText: Any? = null,
        bodyText: Any? = null,
        val showDonateButton: Boolean = false
    ) : StandardScreen(
        allowDonate = allowDonate,
        animatedImageResource = R.drawable.launcher_foreground_yir25,
        headlineText = headlineText,
        bodyText = bodyText,
        showDonateInToolbar = !showDonateButton
    ) {

        override val imageModifier: Modifier = Modifier.size(200.dp)

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
            CustomIconScreen().Header(
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
            CustomIconScreen(allowDonate = true, showDonateButton = true).BottomButton(
                context = LocalContext.current,
                onButtonClick = {}
            )
        }
    }
}
