package org.wikipedia.readinglist.recommended

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.wikipedia.R
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.components.WikipediaAlertDialog
import org.wikipedia.compose.extensions.noRippleClickable
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun RecommendedReadingListSettingsScreen(
    modifier: Modifier = Modifier,
    uiState: RecommendedReadingListSettingsState,
    onBackButtonClick: () -> Unit,
    onRecommendedReadingListSourceClick: () -> Unit,
    onInterestClick: () -> Unit,
    onRecommendedReadingListSwitchClick: (Boolean) -> Unit,
    onNotificationStateChanged: (Boolean) -> Unit,
    onArticleNumberChanged: (Int) -> Unit,
    onUpdateFrequency: (RecommendedReadingListUpdateFrequency) -> Unit
) {
    var showAlertDialog by remember { mutableStateOf(false) }
    val isRecommendedReadingListEnabled = uiState.isRecommendedReadingListEnabled
    Scaffold(
        modifier = modifier,
        topBar = {
            WikiTopAppBar(
                title = stringResource(R.string.recommended_reading_list_settings_title),
                onNavigationClick = onBackButtonClick
            )
        },
        containerColor = WikipediaTheme.colors.paperColor,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            RecommendedReadingListSwitch(
                modifier = Modifier
                    .noRippleClickable {
                        if (isRecommendedReadingListEnabled) {
                            showAlertDialog = true
                            return@noRippleClickable
                        }
                        onRecommendedReadingListSwitchClick(true)
                    }
                    .padding(horizontal = 16.dp),
                isRecommendedReadingListEnabled = isRecommendedReadingListEnabled,
                onCheckedChange = {
                    if (isRecommendedReadingListEnabled) {
                        showAlertDialog = true
                        return@RecommendedReadingListSwitch
                    }
                    onRecommendedReadingListSwitchClick(it)
                }
            )
            if (!uiState.isRecommendedReadingListEnabled) {
                DisabledState(
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                EnabledState(
                    modifier = Modifier.padding(vertical = 16.dp),
                    articlesNumber = uiState.articlesNumber,
                    selectedFrequency = uiState.updateFrequency,
                    discoverSource = uiState.recommendedReadingListSource,
                    isNotificationEnabled = uiState.isRecommendedReadingListNotificationEnabled,
                    onArticleNumberChanged = onArticleNumberChanged,
                    onUpdateFrequency = onUpdateFrequency,
                    onDiscoverSourceClick = onRecommendedReadingListSourceClick,
                    onInterestClick = onInterestClick,
                    onNotificationStateChanged = onNotificationStateChanged
                )
            }
        }

        if (showAlertDialog) {
            WikipediaAlertDialog(
                title = stringResource(R.string.recommended_reading_list_settings_turn_off_dialog_title),
                message = stringResource(R.string.recommended_reading_list_settings_turn_off_dialog_message, uiState.updateFrequency.name.lowercase()),
                confirmButtonText = stringResource(R.string.recommended_reading_list_settings_notifications_dialog_positive_button),
                dismissButtonText = stringResource(R.string.recommended_reading_list_settings_notifications_dialog_negative_button),
                onDismissRequest = {
                    showAlertDialog = false
                },
                onConfirmButtonClick = {
                    showAlertDialog = false
                    onRecommendedReadingListSwitchClick(false)
                },
                onDismissButtonClick = {
                    showAlertDialog = false
                    onRecommendedReadingListSwitchClick(true)
                }
            )
        }
    }
}

@Composable
private fun DisabledState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.recommended_reading_list_settings_toggle_disable_message),
            style = MaterialTheme.typography.bodySmall,
            color = WikipediaTheme.colors.secondaryColor
        )
    }
}

