package org.wikipedia.donate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.Constants
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityDonorHistoryBinding

class DonorHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityDonorHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonorHistoryBinding.inflate(layoutInflater)
    }

    companion object {
        fun newIntent(context: Context, completedDonation: Boolean = false): Intent {
            return Intent(context, DonorHistoryActivity::class.java)
                .putExtra(Constants.ARG_BOOLEAN, completedDonation)
        }
    }
}
