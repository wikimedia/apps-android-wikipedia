package org.wikipedia.donate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityDonorHistoryBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ResourceUtil
import kotlin.getValue

class DonorHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityDonorHistoryBinding
    private val viewModel: DonorHistoryViewModel by viewModels { DonorHistoryViewModel.Factory(intent.extras!!) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonorHistoryBinding.inflate(layoutInflater)
    }

    private fun init() {

        binding.donationInfoContainer.isVisible = viewModel.isDonor

        var donorStatusTextColor = R.attr.primary_color
        val donorStatusText = if (!Prefs.hasDonorHistorySaved) {
            donorStatusTextColor = R.attr.placeholder_color
            R.string.donor_history_update_donor_status_default
        } else if (viewModel.isDonor) {
            R.string.donor_history_update_donor_status_donor
        } else {
            R.string.donor_history_update_donor_status_not_a_donor
        }
        binding.donorStatus.text = getString(donorStatusText)
        binding.donorStatus.setTextColor(ResourceUtil.getThemedColorStateList(this, donorStatusTextColor))

        var lastDonatedTextColor = R.attr.primary_color
        val lastDonatedText = if (viewModel.lastDonated == null) {
            lastDonatedTextColor = R.attr.placeholder_color
            R.string.donor_history_last_donated_hint
        } else {
            R.string.donor_history_last_donated
        }
        binding.lastDonationLabel.text = getString(lastDonatedText)
        binding.donorStatus.setTextColor(ResourceUtil.getThemedColorStateList(this, lastDonatedTextColor))

        binding.recurringDonorCheckbox.isChecked = viewModel.isRecurringDonor
    }

    companion object {
        fun newIntent(context: Context, completedDonation: Boolean = false): Intent {
            return Intent(context, DonorHistoryActivity::class.java)
                .putExtra(Constants.ARG_BOOLEAN, completedDonation)
        }
    }
}
