package org.wikipedia.yearinreview

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearInReviewBottomSheetScaffold() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WikipediaTheme.colors.paperColor)
            .wrapContentHeight()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = 30.dp
            ),
            text = stringResource(R.string.year_in_review_explore_screen_title),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center

        )
        Text(
            modifier = Modifier.padding(
                horizontal = 45.dp
            ),
            text = stringResource(R.string.year_in_review_explore_screen_bodytext),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        AndroidView(
            factory = {
                ImageView(it).apply {
                    Glide.with(this)
                        .asGif()
                        .load(R.drawable.wyir_puzzle_4_v2)
                        .centerCrop()
                        .into(this)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f)
                .clip(RoundedCornerShape(16.dp))
        )
        Button(
            onClick = {
                context.startActivity((YearInReviewActivity.newIntent(context)))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
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

@Preview
@Composable
fun PreviewBottomSheet() {
    YearInReviewBottomSheetScaffold()
}
