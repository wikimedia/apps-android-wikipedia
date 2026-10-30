package org.wikipedia.settings.homefeed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.theme.BaseTheme

class HomeFeedSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _instrument = TestKitchenAdapter.client.getInstrument("apps-home-feed")
            .setDefaultActionSource("settings")

        val startDestination = intent.getStringExtra(EXTRA_START_DESTINATION)
            ?.let { HomeFeedSettingsStartDestination.valueOf(it) } ?: HomeFeedSettingsStartDestination.ROOT

        setContent {
            BaseTheme {
                val navController = rememberNavController()
                HomeFeedSettingsNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    onExit = {
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_START_DESTINATION = "start_destination"
        fun newIntent(context: Context, startDestination: HomeFeedSettingsStartDestination = HomeFeedSettingsStartDestination.ROOT) =
            Intent(context, HomeFeedSettingsActivity::class.java)
                .putExtra(EXTRA_START_DESTINATION, startDestination.name)
    }
}

enum class HomeFeedSettingsStartDestination { ROOT, COMMUNITY_MODULES, FOR_YOU_MODULES, DEFAULT_FEED_VIEW }
