package org.wikipedia.donate.donationreminder

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.InlinePosition
import org.wikipedia.compose.components.TextWithInlineElement
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.extensions.noRippleClickable
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.donate.DonateUtil
import org.wikipedia.theme.Theme

@Composable
fun DonationReminderScreen(
    modifier: Modifier = Modifier,
    viewModel: DonationReminderViewModel,
    wikiErrorClickEvents: WikiErrorClickEvents? = null,
    onBackButtonClick: () -> Unit,
    onConfirmBtnClick: (String) -> Unit,
    onAboutThisExperimentClick: () -> Unit
) {
    val uiState = viewModel.uiState.collectAsState().value

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (viewModel.isFromSettings) {
                        viewModel.saveReminder()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            WikiTopAppBar(
                title = stringResource(R.string.donation_reminders_settings_title),
                onNavigationClick = onBackButtonClick
            )
        },
        containerColor = WikipediaTheme.colors.paperColor,
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = WikipediaTheme.colors.progressiveColor,
                    trackColor = WikipediaTheme.colors.borderColor
                )
            }
            return@Scaffold
        }

        if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                WikiErrorView(
                    modifier = Modifier
                        .fillMaxWidth(),
                    caught = uiState.error,
                    errorClickEvents = wikiErrorClickEvents
                )
            }
            return@Scaffold
        }

        DonationReminderContent(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            viewModel = viewModel,
            uiState = uiState,
            onConfirmBtnClick = onConfirmBtnClick,
            onAboutThisExperimentClick = onAboutThisExperimentClick
        )
    }
}