@Composable
private fun EnabledState(
    articlesNumber: Int,
    selectedFrequency: RecommendedReadingListUpdateFrequency,
    discoverSource: RecommendedReadingListSource,
    isNotificationEnabled: Boolean,
    onUpdateFrequency: (RecommendedReadingListUpdateFrequency) -> Unit,
    onArticleNumberChanged: (Int) -> Unit,
    onDiscoverSourceClick: () -> Unit,
    onInterestClick: () -> Unit,
    onNotificationStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsSection {
            ArticlesNumberView(
                articlesNumber = articlesNumber,
                onArticleNumberChanged = onArticleNumberChanged
            )
            UpdatesFrequencyView(
                selectedFrequency = selectedFrequency,
                onUpdateFrequency = onUpdateFrequency
            )
        }

        SettingsSection {
            SourceView(
                modifier = Modifier
                    .clickable {
                        onDiscoverSourceClick()
                    }
                    .padding(vertical = 8.dp),
                source = discoverSource
            )
        }

        if (discoverSource == RecommendedReadingListSource.INTERESTS) {
            SettingsSection {
                InterestsView(
                    modifier = Modifier
                        .clickable {
                            onInterestClick()
                        }
                        .padding(vertical = 8.dp)
                )
            }
        }

        SettingsSection(
            canShowDivider = false
        ) {
            NotificationView(
                isNotificationEnabled = isNotificationEnabled,
                onNotificationStateChanged = onNotificationStateChanged
            )
        }
    }
}

@Composable
private fun ArticlesNumberView(
    articlesNumber: Int,
    onArticleNumberChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxArticleNumber: Int = 20,
    minArticleNumber: Int = 1,
) {
    var textFieldInput by remember { mutableStateOf(articlesNumber.toString()) }
    var hasUserEdited by remember { mutableStateOf(false) }
    if (!hasUserEdited) {
        hasUserEdited = true
        textFieldInput = ""
    }

    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(
            containerColor = WikipediaTheme.colors.paperColor
        ),
        leadingContent = {
            Icon(
                modifier = Modifier,
                painter = painterResource(R.drawable.newsstand_24dp),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = stringResource(R.string.recommended_reading_list_settings_articles)
            )
        },
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .size(width = 56.dp, height = 56.dp),
                    value = textFieldInput,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = { newValue ->
                        val intValue = newValue.toIntOrNull()
                        when {
                            newValue.isEmpty() -> {
                                textFieldInput = newValue
                                onArticleNumberChanged(minArticleNumber)
                            }
                            intValue != null && intValue < minArticleNumber -> {
                                textFieldInput = minArticleNumber.toString()
                                onArticleNumberChanged(minArticleNumber)
                            }
                            intValue != null && intValue > maxArticleNumber -> {
                                textFieldInput = maxArticleNumber.toString()
                                onArticleNumberChanged(maxArticleNumber)
                            }
                            intValue != null -> { // valid range
                                textFieldInput = newValue
                                onArticleNumberChanged(intValue)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WikipediaTheme.colors.primaryColor,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    placeholder = {
                        Text(articlesNumber.toString())
                    }
                )
                Text(
                    text = stringResource(R.string.recommended_reading_list_settings_articles),
                    style = WikipediaTheme.typography.p,
                    color = WikipediaTheme.colors.primaryColor
                )
            }
        }
    )
}

@Composable
private fun UpdatesFrequencyView(
    selectedFrequency: RecommendedReadingListUpdateFrequency,
    onUpdateFrequency: (RecommendedReadingListUpdateFrequency) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val frequencies = RecommendedReadingListUpdateFrequency.entries.toTypedArray()
    val dialogOptions = frequencies.map { context.getString(it.dialogStringRes) }
    val selectedDialogOption = stringResource(selectedFrequency.dialogStringRes)
    val updateFrequencyString = stringResource(
        R.string.recommended_reading_list_settings_updates_frequency,
        stringResource(selectedFrequency.displayStringRes)
    )
    ListItem(
        modifier = modifier
            .clickable(
                onClick = {
                    showDialog = true
                }
            ),
        colors = ListItemDefaults.colors(
            containerColor = WikipediaTheme.colors.paperColor
        ),
        leadingContent = {
            Icon(
                modifier = Modifier,
                painter = painterResource(R.drawable.refresh_24dp),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = stringResource(R.string.recommended_reading_list_settings_articles)
            )
        },
        headlineContent = {
            Text(
                text = updateFrequencyString,
                style = WikipediaTheme.typography.p,
                color = WikipediaTheme.colors.primaryColor
            )
        }
    )

    if (showDialog) {
        RadioListDialog(
            options = dialogOptions,
            selectedOption = selectedDialogOption,
            onDismissRequest = { showDialog = false },
            onOptionSelected = { selectedOptionString ->
                val newSelectedFrequency = frequencies.find {
                    context.getString(it.dialogStringRes) == selectedOptionString
                } ?: RecommendedReadingListUpdateFrequency.DAILY
                onUpdateFrequency(newSelectedFrequency)
            }
        )
    }
}

