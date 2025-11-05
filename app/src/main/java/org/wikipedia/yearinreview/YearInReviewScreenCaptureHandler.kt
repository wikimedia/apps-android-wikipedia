package org.wikipedia.yearinreview

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import org.wikipedia.R
import org.wikipedia.util.ShareUtil

@Composable
fun YearInReviewScreenCaptureHandler(
    request: YearInReviewCaptureRequest,
    onComplete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val shareImageCallback: (Bitmap) -> Unit = { bitmap ->
        val googlePlayUrl = context.getString(R.string.year_in_review_share_url) + YearInReviewViewModel.YIR_TAG
        val bodyText = context.getString(R.string.year_in_review_share_body, googlePlayUrl, context.getString(R.string.year_in_review_hashtag))
        ShareUtil.shareImage(
            coroutineScope = coroutineScope,
            context = context,
            bmp = bitmap,
            imageFileName = YearInReviewViewModel.YIR_TAG,
            subject = context.getString(R.string.year_in_review_share_subject),
            text = bodyText,
            onShared = onComplete
        )
    }

    when (request) {
        is YearInReviewCaptureRequest.StandardScreen -> {
            CreateScreenShotBitmap(
                screenContent = request.screenData,
                onBitmapReady = shareImageCallback,
                requestScreenshotBitmap = null
            )
        }
        is YearInReviewCaptureRequest.GeoScreen -> {
            CreateScreenShotBitmap(
                screenContent = request.screenData,
                onBitmapReady = shareImageCallback,
                requestScreenshotBitmap = request.requestScreenshotBitmap
            )
        }

        is YearInReviewCaptureRequest.HighlightsScreen -> {
            ShareHighlightsScreenCapture(
                data = request.data,
                onBitmapReady = shareImageCallback
            )
        }
    }
}

sealed class YearInReviewCaptureRequest {
    data class StandardScreen(val screenData: YearInReviewScreenData) : YearInReviewCaptureRequest()
    data class GeoScreen(val screenData: YearInReviewScreenData, val requestScreenshotBitmap: ((Int, Int) -> Bitmap)? = null) : YearInReviewCaptureRequest()
    data class HighlightsScreen(val data: YearInReviewScreenData.HighlightsScreen) : YearInReviewCaptureRequest()
}
