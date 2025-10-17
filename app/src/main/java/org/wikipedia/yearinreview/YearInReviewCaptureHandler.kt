package org.wikipedia.yearinreview

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import org.wikipedia.R
import org.wikipedia.util.ShareUtil

@Composable
fun YearInReviewCaptureHandler(
    request: YearInReviewCaptureRequest,
    onComplete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val shareImageCallback: (Bitmap) -> Unit = { bitmap ->
        ShareUtil.shareImage(
            coroutineScope = coroutineScope,
            context = context,
            bmp = bitmap,
            imageFileName = "year_in_review",
            subject = context.getString(R.string.year_in_review_share_subject),
            text = context.getString(R.string.year_in_review_share_url),
            onShared = onComplete
        )
    }

    when (request) {
        is YearInReviewCaptureRequest.StandardScreen -> {
            CreateScreenShotBitmap(
                screenContent = request.screenData,
                onBitmapReady = shareImageCallback
            )
        }
        is YearInReviewCaptureRequest.HighlightsScreen -> {
            ShareHighlightsScreenCapture(
                highlights = request.highlights,
                onBitmapReady = shareImageCallback
            )
        }
    }
}

sealed class YearInReviewCaptureRequest {
    data class StandardScreen(val screenData: YearInReviewScreenData) : YearInReviewCaptureRequest()
    data class HighlightsScreen(val highlights: List<YearInReviewScreenData.HighlightItem>) : YearInReviewCaptureRequest()
}