@Composable
private fun RecommendedReadingListSwitch(
    isRecommendedReadingListEnabled: Boolean,
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
                text = stringResource(R.string.recommended_reading_list_settings_toggle),
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.primaryColor
            )
        },
        trailingContent = {
            Switch(
                checked = isRecommendedReadingListEnabled,
                onCheckedChange = {
                    onCheckedChange(it)
                },
                colors = SwitchDefaults.colors(
                    uncheckedTrackColor = WikipediaTheme.colors.inactiveColor,
                    uncheckedThumbColor = WikipediaTheme.colors.borderColor,
                    uncheckedBorderColor = WikipediaTheme.colors.borderColor,
                    checkedTrackColor = WikipediaTheme.colors.progressiveColor,
                    checkedThumbColor = WikipediaTheme.colors.paperColor,
                    checkedBorderColor = WikipediaTheme.colors.borderColor
                )
            )
        }
    )
}

@Composable
private fun SourceView(
    source: RecommendedReadingListSource,
    modifier: Modifier = Modifier
) {
    val subtitle = when (source) {
        RecommendedReadingListSource.INTERESTS -> R.string.recommended_reading_list_settings_updates_base_subtitle_interests
        RecommendedReadingListSource.READING_LIST -> R.string.recommended_reading_list_settings_updates_base_subtitle_saved
        RecommendedReadingListSource.HISTORY -> R.string.recommended_reading_list_settings_updates_base_subtitle_history
    }
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(
            containerColor = WikipediaTheme.colors.paperColor
        ),
        leadingContent = {
            Icon(
                modifier = Modifier,
                painter = painterResource(R.drawable.build_24dp),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = stringResource(R.string.recommended_reading_list_settings_updates_base_title)
            )
        },
        headlineContent = {
            Text(
                text = stringResource(R.string.recommended_reading_list_settings_updates_base_title),
                style = WikipediaTheme.typography.p,
                color = WikipediaTheme.colors.primaryColor
            )
        },
        supportingContent = {
            Text(
                modifier = Modifier
                    .padding(top = 4.dp),
                text = stringResource(subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = WikipediaTheme.colors.secondaryColor
            )
        }
    )
}

@Composable
private fun InterestsView(
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(
            containerColor = WikipediaTheme.colors.paperColor
        ),
        leadingContent = {
            Icon(
                modifier = Modifier
                    .size(24.dp),
                painter = painterResource(R.drawable.interests_24dp),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = stringResource(R.string.recommended_reading_list_settings_updates_base_title)
            )
        },
        headlineContent = {
            Text(
                text = stringResource(R.string.recommended_reading_list_settings_interests),
                style = WikipediaTheme.typography.p,
                color = WikipediaTheme.colors.primaryColor
            )
        }
    )
}

