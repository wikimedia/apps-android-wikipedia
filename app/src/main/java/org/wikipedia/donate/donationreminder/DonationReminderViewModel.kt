package org.wikipedia.donate.donationreminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.donate.DonationConfigHelper
import org.wikipedia.donate.DonateUtil
import org.wikipedia.donate.GooglePayComponent
import org.wikipedia.readinglist.recommended.RecommendedReadingListOnboardingActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class DonationReminderViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val isFromSettings = savedStateHandle.get<Boolean>(RecommendedReadingListOnboardingActivity.EXTRA_FROM_SETTINGS) == true

    private val _uiState = MutableStateFlow(DonationReminderUiState())
    val uiState: StateFlow<DonationReminderUiState> = _uiState.asStateFlow()

    private val formatRegex = Regex("\\.00$")

    fun loadData() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            val readFrequencyOptions = createReadFrequencyOptions()
            val donationAmountOptions = createDonationAmountOptions()
            _uiState.update {
                it.copy(
                    readFrequency = readFrequencyOptions,
                    donationAmount = donationAmountOptions,
                    isDonationReminderEnabled = Prefs.donationReminderConfig.isEnabled
                )
            }
        }
    }

    fun saveReminder() {
        with(_uiState.value) {
            Prefs.donationReminderConfig = Prefs.donationReminderConfig.copy(
                donateAmount = donationAmount.selectedValue,
                articleFrequency = readFrequency.selectedValue,
                setupTimestamp = System.currentTimeMillis()
            )
        }
    }

    fun updateDonationAmountState(donationAmount: Float) {
        _uiState.update { it.copy(donationAmount = it.donationAmount.copy(selectedValue = donationAmount)) }
    }

    fun updateReadFrequencyState(readFrequency: Float) {
        _uiState.update { it.copy(readFrequency = it.readFrequency.copy(selectedValue = readFrequency)) }
    }

    fun toggleDonationReminders(enabled: Boolean) {
        Prefs.donationReminderConfig = Prefs.donationReminderConfig.copy(isEnabled = enabled)
        _uiState.update { it.copy(isDonationReminderEnabled = enabled) }
    }

    private fun createReadFrequencyOptions(): SelectableOption {
        val options = DonationReminderHelper.defaultReadFrequencyOptions
        val optionItems = options.map {
            OptionItem.Preset(it, "${it.toInt()} articles")
        } + OptionItem.Custom

        val selectedValue = if (Prefs.donationReminderConfig.articleFrequency <= 0) options.first()
        else Prefs.donationReminderConfig.articleFrequency

        return SelectableOption(
            selectedValue,
            optionItems,
            minimumAmount = 1f,
            maximumAmount = 1000f
        ) {
            "${it.toInt()} articles"
        }
    }

    private suspend fun createDonationAmountOptions(): SelectableOption {
        val donationConfig = DonationConfigHelper.getConfig()
        val currencyCode = DonateUtil.currencyCode
        val currentCountryCode = DonateUtil.currentCountryCode
        val minimumAmount = donationConfig?.currencyMinimumDonation?.get(currencyCode) ?: 0f

        var maximumAmount = donationConfig?.currencyMaximumDonation?.get(currencyCode) ?: 0f
        if (maximumAmount == 0f) {
            val defaultMin = donationConfig?.currencyMinimumDonation?.get(GooglePayComponent.CURRENCY_FALLBACK) ?: 0f
            if (defaultMin > 0f) {
                maximumAmount = (donationConfig?.currencyMinimumDonation?.get(currencyCode) ?: 0f) / defaultMin *
                        (donationConfig?.currencyMaximumDonation?.get(GooglePayComponent.CURRENCY_FALLBACK) ?: 0f)
            }
        }

        val presets = DonationReminderHelper.currencyAmountPresets[currentCountryCode] ?: listOf(0f)
        val options = presets.map {
            OptionItem.Preset(it, DonateUtil.currencyFormat.format(it).replace(formatRegex, ""))
        } + OptionItem.Custom

        val selectedValue = if (Prefs.donationReminderConfig.donateAmount <= 0f) presets.first()
        else Prefs.donationReminderConfig.donateAmount

        return SelectableOption(
            selectedValue,
            options,
            minimumAmount = minimumAmount,
            maximumAmount = maximumAmount,
            displayFormatter = {
                DonateUtil.currencyFormat.format(it).replace(formatRegex, "")
            }
        )
    }
}

data class DonationReminderUiState(
    val isDonationReminderEnabled: Boolean = Prefs.donationReminderConfig.isEnabled,
    val readFrequency: SelectableOption = SelectableOption(Prefs.donationReminderConfig.articleFrequency, emptyList()),
    val donationAmount: SelectableOption = SelectableOption(Prefs.donationReminderConfig.donateAmount, emptyList()),
)

sealed class OptionItem(val displayText: String) {
    data class Preset(val value: Float, val text: String) : OptionItem(text)
    object Custom : OptionItem("Custom...")
}

data class SelectableOption(
    val selectedValue: Float,
    val options: List<OptionItem>,
    val maximumAmount: Float = 0f,
    val minimumAmount: Float = 0f,
    val displayFormatter: (Float) -> String = { it.toString() }
)
