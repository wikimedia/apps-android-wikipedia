package org.wikipedia.yearinreview

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.allowHardware
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.theme.Theme
import org.wikipedia.yearinreview.YearInReviewScreenData.CustomIconScreen

sealed class YearInReviewScreenData {
    open class StandardScreen(
        val animatedImageResource: Int = 0,
        val staticImageResource: Int = 0,
        val headlineText: Any? = null,
        val bodyText: Any? = null,
        val bottomButton: ButtonConfig? = null,
        val unlockIcon: UnlockIconConfig? = null
    ) : YearInReviewScreenData() {
        @Composable
        open fun Header(context: Context,
                        screenCaptureMode: Boolean,
                        isImageResourceLoaded: ((Boolean) -> Unit)? = null,
                        aspectRatio: Float) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(if (screenCaptureMode) staticImageResource else animatedImageResource)
                    .allowHardware(false)
                    .build(),
                loading = { LoadingIndicator() },
                success = { SubcomposeAsyncImageContent() },
                onSuccess = { isImageResourceLoaded?.invoke(true) },
                contentDescription = stringResource(R.string.year_in_review_screendeck_image_content_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
    }

    data class HighlightsScreen(
        val highlights: List<String>,
        val headlineText: String? = null
    ) : YearInReviewScreenData()

    data class GeoScreen(
        val coordinates: Map<String, List<Int>>, // just a placeholder, @TODO: replace with actual data type
        val headlineText: String? = null,
        val bodyText: String? = null
    ) : YearInReviewScreenData()

    class CustomIconScreen(
        headlineText: Any? = null,
        bodyText: Any? = null,
        val showDonateButton: Boolean = true
    ) : StandardScreen(
        headlineText = headlineText,
        bodyText = bodyText
    ) {
        @Composable
        override fun Header(context: Context,
                            screenCaptureMode: Boolean,
                            isImageResourceLoaded: ((Boolean) -> Unit)?,
                            aspectRatio: Float) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .background(
                        brush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0.265f to Color(0xFF0D0D0D),
                                0.385f to Color(0xFF092D60),
                                0.515f to Color(0xFF1171C8),
                                0.585f to Color(0xFF3DB2FF),
                                0.775f to Color(0xFFD3F1F3)
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = Modifier.size(200.dp),
                    painter = painterResource(R.drawable.launcher_foreground_yir25),
                    contentDescription = null
                )
            }
        }
    }
}

data class ButtonConfig(
    val text: String,
    val onClick: () -> Unit,
)

data class UnlockIconConfig(
    val isUnlocked: Boolean = false,
    val onClick: (() -> Unit)? = null
)

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
