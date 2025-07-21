package org.wikipedia.donate.donationreminder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.compose.components.InlinePosition
import org.wikipedia.compose.components.TextWithInlineElement
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

// @TODO: once PM confirms final copy update the strings
@Composable
fun DonationReminderScreen(
    modifier: Modifier = Modifier,
    onBackButtonClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            WikiTopAppBar(
                title = "Donation reminders",
                onNavigationClick = onBackButtonClick
            )
        },
        containerColor = WikipediaTheme.colors.paperColor,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TextWithInlineElement(
                text = "Thank you for joining the 2% of readers who give what they can to keep this valuable resource ad-free, up-to-date, and available for all.",
                position = InlinePosition.END,
                placeholder = Placeholder(
                    width = 20.sp,
                    height = 20.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                ),
                content = {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = WikipediaTheme.colors.destructiveColor
                    )
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Donations go to the Wikimedia Foundation and affiliates, proud hosts of Wikipedia and its sister sites.",
                style = MaterialTheme.typography.bodySmall,
                color = WikipediaTheme.colors.placeholderColor
            )
        }
    }
}

@Preview
@Composable
private fun DonationReminderScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        DonationReminderScreen(
            onBackButtonClick = {}
        )
    }
}
