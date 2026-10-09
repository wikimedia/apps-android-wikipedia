package org.wikipedia.yearinreview.presentation

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.YearInReviewEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.components.WikipediaAlertDialog
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.mwapi.MwNotLoggedInException
import org.wikipedia.login.LoginActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UiState
import org.wikipedia.util.UriUtil
import org.wikipedia.yearinreview.data.PrefsYearInReviewStore
import kotlin.getValue

class YearInReviewOnboardingActivity : BaseActivity() {
    private val viewModel: YearInReviewOnboardingViewModel by viewModels()

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            PrefsYearInReviewStore.clearAll()
            proceed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        YearInReviewEvent.submit(action = "impression", slide = "explore_prompt")
        Prefs.yearInReviewVisited = true

        initializeYearInReviewRive(this)
        // The cover's artwork runs under the status bar, and the paper-colored Explore area sits above the navigation bar
        val navigationBarStyle = if (WikipediaApp.instance.currentTheme.isDark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = navigationBarStyle
        )

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
                // The error is shown on the paper color, where the cover's light status bar icons would disappear
                val isError = uiState is UiState.Error
                LaunchedEffect(isError) {
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
                        isError && !WikipediaApp.instance.currentTheme.isDark
                }

                var showLoginDialog by rememberSaveable { mutableStateOf(false) }
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
                            // Closed so that cancelling the login returns to the cover, where Explore asks again
                            showLoginDialog = false
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
                    onCloseClick = {
                        YearInReviewEvent.submit(action = "close_click", slide = "explore_prompt")
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onLearnMoreClick = {
                        YearInReviewEvent.submit(action = "learn_click", slide = "explore_prompt")
                        UriUtil.handleExternalLink(this, getString(R.string.year_in_review_media_wiki_url).toUri())
                    },
                    onAboutInsightsClick = {
                        YearInReviewEvent.submit(action = "about_insights_click", slide = "explore_prompt")
                        UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_media_wiki_faq_url).toUri())
                    },
                    onShareFeedbackClick = {
                        FeedbackUtil.composeEmail(this, subject = getString(R.string.year_in_review_feedback_email_subject))
                    },
                    onExploreClick = {
                        YearInReviewEvent.submit(action = "start_click", slide = "explore_prompt")
                        if (!AccountUtil.isLoggedIn) {
                            showLoginDialog = true
                        } else {
                            proceed()
                        }
                    },
                    onRetryClick = {
                        viewModel.load()
                    }
                ) { snapshot ->
                    YearInReviewOnboardingCover(snapshot)
                }
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
