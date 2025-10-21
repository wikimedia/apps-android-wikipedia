package org.wikipedia.yearinreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.components.WikipediaAlertDialog
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.login.LoginActivity
import org.wikipedia.settings.Prefs

class YearInReviewOnboardingActivity : BaseActivity() {

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            proceed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.yearInReviewVisited = true
        setContent {
            BaseTheme {
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
                            loginLauncher.launch(LoginActivity.newIntent(this, LoginActivity.SOURCE_YEAR_IN_REVIEW))
                        },
                        onDismissButtonClick = {
                            proceed()
                        }
                    )
                }

                YearInReviewOnboardingScreen(
                    onBackButtonClick = {
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
