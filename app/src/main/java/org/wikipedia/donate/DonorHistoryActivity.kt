package org.wikipedia.donate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
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
import org.wikipedia.main.MainActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.UriUtil
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.getValue

class DonorHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityDonorHistoryBinding
    private val viewModel: DonorHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonorHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.donor_history_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        init()
    }

    override fun onBackPressed() {
        if (viewModel.donorHistoryModified) {
            MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.edit_abandon_confirm))
                .setPositiveButton(getString(R.string.edit_abandon_confirm_yes)) { dialog, _ ->
                    dialog.dismiss()
                    if (viewModel.shouldGoBackToContributeTab) {
                        startActivity(MainActivity.newIntent(this).putExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB, true))
                    } else {
                        finish()
                    }
                }
                .setNegativeButton(getString(R.string.edit_abandon_confirm_no)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            return
        }
        super.onBackPressed()
    }

    private fun init() {

        binding.donationInfoContainer.isVisible = viewModel.isDonor

        binding.donorStatus.setOnClickListener {
            showDonorStatusDialog()
        }

        binding.lastDonationContainer.setOnClickListener {
            showLastDonatedDatePicker()
        }

        binding.recurringDonorCheckbox.isChecked = viewModel.isRecurringDonor
        binding.recurringDonorCheckbox.setOnClickListener {
            viewModel.donorHistoryModified = true
            viewModel.isRecurringDonor = binding.recurringDonorCheckbox.isChecked
            binding.recurringDonorCheckbox.isChecked = viewModel.isRecurringDonor
        }
        binding.recurringDonorContainer.setOnClickListener {
            binding.recurringDonorCheckbox.performClick()
        }

        binding.donateButton.setOnClickListener {
            launchDonateDialog()
        }

        binding.experimentLink.setOnClickListener {
            UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.contributions_dashboard_wiki_url)))
        }

        binding.saveButton.setOnClickListener {
            if (viewModel.donorHistoryModified) {
                viewModel.saveDonorHistory()
                if (viewModel.shouldGoBackToContributeTab) {
                    startActivity(
                        MainActivity.newIntent(this)
                            .putExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB, true)
                    )
                } else {
                    finish()
                }
            } else {
                finish()
            }
        }
        updateDonorStatusText()
        updateLastDonatedText()
    }

    private fun updateDonorStatusText() {
        var donorStatusTextColor = R.attr.primary_color
        val donorStatusText = if (!Prefs.hasDonorHistorySaved && viewModel.currentDonorStatus == -1) {
            donorStatusTextColor = R.attr.placeholder_color
            R.string.donor_history_update_donor_status_default
        } else if (viewModel.isDonor) {
            viewModel.currentDonorStatus = 0
            R.string.donor_history_update_donor_status_donor
        } else {
            viewModel.currentDonorStatus = 1
            R.string.donor_history_update_donor_status_not_a_donor
        }
        binding.donorStatus.text = getString(donorStatusText)
        binding.donorStatus.setTextColor(ResourceUtil.getThemedColorStateList(this, donorStatusTextColor))
        binding.donateButton.isVisible = viewModel.currentDonorStatus == 1 // Not a donor
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
        MaterialAlertDialogBuilder(this)
            .setSingleChoiceItems(donorStatusList, viewModel.currentDonorStatus) { dialog, which ->
                viewModel.isDonor = which == 0
                viewModel.currentDonorStatus = which
                viewModel.donorHistoryModified = true
                updateDonorStatusText()
                updateLastDonatedText()
                dialog.dismiss()
            }
            .show()
    }

    private fun showLastDonatedDatePicker() {
        // The CalendarConstraints handles date in UTC
        val utcMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()
        val defaultDatePickerMilli = viewModel.lastDonated?.let {
            viewModel.dateTimeToMilli(it)
        } ?: run {
            utcMillis
        }

        val calendarConstraints = CalendarConstraints.Builder()
            .setEnd(utcMillis)
            .setValidator(DateValidatorPointBackward.before(utcMillis))
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
                    viewModel.donorHistoryModified = true
                    updateLastDonatedText()
                }
            }
            .show(supportFragmentManager, "datePicker")
    }

    companion object {

        const val RESULT_GO_BACK_TO_CONTRIBUTE_TAB = "goBackToContributeTab"

        fun newIntent(context: Context, completedDonation: Boolean = false, goBackToContributeTab: Boolean = false): Intent {
            return Intent(context, DonorHistoryActivity::class.java)
                .putExtra(Constants.ARG_BOOLEAN, completedDonation)
                .putExtra(RESULT_GO_BACK_TO_CONTRIBUTE_TAB, goBackToContributeTab)
        }
    }
}
