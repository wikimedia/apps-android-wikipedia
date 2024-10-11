package org.wikipedia.donate

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityDonorHistoryBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import kotlin.getValue

class DonorHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityDonorHistoryBinding
    private val viewModel: DonorHistoryViewModel by viewModels { DonorHistoryViewModel.Factory(intent.extras!!) }

    private var donorStatusList = arrayOf(
        getString(R.string.donor_history_update_donor_status_donor),
        getString(R.string.donor_history_update_donor_status_not_a_donor)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonorHistoryBinding.inflate(layoutInflater)

        init()
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

        binding.donateButton.setOnClickListener {
            launchDonateDialog()
        }

        binding.experimentLink.setOnClickListener {
            CustomTabsUtil.openInCustomTab(this, getString(R.string.contributions_dashboard_wiki_url))
        }
    }

    private fun updateDonorStatusText() {
        var donorStatusTextColor = R.attr.primary_color
        val donorStatusText = if (!Prefs.hasDonorHistorySaved && !viewModel.isDonor) {
            donorStatusTextColor = R.attr.placeholder_color
            R.string.donor_history_update_donor_status_default
        } else if (viewModel.isDonor) {
            R.string.donor_history_update_donor_status_donor
        } else {
            R.string.donor_history_update_donor_status_not_a_donor
        }
        binding.donorStatus.text = getString(donorStatusText)
        binding.donorStatus.setTextColor(ResourceUtil.getThemedColorStateList(this, donorStatusTextColor))
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
            binding.lastDonationDate.text = DateUtils.getRelativeTimeSpanString(DateUtil.iso8601DateParse(it).time, System.currentTimeMillis(), 0L)
        }
    }

    private fun showDonorStatusDialog() {
        MaterialAlertDialogBuilder(this)
            .setSingleChoiceItems(donorStatusList, -1) { dialog, which ->
                viewModel.isDonor = which == 0
                updateDonorStatusText()
                dialog.dismiss()
            }
            .show()
    }

    private fun showLastDonatedDatePicker() {
        DatePickerDialog(this, { _, year, month, day ->
            viewModel.lastDonated = "$year-${month + 1}-$day"
            updateLastDonatedText()
        }, 2024, 10, 10).show()
    }

    companion object {

        fun newIntent(context: Context, completedDonation: Boolean = false): Intent {
            return Intent(context, DonorHistoryActivity::class.java)
                .putExtra(Constants.ARG_BOOLEAN, completedDonation)
        }
    }
}
