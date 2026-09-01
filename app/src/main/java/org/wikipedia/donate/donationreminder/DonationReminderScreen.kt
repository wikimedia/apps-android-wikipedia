package org.wikipedia.donate.donationreminder

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
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
    onConfirmButtonClick: (String) -> Unit,
    onFooterButtonClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onReportClick: () -> Unit
) {
    val uiState = viewModel.uiState.collectAsState().value
    var isNavigatingToExternalUrl by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (viewModel.isFromSettings && !isNavigatingToExternalUrl && viewModel.hasValueChanged()) {
                        viewModel.saveReminder()
                        val message = DonationReminderHelper.thankYouMessageForSettings()
                        onConfirmButtonClick(message)
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    isNavigatingToExternalUrl = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DonationReminderAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp)
                    .padding(horizontal = 16.dp),
                onBackButtonClick = onBackButtonClick,
                menuItems = listOf(
                    DonationReminderDropDownMenuItem(
                        text = stringResource(R.string.donation_reminders_settings_learn_more_button),
                        icon = R.drawable.ic_info_outline_black_24dp,
                        onClick = onLearnMoreClick
                    ),
                    DonationReminderDropDownMenuItem(
                        text = stringResource(R.string.donation_reminders_settings_report_button),
                        icon = R.drawable.ic_report_flag,
                        onClick = onReportClick
                    )
                )
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
            onConfirmButtonClick = onConfirmButtonClick,
            onFooterButtonClick = {
                isNavigatingToExternalUrl = true
                onFooterButtonClick()
            }
        )
    }
}

