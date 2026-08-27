package org.wikipedia.createaccount

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil

class CreateAccountEncourageActivity : BaseActivity() {

    private val viewModel: CreateAccountEncourageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)

        setContent {
            BaseTheme {
                val uiState by viewModel.uiState.collectAsState()

                CreateAccountEncourageScreen(
                    uiState = uiState,
                    onCloseClick = {
                        // Explicitly clobber impression count, so that we don't show it again.
                        Prefs.createAccountEncourageImpressions = 100
                        finish()
                    },
                    onCreateAccountClick = {

                    },
                    onMaybeLaterClick = {
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, CreateAccountEncourageActivity::class.java)
        }
    }
}
