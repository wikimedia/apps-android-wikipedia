package org.wikipedia.donate.donationreminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.donate.DonationConfigHelper
import org.wikipedia.donate.GooglePayComponent
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
    val currencyCode get() = currencyFormat.currency?.currencyCode ?: GooglePayComponent.CURRENCY_FALLBACK
    val currencySymbol get() = currencyFormat.currency?.getSymbol() ?: "$"

    fun loadData() {
        viewModelScope.launch {
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

    fun confirmReminder() {
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
        val options = listOf(25, 50, 75, 100)
        val optionItems = options.map {
            OptionItem.Preset(it, "$it articles")
        } + OptionItem.Custom

        val selectedValue = if (Prefs.donationRemindersReadFrequency < 0) options.first()
        else Prefs.donationRemindersReadFrequency

        return SelectableOption(selectedValue, optionItems) { "$it articles" }
    }

    private suspend fun createDonationAmountOptions(): SelectableOption {
        val config = DonationConfigHelper.getConfig()
        val presets = config?.currencyAmountPresets[currencyCode] ?: listOf(5, 10, 15, 20)

        val options = presets.map {
            OptionItem.Preset(it.toInt(), currencyFormat.format(it.toInt()))
        } + OptionItem.Custom

        val selectedValue = if (Prefs.donationRemindersAmount < 0) presets.first().toInt()
        else Prefs.donationRemindersAmount

        return SelectableOption(selectedValue, options, currencyFormat::format)
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
    val displayFormatter: (Int) -> String = { it.toString() }
)
