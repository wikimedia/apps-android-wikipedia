package org.wikipedia.activitytab

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.yearinreview.presentation.YearInReviewEntryCard

@Composable
fun ActivityTabLoggedOutScreen(
    onCreateAccountClick: () -> Unit,
    onLoginClick: () -> Unit,
    showYearInReviewCard: Boolean,
    onYirGetStartedClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor),
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // The spacers center the prompt and keep the card at the bottom while everything fits, and collapse so the whole screen scrolls when it doesn't.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))
                LoggedOutPrompt(
                    onCreateAccountClick = onCreateAccountClick,
                    onLoginClick = onLoginClick
                )
                Spacer(modifier = Modifier.weight(1f))

                if (showYearInReviewCard) {
                    YearInReviewEntryCard(
                        modifier = Modifier.padding(16.dp),
                        title = stringResource(R.string.year_in_review_entry_card_title_collective),
                        subtitle = stringResource(R.string.year_in_review_entry_card_data_low_subtitle),
                        onGetStartedClick = onYirGetStartedClick
                    )
                }
            }
        }
    }
}

@Composable
private fun LoggedOutPrompt(
    modifier: Modifier = Modifier,
    onCreateAccountClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.size(164.dp),
            painter = painterResource(R.drawable.illustration_activity_tab_logged_out),
            contentDescription = null
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(R.string.activity_tab_logged_out_title),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            color = WikipediaTheme.colors.primaryColor
        )
        Button(
            modifier = Modifier.padding(top = 16.dp),
            contentPadding = PaddingValues(horizontal = 18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WikipediaTheme.colors.progressiveColor,
                contentColor = Color.White,
            ),
            onClick = onCreateAccountClick
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_user_avatar),
                tint = Color.White,
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = stringResource(R.string.create_account_button),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Button(
            contentPadding = PaddingValues(horizontal = 18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WikipediaTheme.colors.paperColor,
                contentColor = WikipediaTheme.colors.primaryColor,
            ),
            onClick = onLoginClick
        ) {
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = stringResource(R.string.menu_login),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview
@Composable
private fun ActivityTabLoggedOutScreenPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ActivityTabLoggedOutScreen(
            onCreateAccountClick = {},
            onLoginClick = {},
            showYearInReviewCard = false,
            onYirGetStartedClick = {}
        )
    }
}

@Preview
@Composable
private fun ActivityTabLoggedOutScreenWithYearInReviewPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ActivityTabLoggedOutScreen(
            onCreateAccountClick = {},
            onLoginClick = {},
            showYearInReviewCard = true,
            onYirGetStartedClick = {}
        )
    }
}
