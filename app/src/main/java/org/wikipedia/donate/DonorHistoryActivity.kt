package org.wikipedia.donate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityDonorHistoryBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.ResourceUtil
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.getValue

class DonorHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityDonorHistoryBinding
    private val viewModel: DonorHistoryViewModel by viewModels { DonorHistoryViewModel.Factory(intent.extras!!) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonorHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_donor_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_donor_history_save -> {
                viewModel.saveDonorHistory()
                setResult(RESULT_DONOR_HISTORY_SAVED)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun init() {

        binding.donationInfoContainer.isVisible = viewModel.isDonor

        updateDonorStatusText()
        binding.donorStatus.setOnClickListener {
            showDonorStatusDialog()
        }

        updateLastDonatedText()
        binding.lastDonationLabel.setOnClickListener {
            showLastDonatedDatePicker()
        }

        binding.recurringDonorCheckbox.isChecked = viewModel.isRecurringDonor
        binding.recurringDonorCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.isRecurringDonor = isChecked
        }

        binding.donateButton.isVisible = Prefs.hasDonorHistorySaved
        binding.donateButton.setOnClickListener {
            launchDonateDialog()
        }

        binding.experimentLink.setOnClickListener {
            CustomTabsUtil.openInCustomTab(this, getString(R.string.contributions_dashboard_wiki_url))
        }
    }

    private fun updateDonorStatusText(manualUpdate: Boolean = false) {
        var donorStatusTextColor = R.attr.primary_color
        val donorStatusText = if (!Prefs.hasDonorHistorySaved && !manualUpdate) {
            donorStatusTextColor = R.attr.placeholder_color
            R.string.donor_history_update_donor_status_default
        } else if (viewModel.isDonor) {
            R.string.donor_history_update_donor_status_donor
        } else {
            R.string.donor_history_update_donor_status_not_a_donor
        }
        binding.donorStatus.text = getString(donorStatusText)
        binding.donorStatus.setTextColor(ResourceUtil.getThemedColorStateList(this, donorStatusTextColor))
        binding.donateButton.isVisible = manualUpdate
        binding.donationInfoContainer.isVisible = viewModel.isDonor
    }

    private fun updateLastDonatedText() {
        binding.lastDonationDate.isVisible = viewModel.lastDonated != null
        var lastDonatedTextColor = R.attr.primary_color
        val lastDonatedText = if (viewModel.lastDonated == null) {
            lastDonatedTextColor = R.attr.placeholder_color
            R.string.donor_history_last_donated_hint
        } else {
            R.string.donor_history_last_donated
        }
        binding.lastDonationLabel.text = getString(lastDonatedText)
        binding.lastDonationLabel.setTextColor(ResourceUtil.getThemedColorStateList(this, lastDonatedTextColor))
        viewModel.lastDonated?.let {
            binding.lastDonationDate.text = DateUtils.getRelativeTimeSpanString(
                viewModel.dateTimeToMilli(it),
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS
            )
        }
    }

    private fun showDonorStatusDialog() {
        val donorStatusList = arrayOf(
            getString(R.string.donor_history_update_donor_status_donor),
            getString(R.string.donor_history_update_donor_status_not_a_donor)
        )
        val selectItem = if (!Prefs.hasDonorHistorySaved) {
            -1
        } else if (viewModel.isDonor) {
            0
        } else {
            1
        }
        MaterialAlertDialogBuilder(this)
            .setSingleChoiceItems(donorStatusList, selectItem) { dialog, which ->
                viewModel.isDonor = which == 0
                updateDonorStatusText(true)
                updateLastDonatedText()
                dialog.dismiss()
            }
            .show()
    }

    private fun showLastDonatedDatePicker() {
        val today = System.currentTimeMillis()
        val defaultDatePickerMilli = viewModel.lastDonated?.let {
            viewModel.dateTimeToMilli(it)
        } ?: run {
            today
        }
        val calendarConstraints = CalendarConstraints.Builder()
            .setEnd(today)
            .setValidator(DateValidatorPointBackward.before(today))
            .build()

        MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.MaterialDatePickerStyle)
            .setSelection(defaultDatePickerMilli)
            .setInputMode(MaterialDatePicker.INPUT_MODE_TEXT)
            .setCalendarConstraints(calendarConstraints)
            .build()
            .apply {
                addOnPositiveButtonClickListener {
                    // The date picker returns milliseconds in UTC timezone.
                    val utcDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC)
                    viewModel.lastDonated = ZonedDateTime.of(utcDate, ZoneId.systemDefault()).toLocalDateTime().toString()
                    updateLastDonatedText()
                }
            }
            .show(supportFragmentManager, "datePicker")
    }

    companion object {

        const val RESULT_DONOR_HISTORY_SAVED = 1

        fun newIntent(context: Context, completedDonation: Boolean = false): Intent {
            return Intent(context, DonorHistoryActivity::class.java)
                .putExtra(Constants.ARG_BOOLEAN, completedDonation)
        }
    }
}
