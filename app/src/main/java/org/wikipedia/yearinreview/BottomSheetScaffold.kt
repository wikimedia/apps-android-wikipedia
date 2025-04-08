package org.wikipedia.yearinreview

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme

const val TEXT_PADDING_PERCENT_OF_SCREEN_WIDTH = 0.125
const val BUTTON_PADDING_PERCENT_OF_SCREEN_WIDTH = 0.05

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearInReviewBottomSheetScaffold() {

    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val textHorizontalPadding = (screenWidth * TEXT_PADDING_PERCENT_OF_SCREEN_WIDTH).dp
    val buttonHorizontalPadding = (screenWidth * BUTTON_PADDING_PERCENT_OF_SCREEN_WIDTH).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(WikipediaTheme.colors.paperColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = textHorizontalPadding
            ),
            text = stringResource(R.string.year_in_review_explore_screen_title),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center

        )
        Text(
            modifier = Modifier.padding(
                horizontal = textHorizontalPadding
            ),
            text = stringResource(R.string.year_in_review_explore_screen_bodytext),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        AndroidView(
            factory = {
                ImageView(context).apply {
                    Glide.with(context)
                        .asGif()
                        .load(R.drawable.wyir_puzzle_4_v2)
                        .centerCrop()
                        .into(this)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(getAspectRatio(context, R.drawable.wyir_puzzle_4_v2))
                .clip(RoundedCornerShape(16.dp))
        )
        Button(
            onClick = {
                context.startActivity((YearInReviewActivity.newIntent(context)))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = buttonHorizontalPadding,
                    end = buttonHorizontalPadding
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = WikipediaTheme.colors.progressiveColor,
                contentColor = WikipediaTheme.colors.paperColor
            )
        ) {
            Text(
                text = stringResource(R.string.year_in_review_explore_screen_continue),
                color = WikipediaTheme.colors.paperColor,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

fun getAspectRatio(context: Context, imageResource: Int): Float {

    val bitmapObj = BitmapFactory.decodeResource(context.resources, imageResource)
    val resourceHeight = bitmapObj.height.toFloat()
    val resourceWidth = bitmapObj.width.toFloat()

    return (resourceWidth / resourceHeight)
}

@Preview
@Composable
fun previewBottomSheet() {
    YearInReviewBottomSheetScaffold()
}