@Composable
fun DonationReminderAppBar(
    modifier: Modifier = Modifier,
    onBackButtonClick: () -> Unit,
    menuItems: List<DonationReminderDropDownMenuItem> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .clickable(onClick = onBackButtonClick),
                tint = WikipediaTheme.colors.primaryColor,
                painter = painterResource(R.drawable.ic_arrow_back_black_24dp),
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .weight(1f),
                text = stringResource(R.string.donation_reminders_settings_title),
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                ),
                color = WikipediaTheme.colors.primaryColor
            )
            if (menuItems.isNotEmpty()) {
                Box {
                    Icon(
                        modifier = Modifier
                            .clickable(onClick = { expanded = true }),
                        tint = WikipediaTheme.colors.primaryColor,
                        painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                        contentDescription = null
                    )
                    DropdownMenu(
                        containerColor = WikipediaTheme.colors.paperColor,
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        menuItems.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = WikipediaTheme.colors.primaryColor
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(item.icon),
                                        tint = WikipediaTheme.colors.primaryColor,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    item.onClick.invoke()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
            )
            Row(
                modifier = Modifier
                    .background(
                        color = WikipediaTheme.colors.paperColor,
                    )
                    .border(
                        width = 1.dp,
                        color = WikipediaTheme.colors.borderColor,
                        shape = RoundedCornerShape(size = 16.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(16.dp),
                    painter = painterResource(R.drawable.ic_experiment_24dp),
                    tint = WikipediaTheme.colors.inactiveColor,
                    contentDescription = null
                )

                Text(
                    text = stringResource(R.string.donation_reminders_beta_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = WikipediaTheme.colors.primaryColor
                )
            }
        }
    }
}

@Composable
fun DonationReminderContent(
    modifier: Modifier = Modifier,
    viewModel: DonationReminderViewModel,
    uiState: DonationReminderUiState,
    onConfirmButtonClick: (String) -> Unit,
    onFooterButtonClick: () -> Unit
) {
    val isDonationReminderEnabled = uiState.isDonationReminderEnabled
    var customAmountText by rememberSaveable {
        mutableStateOf(
            if (uiState.donationAmount.selectedSource is SelectedSource.Custom) {
                uiState.donationAmount.selectedValue.toString()
            } else {
                ""
            }
        )
    }
    var customErrorMessage by rememberSaveable { mutableStateOf("") }
    val donateGooglePayMinAmount = stringResource(R.string.donate_gpay_minimum_amount)
    val donateGooglePayMaxAmount = stringResource(R.string.donate_gpay_maximum_amount)

    fun customAmountErrorMessage(inputText: String): String {
        val parsedCustomAmount = DonateUtil.getAmountFloat(inputText)
        return when {
            inputText.isBlank() || parsedCustomAmount < uiState.donationAmount.minimumAmount -> {
                String.format(
                    donateGooglePayMinAmount,
                    uiState.donationAmount.displayFormatter(uiState.donationAmount.minimumAmount)
                )
            }
            parsedCustomAmount >= uiState.donationAmount.maximumAmount -> {
                String.format(
                    donateGooglePayMaxAmount,
                    uiState.donationAmount.displayFormatter(uiState.donationAmount.maximumAmount)
                )
            }
            else -> ""
        }
    }

    Column(
        modifier = modifier
            .imePadding()
            .navigationBarsPadding()
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
                    onOptionSelected = { option, source ->
                        when (option) {
                            is OptionItem.Preset -> {
                                viewModel.updateReadFrequencyState(option.value, source)
                            }

                            is OptionItem.Custom -> { }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                DonationAmountView(
                    option = uiState.donationAmount,
                    currencySymbol = DonateUtil.currencySymbol,
                    customErrorMessage = customErrorMessage,
                    onCustomTextChanged = { newValue ->
                        customAmountText = newValue
                        customErrorMessage = customAmountErrorMessage(newValue)
                        if (customErrorMessage != "" && viewModel.isFromSettings) {
                            // Keep the last valid amount in the field
                            viewModel.updateDonationAmountState(uiState.donationAmount.selectedValue, uiState.donationAmount.selectedSource)
                        }
                    },
                    onCustomTextFocusedEmpty = {
                        customAmountText = ""
                    },
                    onOptionSelected = { option, source ->
                        when (option) {
                            is OptionItem.Preset -> {
                                customErrorMessage = ""
                                viewModel.updateDonationAmountState(option.value, source)
                            }

                            is OptionItem.Custom -> {
                                if (option.displayText.isBlank() && viewModel.isFromSettings) {
                                    // Keep previous amount value but switch selected source to custom.
                                    viewModel.updateDonationAmountState(
                                        uiState.donationAmount.selectedValue,
                                        source
                                    )
                                    return@DonationAmountView
                                }
                                val customValue = DonateUtil.getAmountFloat(option.displayText)
                                viewModel.updateDonationAmountState(customValue, source)
                            }
                        }
                    }
                )
            }
        }
        if (WindowInsets.ime.getBottom(LocalDensity.current) <= 0) {
            if (uiState.isDonationReminderEnabled || !viewModel.isFromSettings) {
                if (!viewModel.isFromSettings) {
                    AppButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                        onClick = {
                            val isCustomSelected = uiState.donationAmount.selectedSource is SelectedSource.Custom
                            if (isCustomSelected) {
                                val customAmountError = customAmountErrorMessage(customAmountText)
                                if (customAmountError.isNotEmpty()) {
                                    customErrorMessage = customAmountError
                                    return@AppButton
                                }
                            }
                            viewModel.toggleDonationReminders(true)
                            viewModel.saveReminder()
                            val message = DonationReminderHelper.thankYouMessageForSettings()
                            onConfirmButtonClick(message)
                        },
                        content = {
                            Text(
                                stringResource(R.string.donation_reminders_settings_confirm_btn_label)
                            )
                        }
                    )
                }
            }

            val footerButtonText = if (viewModel.isFromSettings) stringResource(R.string.donation_reminders_settings_about_experiment_btn_label)
            else stringResource(R.string.donation_reminders_settings_no_thanks_btn_label)
            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                onClick = onFooterButtonClick,
                content = {
                    Text(
                        text = footerButtonText,
                        color = WikipediaTheme.colors.progressiveColor
                    )
                }
            )
        }
    }
}

