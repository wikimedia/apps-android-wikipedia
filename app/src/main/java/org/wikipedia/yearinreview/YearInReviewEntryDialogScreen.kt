package org.wikipedia.yearinreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun YearInReviewEntryDialogScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(vertical = 16.dp)
            .nestedScroll(rememberNestedScrollInteropConnection())
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = 32.dp
            ),
            text = stringResource(R.string.year_in_review_entry_dialog_title),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center

        )
        Text(
            modifier = Modifier.padding(
                horizontal = 16.dp
            ),
            text = stringResource(R.string.year_in_review_entry_dialog_bodytext),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        AsyncImage(
            model = R.drawable.wyir_puzzle_4_v2,
            contentDescription = stringResource(R.string.year_in_review_entry_dialog_screen_image_content_description),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
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
                text = stringResource(R.string.year_in_review_entry_dialog_continue),
                color = WikipediaTheme.colors.paperColor,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomSheet() {
    YearInReviewEntryDialogScreen()
}
