package org.wikipedia.settings.homefeed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme

class HomeFeedSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                val navController = rememberNavController()
                HomeFeedSettingsNavHost(
                    navController = navController,
                    onExit = {
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, HomeFeedSettingsActivity::class.java)
    }
}