@Composable
fun DonationAmountView(
    option: SelectableOption<Float>,
    currencySymbol: String,
    customErrorMessage: String,
    onCustomTextChanged: (String) -> Unit,
    onCustomTextFocusedEmpty: () -> Unit,
    onOptionSelected: (OptionItem<Float>, SelectedSource) -> Unit,
) {
    val initialCustomText = if (option.selectedSource is SelectedSource.Custom) {
        option.selectedValue.toString()
    } else ""

    val initialSelectedOption = if (option.selectedSource is SelectedSource.Custom) {
        OptionItem.Custom(initialCustomText)
    } else {
        OptionItem.Preset(option.selectedValue, option.displayFormatter(option.selectedValue))
    }

    var selectedOption by remember { mutableStateOf(initialSelectedOption) }
    var textFieldValue by remember { mutableStateOf(initialCustomText) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(customErrorMessage) {
        if (customErrorMessage.isNotEmpty()) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    LaunchedEffect(option.selectedSource) {
        if (option.selectedSource is SelectedSource.Custom) {
            focusRequester.requestFocus()
        }
    }

    OptionSelector(
        title = stringResource(R.string.donation_reminders_settings_amount_label),
        headerIcon = R.drawable.credit_card_heart_24,
        option = option,
        onOptionSelected = { option, source ->
            selectedOption = option
            textFieldValue = ""
            onOptionSelected(option, source)
            focusManager.clearFocus()
        },
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = if (selectedOption is OptionItem.Custom) textFieldValue else "",
        onValueChange = { newValue ->

            // read the value + source and send to viewmodel
            textFieldValue = newValue
            selectedOption = OptionItem.Custom( newValue)
            onOptionSelected(OptionItem.Custom( newValue), SelectedSource.Custom)
            onCustomTextChanged(newValue)
        },
        prefix = { Text(
            text = currencySymbol,
            style = MaterialTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.primaryColor
        ) },
        placeholder = {
            Text(
                text = stringResource(R.string.donation_reminders_settings_custom_amount_label),
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.placeholderColor
            )
        },
        trailingIcon = {
            if (customErrorMessage.isNotEmpty()) {
                Icon(
                    painter = painterResource(R.drawable.baseline_info_24),
                    tint = WikipediaTheme.colors.destructiveColor,
                    contentDescription = null
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = WikipediaTheme.colors.primaryColor,
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = WikipediaTheme.colors.primaryColor,
            errorTextColor = WikipediaTheme.colors.primaryColor
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
        isError = customErrorMessage.isNotEmpty(),
        supportingText = if (customErrorMessage.isNotEmpty()) {
            {
                Text(
                    text = customErrorMessage,
                    color = WikipediaTheme.colors.destructiveColor,
                )
            }
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                    if (textFieldValue.isEmpty()) {
                        onCustomTextFocusedEmpty()
                        onOptionSelected(OptionItem.Custom(""), SelectedSource.Custom)
                    }
                }
            }
    )
}

@Composable
fun ReadFrequencyView(
    option: SelectableOption<Int>,
    onOptionSelected: (OptionItem<Int>, SelectedSource) -> Unit,
) {
    OptionSelector(
        title = stringResource(R.string.donation_reminders_settings_article_frequency_label),
        headerIcon = R.drawable.newsstand_24dp,
        option = option,
        showInfo = true,
        showArticleLabel = true,
        onOptionSelected = { option, source ->
            onOptionSelected(option, source)
        }
    )
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

        Text(
            text = formattedString,
            style = MaterialTheme.typography.bodyMedium,
            color = WikipediaTheme.colors.primaryColor
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
fun <T : Number> OptionSelector(
    title: String,
    option: SelectableOption<T>,
    @DrawableRes headerIcon: Int,
    onOptionSelected: (OptionItem<T>, SelectedSource) -> Unit,
    showInfo: Boolean = false,
    showArticleLabel: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(headerIcon),
                tint = WikipediaTheme.colors.inactiveColor,
                contentDescription = null
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.primaryColor,
            )
            if (showInfo) {
                InfoTooltip(
                    modifier = Modifier,
                    plainTooltipText = stringResource(R.string.donation_reminders_settings_tooltip_info_label)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            option.options.forEachIndexed { index, currentOption ->
                if (currentOption is OptionItem.Preset) {
                    val isSelected = (option.selectedSource is SelectedSource.Preset) &&
                            (option.selectedSource.key == index)

                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) WikipediaTheme.colors.progressiveColor
                            else WikipediaTheme.colors.backgroundColor
                        ),
                        onClick = { onOptionSelected(currentOption, SelectedSource.Preset(index)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = currentOption.displayText,
                            color = if (isSelected) WikipediaTheme.colors.paperColor
                            else WikipediaTheme.colors.primaryColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            if (showArticleLabel) {
                Text(
                    text = stringResource(R.string.donation_reminders_settings_article_number_selection_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor,
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
                text = stringResource(R.string.donation_reminders_settings_option_title),
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
                modifier = Modifier
                    .padding(horizontal = 16.dp),
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

@Preview(showBackground = true)
@Composable
private fun DonationReminderAppBarPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        DonationReminderAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp),
            onBackButtonClick = {},
            menuItems = listOf(
                DonationReminderDropDownMenuItem(
                    text = "Learn more",
                    icon = R.drawable.ic_info_outline_black_24dp,
                    onClick = {}
                ),
                DonationReminderDropDownMenuItem(
                    text = "Problem with feature",
                    icon = R.drawable.ic_report_flag,
                    onClick = {}
                )
            )
        )
    }
}
