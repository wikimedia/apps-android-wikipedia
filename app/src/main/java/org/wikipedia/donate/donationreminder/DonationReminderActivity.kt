package org.wikipedia.donate.donationreminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.util.DeviceUtil

class DonationReminderActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)
        setContent {
            BaseTheme {
                DonationReminderScreen(
                    onBackButtonClick = {
                        onBackPressed()
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(contex: Context): Intent {
            return Intent(contex, DonationReminderActivity::class.java)
        }
    }
}
