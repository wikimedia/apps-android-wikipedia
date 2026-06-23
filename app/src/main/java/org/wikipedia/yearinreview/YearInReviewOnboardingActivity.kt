package org.wikipedia.yearinreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.YearInReviewEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.components.WikipediaAlertDialog
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.mwapi.MwNotLoggedInException
import org.wikipedia.login.LoginActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.UiState
import kotlin.getValue

class YearInReviewOnboardingActivity : BaseActivity() {
    private val viewModel: YearInReviewOnboardingViewModel by viewModels()

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            Prefs.yearInReviewModelData = emptyMap()
            proceed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        YearInReviewEvent.submit(action = "impression", slide = "explore_prompt")
        Prefs.yearInReviewVisited = true

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collectLatest {
                    if (it is UiState.Error && it.error is MwNotLoggedInException) {
                        AccountUtil.bailWithLogout()
                    }
                }
            }
        }

        setContent {
            BaseTheme {
                val uiState = viewModel.uiState.collectAsState().value

                var showLoginDialog by remember { mutableStateOf(false) }
                if (showLoginDialog) {
                    WikipediaAlertDialog(
                        title = stringResource(R.string.year_in_review_login_dialog_title),
                        message = stringResource(R.string.year_in_review_login_dialog_body),
                        confirmButtonText = stringResource(R.string.year_in_review_login_dialog_positive),
                        dismissButtonText = stringResource(R.string.year_in_review_login_dialog_negative),
                        onDismissRequest = {
                            showLoginDialog = false
                        },
                        onConfirmButtonClick = {
                            YearInReviewEvent.submit(action = "login_click", slide = "explore_prompt")
                            loginLauncher.launch(LoginActivity.newIntent(this, LoginActivity.SOURCE_YEAR_IN_REVIEW))
                        },
                        onDismissButtonClick = {
                            YearInReviewEvent.submit(action = "continue_click", slide = "explore_prompt")
                            proceed()
                        }
                    )
                }

                YearInReviewOnboardingScreen(
                    uiState = uiState,
                    onBackButtonClick = {
                        YearInReviewEvent.submit(action = "close_click", slide = "explore_prompt")
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onGetStartedClick = {
                        if (!AccountUtil.isLoggedIn) {
                            showLoginDialog = true
                        } else {
                            proceed()
                        }
                    }
                )
            }
        }
    }

    private fun proceed() {
        setResult(RESULT_OK)
        startActivity(YearInReviewActivity.newIntent(this))
        finish()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, YearInReviewOnboardingActivity::class.java)
        }
    }
}
