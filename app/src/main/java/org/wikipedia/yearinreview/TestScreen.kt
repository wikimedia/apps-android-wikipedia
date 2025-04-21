package org.wikipedia.yearinreview

import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun TestScreen(
    viewModel: YearInReviewViewModel,
    screenData: YearInReviewScreenData = nonEnglishCollectiveReadCountData
) {
    val scrollState = rememberScrollState()
    val gifAspectRatio = 3f / 2f
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        Glide.with(context)
                            .asGif()
                            .load(screenData.imageResource)
                            .centerCrop()
                            .into(this)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(gifAspectRatio)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .border(BorderStroke(width = 2.dp, color = Color.Red))
        ) {

            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .height(IntrinsicSize.Min)
                        .weight(1f),
                    text = stringResource(screenData.headLineText),
                    color = Color.Black,
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(
                    onClick = { /* TODO() */ }) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_info_24),
                        tint = WikipediaTheme.colors.primaryColor,
                        contentDescription = stringResource(R.string.year_in_review_information_icon)
                    )
                }
            }
            Text(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .height(IntrinsicSize.Min),
                text = stringResource(screenData.bodyText),
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview
@Composable
fun PreviewTestScreen() {
    // TestScreen()
}

/*

when (derivedMapState.value) {
            is Resource.Loading -> {
                Text(
                    text = "LOADING",
                    fontSize = 20.sp,
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                )
            }

            is Resource.Success -> {
                val fetchedHeadLine =
                    (derivedMapState.value as Resource.Success<YearInReviewTextData>).data.headLineText
                val fetchedBodyText =
                    (derivedMapState.value as Resource.Success<YearInReviewTextData>).data.bodyText
                Text(
                    text = String.format(
                        stringResource(screenDataObj.headLineText),
                        fetchedHeadLine
                    ),
                    fontSize = 20.sp,
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                )

                Text(
                    text = String.format(stringResource(screenDataObj.bodyText), fetchedBodyText),
                    fontSize = 20.sp,
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                )
            }

            else -> {
                Text(
                    text = "Error",
                    fontSize = 30.sp,
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                )
            }
        }
 */
