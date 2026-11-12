package org.wikipedia.createaccount

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.extensions.instrument
import org.wikipedia.login.LoginActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil

class CreateAccountEncourageActivity : BaseActivity() {
    private val viewModel: CreateAccountEncourageViewModel by viewModels()

    private val requestLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)

        _instrument = TestKitchenAdapter.client.getInstrument("apps-authentication")
            .startFunnel("create_account_encourage")

        instrument?.submitInteraction("impression")

        setContent {
            BaseTheme {
                val uiState by viewModel.uiState.collectAsState()

                CreateAccountEncourageScreen(
                    uiState = uiState,
                    onCloseClick = {
                        // Explicitly clobber impression count, so that we don't show it again.
                        Prefs.createAccountEncourageImpressions = 100
                        instrument?.submitInteraction("click", elementId = "close")
                        finish()
                    },
                    onCreateAccountClick = {
                        instrument?.submitInteraction("click", elementId = "create_account")
                        requestLogin.launch(LoginActivity.newIntent(this, LoginActivity.SOURCE_ENCOURAGE, createAccountFirst = true))
                    },
                    onMaybeLaterClick = {
                        instrument?.submitInteraction("click", elementId = "maybe_later")
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
