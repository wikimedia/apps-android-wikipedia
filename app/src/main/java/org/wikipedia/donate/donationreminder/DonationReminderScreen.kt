package org.wikipedia.donate.donationreminder

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    viewModel: DonationReminderViewModel = viewModel(),
    onBackButtonClick: () -> Unit,
) {
    val uiState = viewModel.uiState.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

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
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
                    .padding(16.dp)
            ) {
                TopContent()
                MainContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    uiState = uiState,
                    onDonationsReminderSwitchClick = { viewModel.toggleDonationReminders(it) },
                    currencyFormatter = { amount ->
                        viewModel.currencyFormat.format(amount)
                    },
                    onReadFrequencySelected = { option ->
                        when (option) {
                            is DropDownOption.Regular -> {
                                viewModel.updateReadFrequencyState(option.value)
                            }

                            DropDownOption.Custom -> {}
                        }
                    },
                    onDonationAmountSelected = { option ->
                        when (option) {
                            is DropDownOption.Regular -> {
                                viewModel.updateDonationAmountState(option.value)
                            }

                            DropDownOption.Custom -> {}
                        }
                    },
                    onInfoClick = {}
                )
            }

            if (uiState.isDonationReminderEnabled) {
                BottomContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                    onConfirmBtnClick = { viewModel.confirmReminder() },
                    onAboutThisExperimentClick = {}
                )
            }
        }
    }
}

@Composable
fun TopContent(
    modifier: Modifier = Modifier
) {
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
                .padding(top = 24.dp),
            color = WikipediaTheme.colors.borderColor
        )
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    uiState: DonationReminderUiState,
    onDonationsReminderSwitchClick: (Boolean) -> Unit,
    currencyFormatter: (Int) -> String,
    onReadFrequencySelected: (DropDownOption) -> Unit,
    onDonationAmountSelected: (DropDownOption) -> Unit,
    onInfoClick: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        DonationRemindersSwitch(
            isDonationRemindersEnabled = uiState.isDonationReminderEnabled,
            onCheckedChange = onDonationsReminderSwitchClick
        )
        if (uiState.isDonationReminderEnabled) {
            DonationReminderOption(
                modifier = modifier,
                headlineText = "When I read",
                selectedOption = uiState.selectedReadFrequency,
                dropdownOptions = uiState.readFrequencyList,
                headlineIcon = R.drawable.newsstand_24dp,
                onOptionSelected = onReadFrequencySelected,
                onInfoIconClick = onInfoClick,
                displayFormatter = { "$it articles" },
            )

            DonationReminderOption(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                headlineText = "Remind me to donate",
                selectedOption = uiState.selectedDonationAmount,
                dropdownOptions = uiState.donationAmountList,
                headlineIcon = R.drawable.credit_card_heart_24,
                displayFormatter = currencyFormatter,
                onOptionSelected = onDonationAmountSelected,
            )
        }
    }
}

@Composable
fun BottomContent(
    modifier: Modifier = Modifier,
    onConfirmBtnClick: () -> Unit,
    onAboutThisExperimentClick: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        AppButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = onConfirmBtnClick,
            content = {
                Text(
                    "Confirm Reminder"
                )
            }
        )
        AppTextButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = onAboutThisExperimentClick,
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
    @DrawableRes headlineIcon: Int,
    dropdownOptions: List<DropDownOption>,
    selectedOption: Int,
    displayFormatter: (Int) -> String,
    onOptionSelected: (DropDownOption) -> Unit,
    onInfoIconClick: (() -> Unit)? = null,
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextField(
                    modifier = Modifier
                        .width(210.dp)
                        .clickable { isDropdownExpanded = true },
                    value = displayFormatter(selectedOption),
                    enabled = false,
                    onValueChange = {},
                    colors = TextFieldDefaults.colors(
                        disabledContainerColor = WikipediaTheme.colors.backgroundColor,
                        disabledTextColor = WikipediaTheme.colors.primaryColor
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                )
                if (onInfoIconClick != null) {
                    Icon(
                        modifier = Modifier
                            .clickable(onClick = onInfoIconClick),
                        painter = painterResource(R.drawable.ic_info_outline_black_24dp),
                        contentDescription = null
                    )
                }

                DropdownMenu(
                    modifier = Modifier
                        .width(210.dp),
                    containerColor = WikipediaTheme.colors.backgroundColor,
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    content = {
                        dropdownOptions.forEach { option ->
                            val text = when (option) {
                                DropDownOption.Custom -> "Custom..."
                                is DropDownOption.Regular -> {
                                    displayFormatter(option.value)
                                }
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = WikipediaTheme.colors.primaryColor
                                    )
                                },
                                onClick = {
                                    onOptionSelected(option)
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                )
            }
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
