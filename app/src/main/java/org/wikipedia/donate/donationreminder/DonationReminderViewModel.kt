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

    fun loadData() {
        val readFrequencyList = listOf(25, 50, 75, 100).map { DropDownOption.Regular(it) } + DropDownOption.Custom
        viewModelScope.launch {
            val donationConfig = DonationConfigHelper.getConfig()
            val presets = donationConfig?.currencyAmountPresets[currencyCode]
            presets?.let {
                val donationAmountList = it.map { amount -> DropDownOption.Regular(amount.toInt()) } + DropDownOption.Custom
                val selectedDonationAmount = if (Prefs.donationRemindersReadFrequency < 0) it.first().toInt() else Prefs.donationRemindersReadFrequency
                val selectedReadFrequency = if (Prefs.donationRemindersAmount < 0) it.first().toInt() else Prefs.donationRemindersAmount
                _uiState.update { currentState -> currentState.copy(
                    readFrequencyList = readFrequencyList,
                    donationAmountList = donationAmountList,
                    selectedDonationAmount = selectedDonationAmount,
                    selectedReadFrequency = selectedReadFrequency)
                }
            }
        }
    }

    fun confirmReminder() {
        Prefs.donationRemindersAmount = _uiState.value.selectedDonationAmount
        Prefs.donationRemindersReadFrequency = _uiState.value.selectedReadFrequency
    }

    fun updateDonationAmountState(donationAmount: Int) {
        _uiState.update { it.copy(selectedDonationAmount = donationAmount) }
    }

    fun updateReadFrequencyState(readFrequency: Int) {
        _uiState.update { it.copy(selectedReadFrequency = readFrequency) }
    }

    fun toggleDonationReminders(enabled: Boolean) {
        Prefs.isDonationRemindersEnabled = enabled
        _uiState.update { it.copy(isDonationReminderEnabled = enabled) }
    }
}

data class DonationReminderUiState(
    val isDonationReminderEnabled: Boolean = Prefs.isDonationRemindersEnabled,
    val readFrequencyList: List<DropDownOption> = listOf(),
    val donationAmountList: List<DropDownOption> = listOf(),
    val selectedReadFrequency: Int = Prefs.donationRemindersReadFrequency,
    val selectedDonationAmount: Int = Prefs.donationRemindersAmount
)

sealed class DropDownOption {
    data class Regular(val value: Int) : DropDownOption()
    object Custom : DropDownOption()
}
