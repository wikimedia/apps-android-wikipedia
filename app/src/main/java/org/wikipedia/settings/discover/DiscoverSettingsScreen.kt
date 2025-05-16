package org.wikipedia.settings.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.wikipedia.R
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.extensions.noRippleClickable
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.readinglist.recommended.UpdateFrequency
import org.wikipedia.settings.Prefs

@Composable
fun DiscoverScreen(
    onBackButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDiscoverReadingOn by remember { mutableStateOf(Prefs.isRecommendedReadingListEnabled) }

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
            if (!isDiscoverReadingOn) {
                DiscoverSettingsOffState(
                    onCheckedChange = {
                        isDiscoverReadingOn = it
                        Prefs.isRecommendedReadingListEnabled = it
                    }
                )
            } else {
                DiscoverSettingsOnState(
                    onCheckedChange = {
                        isDiscoverReadingOn = it
                        Prefs.isRecommendedReadingListEnabled = it
                    }
                )
            }
        }
    }
}

@Composable
fun DiscoverSettingsOffState(
    onCheckedChange: ((Boolean) -> Unit),
    modifier: Modifier = Modifier

) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DiscoverReadingListSwitch(
            isDiscoverReadingOn = false,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = stringResource(R.string.recommended_reading_list_settings_toggle_disable_message),
            style = MaterialTheme.typography.bodySmall,
            color = WikipediaTheme.colors.secondaryColor
        )
    }
}

@Composable
fun DiscoverSettingsOnState(
    onCheckedChange: ((Boolean) -> Unit),
    modifier: Modifier = Modifier

) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DiscoverReadingListSwitch(
            isDiscoverReadingOn = true,
            onCheckedChange = onCheckedChange
        )
        ArticlesOption()
        UpdatesFrequencyView()
    }
}

@Composable
fun ArticlesOption(
    modifier: Modifier = Modifier
) {
    var articlesNumber by remember { mutableStateOf(Prefs.recommendedReadingListArticlesNumber.toString()) }

    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(
            containerColor = WikipediaTheme.colors.paperColor
        ),
        leadingContent = {
            Icon(
                modifier = Modifier
                    .size(24.dp),
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
                        .size(width = 41.dp, height = 56.dp),
                    value = articlesNumber,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        articlesNumber = it
                        Prefs.recommendedReadingListArticlesNumber = if (it.isNotEmpty()) it.toInt() else 0
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                    )
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
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(UpdateFrequency.fromInt(Prefs.recommendedReadingListUpdateFrequency)) }
    val options = listOf(
        stringResource(R.string.recommended_reading_list_settings_updates_frequency_dialog_daily),
        stringResource(R.string.recommended_reading_list_settings_updates_frequency_dialog_weekly),
        stringResource(R.string.recommended_reading_list_settings_updates_frequency_dialog_monthly)
    )
    val frequencyToString = mapOf(
        UpdateFrequency.DAILY to options[0],
        UpdateFrequency.WEEKLY to options[1],
        UpdateFrequency.MONTHLY to options[2]
    )

    val stringToFrequency = mapOf(
        options[0] to UpdateFrequency.DAILY,
        options[1] to UpdateFrequency.WEEKLY,
        options[2] to UpdateFrequency.MONTHLY
    )

    val updatesFrequencyString = mapOf(
        UpdateFrequency.DAILY to stringResource(R.string.recommended_reading_list_settings_updates_frequency_daily),
        UpdateFrequency.WEEKLY to stringResource(R.string.recommended_reading_list_settings_updates_frequency_weekly),
        UpdateFrequency.MONTHLY to stringResource(R.string.recommended_reading_list_settings_updates_frequency_monthly)
    )

    val updateFrequencyString = stringResource(R.string.recommended_reading_list_settings_updates_frequency, updatesFrequencyString[selectedFrequency] ?: "")
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
                modifier = Modifier
                    .size(24.dp),
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
            options = options,
            selectedOption = frequencyToString[selectedFrequency] ?: options[0],
            onDismissRequest = { showDialog = false },
            onOptionSelected = {
                val option = stringToFrequency[it]
                selectedFrequency = option ?: UpdateFrequency.DAILY
                Prefs.recommendedReadingListUpdateFrequency = selectedFrequency.ordinal
            }
        )
    }
}

@Composable
fun DiscoverReadingListSwitch(
    isDiscoverReadingOn: Boolean,
    onCheckedChange: ((Boolean) -> Unit),
    ) {
    var checked by remember { mutableStateOf(isDiscoverReadingOn) }

    ListItem(
        modifier = Modifier
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
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onCheckedChange(checked)
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
