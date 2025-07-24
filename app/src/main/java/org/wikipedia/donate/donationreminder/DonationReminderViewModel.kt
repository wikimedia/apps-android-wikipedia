package org.wikipedia.donate.donationreminder

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.wikipedia.donate.DonationReminderHelper
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import java.text.NumberFormat
import java.util.Locale

class DonationReminderViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DonationReminderUiState())
    val uiState: StateFlow<DonationReminderUiState> = _uiState.asStateFlow()

    private val currentCountryCode get() = GeoUtil.geoIPCountry.orEmpty()
    val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.Builder()
        .setLocale(Locale.getDefault()).setRegion(currentCountryCode).build()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    val currencySymbol get() = currencyFormat.currency?.getSymbol() ?: "$"

    fun loadData() {
        val readFrequencyOptions = createReadFrequencyOptions()
        val donationAmountOptions = createExperimentalDonationAmount()
        _uiState.update {
            it.copy(
                readFrequency = readFrequencyOptions,
                donationAmount = donationAmountOptions,
                isDonationReminderEnabled = Prefs.isDonationRemindersEnabled
            )
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
            maxNumber = options.last()
        ) { "$it articles" }
    }

    private fun createExperimentalDonationAmount(): SelectableOption {
        val presets = DonationReminderHelper.currencyAmountPresets[currentCountryCode] ?: listOf(0)
        val options = presets.map {
            OptionItem.Preset(it, currencyFormat.format(it))
        } + OptionItem.Custom

        val selectedValue = if (Prefs.donationRemindersAmount < 0) presets.first()
        else Prefs.donationRemindersAmount

        return SelectableOption(
            selectedValue,
            options,
            maxNumber = presets.last(),
            displayFormatter = currencyFormat::format
        )
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
    val maxNumber: Int = 0,
    val displayFormatter: (Int) -> String = { it.toString() }
)
