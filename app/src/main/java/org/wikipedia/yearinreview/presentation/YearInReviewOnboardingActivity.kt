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
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.components.WikipediaAlertDialog
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.mwapi.MwNotLoggedInException
import org.wikipedia.extensions.instrument
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
        Prefs.yearInReviewVisited = true

        _instrument = TestKitchenAdapter.client.getInstrument("apps-year-in-review")
            .setDefaultActionSource("announcement")
            .startFunnel("year-in-review")

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
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collectLatest {
                    if (it is UiState.Success && it.data.first) {
                        submitInteraction(it.data.second.isDataRich, action = "impression")
                    }
                    if (it is UiState.Error && it.error is MwNotLoggedInException) {
                        AccountUtil.bailWithLogout()
                    }
                }
            }
        }

        setContent {
            BaseTheme {
                val uiState = viewModel.uiState.collectAsState().value
                val isDataRich = uiState is UiState.Success && uiState.data.second.isDataRich
                // The error is shown on the paper color, where the cover's light status bar icons would disappear
                val isError = uiState is UiState.Error
                LaunchedEffect(isError) {
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
                        isError && !WikipediaApp.instance.currentTheme.isDark
                }

                var showLoginDialog by rememberSaveable { mutableStateOf(false) }
                if (showLoginDialog) {
                    WikipediaAlertDialog(
                        title = stringResource(if (isDataRich) R.string.year_in_review_login_dialog_title_data else R.string.year_in_review_login_dialog_title_nodata),
                        message = stringResource(if (isDataRich) R.string.year_in_review_login_dialog_body_data else R.string.year_in_review_login_dialog_body_nodata),
                        confirmButtonText = stringResource(R.string.year_in_review_login_dialog_positive),
                        dismissButtonText = stringResource(R.string.year_in_review_login_dialog_negative),
                        onDismissRequest = {
                            showLoginDialog = false
                        },
                        onConfirmButtonClick = {
                            submitInteraction(isDataRich, action = "click", actionSource = "login_dialog", elementId = "login_button")
                            // Closed so that cancelling the login returns to the cover, where Explore asks again
                            showLoginDialog = false
                            loginLauncher.launch(LoginActivity.newIntent(this, LoginActivity.SOURCE_YEAR_IN_REVIEW))
                        },
                        onDismissButtonClick = {
                            submitInteraction(isDataRich, action = "click", actionSource = "login_dialog", elementId = "continue_button")
                            proceed()
                        }
                    )
                }

                YearInReviewOnboardingScreen(
                    uiState = uiState,
                    onCloseClick = {
                        submitInteraction(isDataRich, action = "click", elementId = "close_button")
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onLearnMoreClick = {
                        submitInteraction(isDataRich, action = "click", elementId = "learn_more_button")
                        UriUtil.handleExternalLink(this, getString(R.string.year_in_review_media_wiki_url).toUri())
                    },
                    onAboutInsightsClick = {
                        submitInteraction(isDataRich, action = "click", elementId = "about_insights_button")
                        UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_media_wiki_faq_url).toUri())
                    },
                    onShareFeedbackClick = {
                        submitInteraction(isDataRich, action = "click", elementId = "share_feedback_button")
                        FeedbackUtil.composeEmail(this, subject = getString(R.string.year_in_review_feedback_email_subject))
                    },
                    onExploreClick = {
                        submitInteraction(isDataRich, action = "click", elementId = "explore_button")
                        if (!AccountUtil.isLoggedIn) {
                            showLoginDialog = true
                        } else {
                            proceed()
                        }
                    },
                    onRetryClick = {
                        viewModel.load()
                    }
                ) { entryStats, onCoverReady ->
                    YearInReviewOnboardingCover(entryStats, onCoverReady)
                }
            }
        }
    }

    private fun submitInteraction(isDataRich: Boolean, action: String, actionSource: String? = null, elementId: String? = null, actionContext: Map<String, Any> = emptyMap()) {
        instrument?.submitInteraction(action = action, actionSource = actionSource, elementId = elementId, actionContext = actionContext.plus("personalized" to isDataRich))
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
