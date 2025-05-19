package org.wikipedia.settings.discover

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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import org.wikipedia.R
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.extensions.noRippleClickable
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.readinglist.recommended.RecommendedReadingListSource
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoverSettingsViewModel = viewModel(),
    onBackButtonClick: () -> Unit,
    onDiscoverSourceClick: () -> Unit,
    onInterestClick: () -> Unit,
    onNotificationDialogButtonClicked: (Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAlertDialog by remember { mutableStateOf(false) }
    val isDiscoverReadingOn = uiState.isRecommendedReadingListEnabled
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
                .padding(horizontal = 16.dp)
        ) {
            DiscoverReadingListSwitch(
                modifier = Modifier.noRippleClickable {
                    if (!isDiscoverReadingOn) {
                        showAlertDialog = true
                        return@noRippleClickable
                    }
                    viewModel.toggleRecommendedReadingList(true)
                },
                isDiscoverReadingOn = isDiscoverReadingOn,
                onCheckedChange = {
                    if (!isDiscoverReadingOn) {
                        showAlertDialog = true
                        return@DiscoverReadingListSwitch
                    }
                    viewModel.toggleRecommendedReadingList(it)
                }
            )
            if (!uiState.isRecommendedReadingListEnabled) {
                DiscoverSettingsOffState()
            } else {
                DiscoverSettingsOnState(
                    onCheckedChange = {
                        viewModel.toggleRecommendedReadingList(it)
                    },
                    articlesNumber = uiState.articlesNumber,
                    selectedFrequency = uiState.updateFrequency,
                    discoverSource = uiState.recommendedReadingListSource,
                    isNotificationEnabled = uiState.isRecommendedReadingListNotificationEnabled,
                    onArticleNumberChanged = {
                        viewModel.updateArticleNumberForRecommendingReadingList(it)
                    },
                    onUpdateFrequency = {
                        viewModel.updateFrequency(it)
                    },
                    onDiscoverSourceClick = onDiscoverSourceClick,
                    onInterestClick = onInterestClick,
                    onNotificationDialogButtonClicked = {
                        onNotificationDialogButtonClicked(it)
                        viewModel.toggleNotification(it)
                    }
                )
            }
        }

        if (showAlertDialog) {
            AlertDialog(
                title = {
                    Text(text = stringResource(R.string.recommended_reading_list_settings_turn_off_dialog_title))
                },
                text = {
                    Text(text = stringResource(R.string.recommended_reading_list_settings_turn_off_dialog_message))
                },
                onDismissRequest = {
                    showAlertDialog = false
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showAlertDialog = false
                            viewModel.toggleRecommendedReadingList(false)
                        }
                    ) {
                        Text(stringResource(R.string.recommended_reading_list_settings_notifications_dialog_positive_button))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAlertDialog = false
                            viewModel.toggleRecommendedReadingList(true)
                        }
                    ) {
                        Text(stringResource(R.string.recommended_reading_list_settings_notifications_dialog_negative_button))
                    }
                }
            )
        }
    }
}

@Composable
fun DiscoverSettingsOffState(
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
fun DiscoverSettingsOnState(
    articlesNumber: Int,
    selectedFrequency: RecommendedReadingListUpdateFrequency,
    discoverSource: RecommendedReadingListSource,
    isNotificationEnabled: Boolean,
    onUpdateFrequency: (RecommendedReadingListUpdateFrequency) -> Unit,
    onCheckedChange: ((Boolean) -> Unit),
    onArticleNumberChanged: (Int) -> Unit,
    onDiscoverSourceClick: () -> Unit,
    onInterestClick: () -> Unit,
    onNotificationDialogButtonClicked: (Boolean) -> Unit,
    modifier: Modifier = Modifier

) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
            DiscoverSourceView(
                source = discoverSource,
                onDiscoverSourceClick = onDiscoverSourceClick
            )
        }

        if (discoverSource == RecommendedReadingListSource.INTERESTS) {
            SettingsSection {
                InterestsSourceView(
                    onInterestClick = onInterestClick
                )
            }
        }

        SettingsSection(
            canShowDivider = false
        ) {
            DiscoverNotificationView(
                isNotificationEnabled = isNotificationEnabled,
                onNotificationDialogButtonClicked = onNotificationDialogButtonClicked
            )
        }
    }
}

@Composable
fun ArticlesNumberView(
    articlesNumber: Int,
    onArticleNumberChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxArticleNumber: Int = 20,
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
                                onArticleNumberChanged(0)
                            }
                            intValue != null && intValue <= maxArticleNumber -> {
                                textFieldInput = newValue
                                onArticleNumberChanged(intValue)
                            }
                            intValue != null && intValue > maxArticleNumber -> {
                                textFieldInput = maxArticleNumber.toString()
                                onArticleNumberChanged(maxArticleNumber)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
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
        },

    )
}

@Composable
fun UpdatesFrequencyView(
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
            .noRippleClickable(
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
fun DiscoverReadingListSwitch(
    isDiscoverReadingOn: Boolean,
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
                checked = isDiscoverReadingOn,
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
fun DiscoverSourceView(
    source: RecommendedReadingListSource,
    onDiscoverSourceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier
            .clickable {
                onDiscoverSourceClick()
            },
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
                text = stringResource(source.type),
                style = MaterialTheme.typography.bodySmall,
                color = WikipediaTheme.colors.secondaryColor
            )
        }
    )
}

@Composable
fun InterestsSourceView(
    modifier: Modifier = Modifier,
    onInterestClick: () -> Unit
) {
    ListItem(
        modifier = modifier
            .clickable {
                onInterestClick()
            },
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
fun DiscoverNotificationView(
    modifier: Modifier = Modifier,
    isNotificationEnabled: Boolean,
    onNotificationDialogButtonClicked: (Boolean) -> Unit,
) {
    var showAlertDialog by remember { mutableStateOf(false) }
    var checked by remember(isNotificationEnabled) { mutableStateOf(isNotificationEnabled) }
    val subtitle = if (isNotificationEnabled) stringResource(R.string.recommended_reading_list_settings_notification_subtitle_enable)
    else stringResource(R.string.recommended_reading_list_settings_notifications_subtitle_disable)
    ListItem(
        modifier = modifier
            .noRippleClickable {
                if (!isNotificationEnabled) {
                    onNotificationDialogButtonClicked(true)
                    return@noRippleClickable
                }
                showAlertDialog = true
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
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = WikipediaTheme.colors.secondaryColor
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    showAlertDialog = true
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
        AlertDialog(
            title = {
                Text(text = stringResource(R.string.recommended_reading_list_settings_notifications_dialog_title))
            },
            text = {
                Text(text = stringResource(R.string.recommended_reading_list_settings_notifications_dialog_message))
            },
            onDismissRequest = {
                showAlertDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAlertDialog = false
                        onNotificationDialogButtonClicked(false)
                    }
                ) {
                    Text(stringResource(R.string.recommended_reading_list_settings_notifications_dialog_positive_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAlertDialog = false
                        onNotificationDialogButtonClicked(true)
                    }
                ) {
                    Text(stringResource(R.string.recommended_reading_list_settings_notifications_dialog_negative_button))
                }
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
                            .noRippleClickable {
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
private fun SettingsSection(
    canShowDivider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            content()
            if (canShowDivider) {
                HorizontalDivider()
            }
        }
    )
}
