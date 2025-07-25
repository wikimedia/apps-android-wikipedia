package org.wikipedia.donate.donationreminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.donate.DonationConfigHelper
import org.wikipedia.donate.DonationReminderHelper
import org.wikipedia.donate.GooglePayComponent
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class DonationReminderViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DonationReminderUiState())
    val uiState: StateFlow<DonationReminderUiState> = _uiState.asStateFlow()

    val currentCountryCode = DonationReminderHelper.currentCountryCode
    val currencyFormat = DonationReminderHelper.currencyFormat
    val currencySymbol get() = currencyFormat.currency?.getSymbol() ?: "$"
    val currencyCode get() = currencyFormat.currency?.currencyCode ?: GooglePayComponent.CURRENCY_FALLBACK

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
                    isDonationReminderEnabled = Prefs.isDonationRemindersEnabled
                )
            }
        }
    }

    fun saveReminder() {
        with(_uiState.value) {
            Prefs.donationRemindersAmount = donationAmount.selectedValue
            Prefs.donationRemindersReadFrequency = readFrequency.selectedValue
        }
    }

    fun updateDonationAmountState(donationAmount: Int) {
        _uiState.update { it.copy(donationAmount = it.donationAmount.copy(selectedValue = donationAmount)) }
    }

    fun updateReadFrequencyState(readFrequency: Int) {
        _uiState.update { it.copy(readFrequency = it.readFrequency.copy(selectedValue = readFrequency)) }
    }

    fun toggleDonationReminders(enabled: Boolean) {
        Prefs.isDonationRemindersEnabled = enabled
        _uiState.update { it.copy(isDonationReminderEnabled = enabled) }
    }

    private fun createReadFrequencyOptions(): SelectableOption {
        val options = listOf(2, 10, 15)
        val optionItems = options.map {
            OptionItem.Preset(it, "$it articles")
        } + OptionItem.Custom

        val selectedValue = if (Prefs.donationRemindersReadFrequency < 0) options.first()
        else Prefs.donationRemindersReadFrequency

        return SelectableOption(
            selectedValue,
            optionItems,
            minimumAmount = 1f,
            maximumAmount = 1000f
        ) { "$it articles" }
    }

    private suspend fun createDonationAmountOptions(): SelectableOption {
        val donationConfig = DonationConfigHelper.getConfig()
        val minimumAmount = donationConfig?.currencyMinimumDonation?.get(currencyCode) ?: 0f

        var maximumAmount = donationConfig?.currencyMaximumDonation?.get(currencyCode) ?: 0f
        if (maximumAmount == 0f) {
            val defaultMin = donationConfig?.currencyMinimumDonation?.get(GooglePayComponent.CURRENCY_FALLBACK) ?: 0f
            if (defaultMin > 0f) {
                maximumAmount = (donationConfig?.currencyMinimumDonation?.get(currencyCode) ?: 0f) / defaultMin *
                        (donationConfig?.currencyMaximumDonation?.get(GooglePayComponent.CURRENCY_FALLBACK) ?: 0f)
            }
        }

        val presets = DonationReminderHelper.currencyAmountPresets[currentCountryCode] ?: listOf(0)
        val options = presets.map {
            OptionItem.Preset(it, currencyFormat.format(it))
        } + OptionItem.Custom

        val selectedValue = if (Prefs.donationRemindersAmount < 0) presets.first()
        else Prefs.donationRemindersAmount

        return SelectableOption(
            selectedValue,
            options,
            minimumAmount = minimumAmount,
            maximumAmount = maximumAmount,
            displayFormatter = currencyFormat::format
        )
    }

    fun getAmountFloat(text: String): Float {
        var result: Float?
        result = text.toFloatOrNull()
        if (result == null) {
            val text2 = if (text.contains(".")) text.replace(".", ",") else text.replace(",", ".")
            result = text2.toFloatOrNull()
        }
        return result ?: 0f
    }
}

data class DonationReminderUiState(
    val isDonationReminderEnabled: Boolean = Prefs.isDonationRemindersEnabled,
    val readFrequency: SelectableOption = SelectableOption(Prefs.donationRemindersReadFrequency, emptyList()),
    val donationAmount: SelectableOption = SelectableOption(Prefs.donationRemindersAmount, emptyList()),
)

sealed class OptionItem(val displayText: String) {
    data class Preset(val value: Int, val text: String) : OptionItem(text)
    object Custom : OptionItem("Custom...")
}

data class SelectableOption(
    val selectedValue: Int,
    val options: List<OptionItem>,
    val maximumAmount: Float = 0f,
    val minimumAmount: Float = 0f,
    val displayFormatter: (Any) -> String = { it.toString() }
)