@Composable
private fun NotificationView(
    modifier: Modifier = Modifier,
    isNotificationEnabled: Boolean,
    onNotificationStateChanged: (Boolean) -> Unit,
) {
    var showAlertDialog by remember { mutableStateOf(false) }
    val subtitle = if (isNotificationEnabled) stringResource(R.string.recommended_reading_list_settings_notification_subtitle_enable)
    else stringResource(R.string.recommended_reading_list_settings_notifications_subtitle_disable)
    ListItem(
        modifier = modifier
            .noRippleClickable {
                if (isNotificationEnabled) {
                    showAlertDialog = true
                } else
                    onNotificationStateChanged(true)
            },
        colors = ListItemDefaults.colors(
            containerColor = WikipediaTheme.colors.paperColor
        ),
        leadingContent = {
            Icon(
                modifier = Modifier
                    .size(24.dp),
                painter = painterResource(R.drawable.notifications_24dp),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = stringResource(R.string.recommended_reading_list_settings_updates_base_title)
            )
        },
        headlineContent = {
            Text(
                text = stringResource(R.string.recommended_reading_list_settings_notifications_title),
                style = WikipediaTheme.typography.p,
                color = WikipediaTheme.colors.primaryColor
            )
        },
        supportingContent = {
            Text(
                modifier = Modifier
                    .padding(top = 4.dp),
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = WikipediaTheme.colors.secondaryColor
            )
        },
        trailingContent = {
            Switch(
                checked = isNotificationEnabled,
                onCheckedChange = {
                    if (isNotificationEnabled) {
                        showAlertDialog = true
                    } else
                        onNotificationStateChanged(it)
                },
                colors = SwitchDefaults.colors(
                    uncheckedTrackColor = WikipediaTheme.colors.inactiveColor,
                    uncheckedThumbColor = WikipediaTheme.colors.borderColor,
                    uncheckedBorderColor = WikipediaTheme.colors.borderColor,
                    checkedTrackColor = WikipediaTheme.colors.progressiveColor,
                    checkedThumbColor = WikipediaTheme.colors.paperColor,
                    checkedBorderColor = WikipediaTheme.colors.borderColor
                )
            )
        }
    )

    if (showAlertDialog) {
        WikipediaAlertDialog(
            title = stringResource(R.string.recommended_reading_list_settings_notifications_dialog_title),
            message = stringResource(R.string.recommended_reading_list_settings_notifications_dialog_message),
            confirmButtonText = stringResource(R.string.recommended_reading_list_settings_notifications_dialog_positive_button),
            dismissButtonText = stringResource(R.string.recommended_reading_list_settings_notifications_dialog_negative_button),
            onDismissRequest = {
                showAlertDialog = false
            },
            onConfirmButtonClick = {
                showAlertDialog = false
                onNotificationStateChanged(false)
            },
            onDismissButtonClick = {
                showAlertDialog = false
                onNotificationStateChanged(true)
            }
        )
    }
}

@Composable
fun RadioListDialog(
    modifier: Modifier = Modifier,
    options: List<String>,
    selectedOption: String,
    onDismissRequest: () -> Unit,
    onOptionSelected: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        content = {
            LazyColumn(
                modifier = modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(WikipediaTheme.colors.paperColor)
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(options) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDismissRequest()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = {
                                onOptionSelected(option)
                                onDismissRequest()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = WikipediaTheme.colors.progressiveColor
                            )
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    canShowDivider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        content = {
            content()
            if (canShowDivider) {
                HorizontalDivider()
            }
        }
    )
}

@Preview
@Composable
private fun RecommendedReadingListSettingsScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        RecommendedReadingListSettingsScreen(
            uiState = RecommendedReadingListSettingsState(
                isRecommendedReadingListEnabled = true,
                articlesNumber = 1,
                updateFrequency = RecommendedReadingListUpdateFrequency.WEEKLY,
                recommendedReadingListSource = RecommendedReadingListSource.INTERESTS,
                isRecommendedReadingListNotificationEnabled = true
            ),
            onRecommendedReadingListSourceClick = {},
            onRecommendedReadingListSwitchClick = {},
            onInterestClick = {},
            onUpdateFrequency = {},
            onArticleNumberChanged = {},
            onBackButtonClick = {},
            onNotificationStateChanged = {}
        )
    }
}
