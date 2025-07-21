package org.wikipedia.donate.donationreminder

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.AppTextButton
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
                .fillMaxSize()
        ) {
            MainContent(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
                    .padding(16.dp)
            )
            BottomContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun MainContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
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

        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 24.dp),
            color = WikipediaTheme.colors.borderColor
        )

        DonationRemindersSwitch(
            isDonationRemindersEnabled = true,
            onCheckedChange = {}
        )

        DonationReminderOption(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            headlineText = "When I read",
            placeHolderText = "25 articles",
            headlineIcon = R.drawable.newsstand_24dp
        )

        DonationReminderOption(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            headlineText = "Remind me to donate",
            placeHolderText = "$3",
            headlineIcon = R.drawable.newsstand_24dp
        )
    }
}

@Composable
fun BottomContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
    ) {
        AppButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = {},
            content = {
                Text(
                    "Confirm Reminder"
                )
            }
        )
        AppTextButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = {},
            content = {
                Text(
                    "About this experiment"
                )
            }
        )
    }
}

@Composable
fun DonationReminderOption(
    modifier: Modifier = Modifier,
    headlineText: String,
    placeHolderText: String,
    @DrawableRes headlineIcon: Int,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(headlineIcon),
            contentDescription = null
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.bodyLarge
            )

            TextField(
                modifier = Modifier
                    .width(210.dp),
                value = "",
                onValueChange = {},
                placeholder = {
                    Text(
                        text = placeHolderText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = WikipediaTheme.colors.primaryColor,
                    unfocusedContainerColor = WikipediaTheme.colors.backgroundColor
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun DonationRemindersSwitch(
    isDonationRemindersEnabled: Boolean,
    onCheckedChange: ((Boolean) -> Unit),
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        colors = ListItemDefaults.colors(
            containerColor = WikipediaTheme.colors.backgroundColor
        ),
        headlineContent = {
            Text(
                text = "Donation reminders",
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.primaryColor
            )
        },
        trailingContent = {
            Switch(
                checked = isDonationRemindersEnabled,
                onCheckedChange = {
                    onCheckedChange(it)
                },
                colors = SwitchDefaults.colors(
                    uncheckedTrackColor = WikipediaTheme.colors.paperColor,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    checkedTrackColor = WikipediaTheme.colors.progressiveColor,
                    checkedThumbColor = WikipediaTheme.colors.paperColor
                )
            )
        }
    )
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
