package org.wikipedia.settings.discover

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme

class DiscoverSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                DiscoverScreen(
                    modifier = Modifier
                        .fillMaxSize(),
                    onBackButtonClick = {
                        onBackPressed()
                    },
                )
            }
        }
    }
}
