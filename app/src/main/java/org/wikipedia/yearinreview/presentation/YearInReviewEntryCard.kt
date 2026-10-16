package org.wikipedia.yearinreview.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.theme.Theme

private val EntryCardBaseGradient = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.26f to Color(0xFF010012),
        0.75f to Color(0xFF00146C),
        1f to Color(0xFF003489)
    )
)

private val EntryCardGlowColor = Color(0xFF07289E)

private val EntryCardBottomGlow = Brush.verticalGradient(
    colorStops = arrayOf(
        0.34f to EntryCardGlowColor.copy(alpha = 0f),
        0.79f to EntryCardGlowColor.copy(alpha = 0.57f),
        1f to EntryCardGlowColor
    )
)

@Composable
fun YearInReviewEntryCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onGetStartedClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(EntryCardBaseGradient)
            .background(EntryCardBottomGlow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                color = ComposeColors.White
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = ComposeColors.White
            )
            Button(
                modifier = Modifier.padding(top = 16.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ComposeColors.Blue600,
                    contentColor = ComposeColors.White
                ),
                onClick = onGetStartedClick
            ) {
                Text(
                    text = stringResource(R.string.year_in_review_get_started),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Preview
@Composable
private fun YearInReviewEntryCardPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        YearInReviewEntryCard(
            modifier = Modifier.padding(16.dp),
            title = stringResource(R.string.year_in_review_entry_card_title_collective),
            subtitle = stringResource(R.string.year_in_review_entry_card_subtitle),
            onGetStartedClick = {}
        )
    }
}
