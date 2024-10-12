package org.wikipedia.donate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityDonorHistoryBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.ResourceUtil
import java.time.LocalDateTime
import java.time.ZoneOffset
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
            val millis = LocalDateTime.parse(it).toInstant(ZoneOffset.UTC).toEpochMilli()
            binding.lastDonationDate.text = DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), 0L)
        }
    }

    private fun showDonorStatusDialog() {
        val donorStatusList = arrayOf(
            getString(R.string.donor_history_update_donor_status_donor),
            getString(R.string.donor_history_update_donor_status_not_a_donor)
        )
        MaterialAlertDialogBuilder(this)
            .setSingleChoiceItems(donorStatusList, -1) { dialog, which ->
                viewModel.isDonor = which == 0
                updateDonorStatusText(true)
                updateLastDonatedText()
                dialog.dismiss()
            }
            .show()
    }

    private fun showLastDonatedDatePicker() {
        // TODO: investigate why the theme is not being applied properly
        // TODO: use local time
        MaterialDatePicker.Builder.datePicker()
            .setTheme(com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar)
            .setSelection(LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli())
            .setInputMode(MaterialDatePicker.INPUT_MODE_TEXT)
            .build()
            .apply {
                addOnPositiveButtonClickListener {
                    viewModel.lastDonated = LocalDateTime.ofEpochSecond(it / 1000, 0, ZoneOffset.UTC).toString()
                    updateLastDonatedText()
                }
            }
            .show(supportFragmentManager, "date_picker")
    }

    companion object {

        const val RESULT_DONOR_HISTORY_SAVED = 1

        fun newIntent(context: Context, completedDonation: Boolean = false): Intent {
            return Intent(context, DonorHistoryActivity::class.java)
                .putExtra(Constants.ARG_BOOLEAN, completedDonation)
        }
    }
}