@Composable
fun DonationReminderContent(
    modifier: Modifier = Modifier,
    viewModel: DonationReminderViewModel,
    uiState: DonationReminderUiState,
    onConfirmBtnClick: (String) -> Unit,
    onAboutThisExperimentClick: () -> Unit
) {
    val isDonationReminderEnabled = uiState.isDonationReminderEnabled
    var showReadFrequencyCustomDialog by remember { mutableStateOf(false) }
    var showDonationAmountCustomDialog by remember { mutableStateOf(false) }
    var customDialogErrorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f)
                .padding(16.dp)
        ) {
            DonationHeader()
            if (viewModel.isFromSettings) {
                DonationRemindersSwitch(
                    modifier = Modifier
                        .noRippleClickable {
                            viewModel.toggleDonationReminders(!isDonationReminderEnabled)
                        }
                        .padding(top = 24.dp),
                    isDonationRemindersEnabled = isDonationReminderEnabled,
                    onCheckedChange = { viewModel.toggleDonationReminders(it) }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (uiState.isDonationReminderEnabled || !viewModel.isFromSettings) {
                ReadFrequencyView(
                    option = uiState.readFrequency,
                    showReadFrequencyCustomDialog = showReadFrequencyCustomDialog,
                    customDialogErrorMessage = customDialogErrorMessage,
                    onOptionSelected = { option ->
                        when (option) {
                            is OptionItem.Preset -> {
                                viewModel.updateReadFrequencyState(option.value)
                            }

                            is OptionItem.Custom -> {
                                showReadFrequencyCustomDialog = true
                            }
                        }
                    },
                    onDismissRequest = {
                        showReadFrequencyCustomDialog = false
                        customDialogErrorMessage = ""
                    },
                    onDoneClick = { readFrequency ->
                        if (customDialogErrorMessage.isEmpty()) {
                            viewModel.updateReadFrequencyState(readFrequency.toInt())
                            showReadFrequencyCustomDialog = false
                        }
                    },
                    onValueChange = { value ->
                        val minimumAmount = uiState.readFrequency.minimumAmount
                        val maximumAmount = uiState.readFrequency.maximumAmount
                        val amount = DonateUtil.getAmountFloat(value)
                        customDialogErrorMessage = when {
                            amount <= minimumAmount -> {
                                context.getString(
                                    R.string.donation_reminders_settings_warning_min_amount,
                                    uiState.readFrequency.displayFormatter(minimumAmount + 1)
                                )
                            }
                            amount >= maximumAmount -> {
                                context.getString(
                                    R.string.donation_reminders_settings_warning_max_amount,
                                    uiState.readFrequency.displayFormatter(maximumAmount - 1)
                                )
                            }
                            else -> ""
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                DonationAmountView(
                    option = uiState.donationAmount,
                    showDonationAmountCustomDialog = showDonationAmountCustomDialog,
                    currencySymbol = DonateUtil.currencySymbol,
                    customDialogErrorMessage = customDialogErrorMessage,
                    onDismissRequest = {
                        showDonationAmountCustomDialog = false
                        customDialogErrorMessage = ""
                    },
                    onOptionSelected = { option ->
                        when (option) {
                            is OptionItem.Preset -> {
                                viewModel.updateDonationAmountState(option.value)
                            }

                            is OptionItem.Custom -> {
                                showDonationAmountCustomDialog = true
                            }
                        }
                    },
                    onDoneClick = { amount ->
                        if (customDialogErrorMessage.isEmpty()) {
                            viewModel.updateDonationAmountState(amount.toFloat())
                            showDonationAmountCustomDialog = false
                        }
                    },
                    onValueChange = { value ->
                        val amount = DonateUtil.getAmountFloat(value)
                        val minimumAmount = uiState.donationAmount.minimumAmount
                        val maximumAmount = uiState.donationAmount.maximumAmount
                        customDialogErrorMessage = when {
                            amount <= 0 && amount < minimumAmount -> {
                                context.getString(
                                    R.string.donate_gpay_minimum_amount,
                                    uiState.donationAmount.displayFormatter(minimumAmount)
                                )
                            }
                            maximumAmount > 0 && amount >= maximumAmount -> {
                                context.getString(
                                    R.string.donate_gpay_maximum_amount,
                                    uiState.donationAmount.displayFormatter(maximumAmount)
                                )
                            }
                            else -> ""
                        }
                    }
                )
            }
        }

        if (uiState.isDonationReminderEnabled || !viewModel.isFromSettings) {
            BottomContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                isFromSettings = viewModel.isFromSettings,
                onConfirmBtnClick = {
                    viewModel.toggleDonationReminders(true)
                    viewModel.saveReminder()
                    val message = DonationReminderHelper.thankYouMessageForSettings()
                    onConfirmBtnClick(message)
                },
                onAboutThisExperimentClick = onAboutThisExperimentClick
            )
        }
    }
}

@Composable
fun DonationAmountView(
    option: SelectableOption<Float>,
    currencySymbol: String,
    showDonationAmountCustomDialog: Boolean,
    customDialogErrorMessage: String,
    onOptionSelected: (OptionItem<Float>) -> Unit,
    onDismissRequest: () -> Unit,
    onDoneClick: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    OptionSelector(
        title = stringResource(R.string.donation_reminders_settings_amount_label),
        headerIcon = R.drawable.credit_card_heart_24,
        option = option,
        onOptionSelected = onOptionSelected
    )
    if (showDonationAmountCustomDialog) {
        CustomInputDialog(
            title = stringResource(R.string.donation_reminders_settings_amount_label),
            decimalEnabled = true,
            errorMessage = customDialogErrorMessage,
            onDismissRequest = onDismissRequest,
            prefix = {
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            onDoneClick = onDoneClick,
            onValueChange = onValueChange
        )
    }
}

@Composable
fun ReadFrequencyView(
    option: SelectableOption<Int>,
    showReadFrequencyCustomDialog: Boolean,
    customDialogErrorMessage: String,
    onOptionSelected: (OptionItem<Int>) -> Unit,
    onDismissRequest: () -> Unit,
    onDoneClick: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    OptionSelector(
        title = stringResource(R.string.donation_reminders_settings_article_frequency_label),
        headerIcon = R.drawable.newsstand_24dp,
        option = option,
        showInfo = true,
        onOptionSelected = onOptionSelected
    )
    if (showReadFrequencyCustomDialog) {
        CustomInputDialog(
            title = stringResource(R.string.donation_reminders_settings_article_frequency_label),
            errorMessage = customDialogErrorMessage,
            onDismissRequest = onDismissRequest,
            suffix = {
                Text(
                    text = stringResource(R.string.donation_reminders_settings_article_frequency_input_suffix_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            onDoneClick = onDoneClick,
            onValueChange = onValueChange
        )
    }
}

@Composable
fun DonationHeader(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        val rawString = stringResource(R.string.donation_reminders_settings_thank_you_message)
        val formattedString = rawString.replace("%%", "%")
        TextWithInlineElement(
            text = formattedString,
            position = InlinePosition.END,
            placeholder = Placeholder(
                width = 20.sp,
                height = 20.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            ),
            content = {
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(start = 4.dp),
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = WikipediaTheme.colors.destructiveColor
                )
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.donation_reminders_settings_donation_info),
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
fun BottomContent(
    modifier: Modifier = Modifier,
    onConfirmBtnClick: () -> Unit,
    isFromSettings: Boolean,
    onAboutThisExperimentClick: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        if (!isFromSettings) {
            AppButton(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = onConfirmBtnClick,
                content = {
                    Text(
                        stringResource(R.string.donation_reminders_settings_confirm_btn_label)
                    )
                }
            )
        }

        TextButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = onAboutThisExperimentClick,
            content = {
                Text(
                    text = stringResource(R.string.donation_reminders_settings_about_experiment_btn_label),
                    color = WikipediaTheme.colors.progressiveColor
                )
            }
        )
    }
}

@Composable
fun <T : Number>OptionSelector(
    title: String,
    option: SelectableOption<T>,
    @DrawableRes headerIcon: Int,
    onOptionSelected: (OptionItem<T>) -> Unit,
    showInfo: Boolean = false,
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val displayValue by remember(option.selectedValue, option.displayFormatter) {
        derivedStateOf { option.displayFormatter(option.selectedValue) }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            modifier = Modifier
                .padding(top = 3.dp),
            painter = painterResource(headerIcon),
            tint = WikipediaTheme.colors.primaryColor,
            contentDescription = null
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.primaryColor,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextField(
                    modifier = Modifier
                        .width(210.dp)
                        .clickable { isDropdownExpanded = true },
                    value = displayValue,
                    enabled = false,
                    onValueChange = {},
                    colors = TextFieldDefaults.colors(
                        disabledContainerColor = WikipediaTheme.colors.backgroundColor,
                        disabledTextColor = WikipediaTheme.colors.primaryColor
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = null
                        )
                    }
                )

                if (showInfo) {
                    InfoTooltip(
                        modifier = Modifier,
                        plainTooltipText = stringResource(R.string.donation_reminders_settings_tooltip_info_label)
                    )
                }

                DropdownMenu(
                    modifier = Modifier
                        .width(210.dp),
                    containerColor = WikipediaTheme.colors.backgroundColor,
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    content = {
                        option.options.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayText,
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
                text = stringResource(R.string.donation_reminders_settings_title),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoTooltip(
    modifier: Modifier = Modifier,
    plainTooltipText: String
) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                containerColor = WikipediaTheme.colors.primaryColor,
                content = {
                    Text(
                        text = plainTooltipText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WikipediaTheme.colors.paperColor
                    )
                }
            )
        },
        state = tooltipState,
        content = {
            Icon(
                modifier = Modifier
                    .noRippleClickable(onClick = {
                        scope.launch {
                            tooltipState.show()
                        }
                    }),
                painter = painterResource(R.drawable.ic_info_outline_black_24dp),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = null
            )
        }
    )
}

@Composable
fun CustomInputDialog(
    modifier: Modifier = Modifier,
    title: String,
    decimalEnabled: Boolean = false,
    errorMessage: String = "",
    onDoneClick: (String) -> Unit,
    onDismissRequest: () -> Unit,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    onValueChange: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                modifier = modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(WikipediaTheme.colors.paperColor)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
                OutlinedTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester),
                    value = value,
                    singleLine = true,
                    onValueChange = { newValue ->
                        onValueChange(newValue)
                        value = newValue
                    },
                    isError = errorMessage.isNotEmpty(),
                    prefix = prefix,
                    suffix = suffix,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(keyboardType = if (decimalEnabled) KeyboardType.Number else KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WikipediaTheme.colors.primaryColor,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = WikipediaTheme.colors.primaryColor,
                        errorTextColor = WikipediaTheme.colors.primaryColor,
                    ),
                    supportingText = {
                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                color = WikipediaTheme.colors.destructiveColor,
                            )
                        }
                    },
                    trailingIcon = if (errorMessage.isNotEmpty()) {
                        {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = WikipediaTheme.colors.destructiveColor
                            )
                        }
                    } else null
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            if (value.isEmpty()) {
                                onValueChange("")
                                return@TextButton
                            }
                            onDoneClick(value)
                        },
                        content = {
                            Text(
                                text = "Done",
                                color = WikipediaTheme.colors.progressiveColor
                            )
                        }
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun CustomInputDialogPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        CustomInputDialog(
            title = "Remind me to donate",
            onDoneClick = {},
            onDismissRequest = {},
            prefix = {
                Text(
                    text = "$",
                    modifier = Modifier.padding(start = 16.dp)
                )
            },
            suffix = {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 12.dp),
                    text = "articles"
                )
            },
            onValueChange = {}
        )
    }
}
