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
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.donate.DonationConfigHelper
import org.wikipedia.donate.DonateUtil
import org.wikipedia.donate.GooglePayComponent
import org.wikipedia.readinglist.recommended.RecommendedReadingListOnboardingActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class DonationReminderViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val maxArticleFrequencyLimit = 1000
    private val minArticleFrequencyLimit = 1
    val isFromSettings = savedStateHandle.get<Boolean>(RecommendedReadingListOnboardingActivity.EXTRA_FROM_SETTINGS) == true

    private val _uiState = MutableStateFlow(DonationReminderUiState())
    val uiState: StateFlow<DonationReminderUiState> = _uiState.asStateFlow()

    private val formatRegex = Regex("\\.00$")

    fun loadData() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            _uiState.update { it.copy(isLoading = false, error = throwable) }
        }) {
            val readFrequencyOptions = createReadFrequencyOptions()
            val donationAmountOptions = createDonationAmountOptions()
            _uiState.update {
                it.copy(
                    readFrequency = readFrequencyOptions,
                    donationAmount = donationAmountOptions,
                    isDonationReminderEnabled = Prefs.donationReminderConfig.isEnabled,
                    isLoading = false,
                    error = null
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

    fun getThankYouMessage(): String {
        val context = WikipediaApp.instance
        val donationAmount =
            DonateUtil.currencyFormat.format(Prefs.donationReminderConfig.donateAmount)
        val readFrequency = Prefs.donationReminderConfig.articleFrequency
        val articleNumber = context.resources.getQuantityString(R.plurals.donation_reminders_text_articles,
            readFrequency, readFrequency)
        val message = context.getString(R.string.donation_reminders_snacbkbar_confirmation_label, donationAmount, articleNumber)
        return message
    }

    fun updateDonationAmountState(donationAmount: Float) {
        _uiState.update { it.copy(donationAmount = it.donationAmount.copy(selectedValue = donationAmount)) }
    }

    fun updateReadFrequencyState(readFrequency: Int) {
        _uiState.update { it.copy(readFrequency = it.readFrequency.copy(selectedValue = readFrequency)) }
    }

    fun toggleDonationReminders(enabled: Boolean) {
        Prefs.donationReminderConfig = Prefs.donationReminderConfig.copy(isEnabled = enabled)
        _uiState.update { it.copy(isDonationReminderEnabled = enabled) }
    }

    private fun createReadFrequencyOptions(): SelectableOption<Int> {
        val context = WikipediaApp.instance
        val options = DonationReminderHelper.defaultReadFrequencyOptions
        val optionItems = options.map {
            OptionItem.Preset(it, context.resources.getQuantityString(R.plurals.donation_reminders_text_articles,
                it, it))
        } + OptionItem.Custom()

        val selectedValue = if (Prefs.donationReminderConfig.articleFrequency <= 0) options.first()
        else Prefs.donationReminderConfig.articleFrequency

        return SelectableOption(
            selectedValue,
            optionItems,
            minimumAmount = minArticleFrequencyLimit,
            maximumAmount = maxArticleFrequencyLimit,
            displayFormatter = {
                context.resources.getQuantityString(R.plurals.donation_reminders_text_articles,
                    it, it)
            }
        )
    }

    private suspend fun createDonationAmountOptions(): SelectableOption<Float> {
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
        } + OptionItem.Custom()

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
    val readFrequency: SelectableOption<Int> = SelectableOption(
        selectedValue = Prefs.donationReminderConfig.articleFrequency,
        options = emptyList(),
        maximumAmount = 1000,
        minimumAmount = 1
    ),
    val donationAmount: SelectableOption<Float> = SelectableOption(
        selectedValue = Prefs.donationReminderConfig.donateAmount,
        options = emptyList(),
        maximumAmount = 0f,
        minimumAmount = 0f
    ),
    val isLoading: Boolean = true,
    val error: Throwable? = null
)

sealed class OptionItem<T : Number>(val displayText: String) {
    data class Preset<T : Number>(val value: T, val text: String) : OptionItem<T>(text)
    class Custom<T : Number> : OptionItem<T>("Custom...")
}

data class SelectableOption<T : Number>(
    val selectedValue: T,
    val options: List<OptionItem<T>>,
    val maximumAmount: T,
    val minimumAmount: T,
    val displayFormatter: (T) -> String = { it.toString() }
)
