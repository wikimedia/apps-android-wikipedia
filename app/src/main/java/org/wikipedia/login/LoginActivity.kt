package org.wikipedia.login

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.extensions.getInstrumentActionContext
import org.wikipedia.extensions.instrument
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository
import java.time.LocalDate

/**
 * Logs the user in with OAuth, in a browser where they can also create an account, reset their
 * password, etc. Also handles adding an account from the system settings, via WikimediaAuthenticator.
 */
class LoginActivity : BaseActivity() {
    private val viewModel: LoginViewModel by viewModels()
    private var loginSource = ""
    private var authenticatorResponse: AccountAuthenticatorResponse? = null
    private var authenticatorResult: Bundle? = null

    private val authorizationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onAuthorizationResult(it.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)

        _instrument = TestKitchenAdapter.client.getInstrument("apps-authentication")
            .setDefaultActionSource("login_oauth")
            .startFunnel("login_account")

        loginSource = intent.getStringExtra(LOGIN_REQUEST_SOURCE).orEmpty()
        authenticatorResponse = intent.parcelableExtra<AccountAuthenticatorResponse>(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        authenticatorResponse?.onRequestContinued()

        // Assume no login by default
        setResult(RESULT_LOGIN_FAIL)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        LoginViewModel.Event.LoggedIn -> onLoginSuccess()
                        LoginViewModel.Event.Canceled -> finish()
                        is LoginViewModel.Event.Failed -> instrument?.submitInteraction("error", actionContext = event.throwable.getInstrumentActionContext())
                    }
                }
            }
        }

        setContent {
            BaseTheme {
                val error by viewModel.error.collectAsState()
                LoginScreen(
                    error = error,
                    onRetryClick = {
                        instrument?.submitInteraction("click", elementId = "error_retry_button")
                        startAuthorization()
                    },
                    onBackClick = {
                        instrument?.submitInteraction("click", elementId = "error_back_button")
                        finish()
                    }
                )
            }
        }

        if (savedInstanceState == null) {
            if (loginSource == SOURCE_SUGGESTED_EDITS) {
                Prefs.isSuggestedEditsHighestPriorityEnabled = true
            }
            instrument?.submitInteraction("impression", actionContext = mapOf("invoke_source" to loginSource))
            startAuthorization()
        }
    }

    override fun finish() {
        // The system's account settings are waiting for us to tell them whether an account was added.
        authenticatorResponse?.let {
            authenticatorResult?.let { result -> it.onResult(result) } ?: it.onError(AccountManager.ERROR_CODE_CANCELED, "canceled")
            authenticatorResponse = null
        }
        super.finish()
    }

    private fun startAuthorization() {
        viewModel.onAuthorizationStarted()
        try {
            authorizationLauncher.launch(WikipediaApp.instance.oauthClient.getLoginIntent())
        } catch (e: ActivityNotFoundException) {
            // There's no browser to log in with.
            viewModel.onError(e)
        }
    }

    private fun onLoginSuccess() {
        val isReadingChallenge = loginSource == SOURCE_READING_CHALLENGE
        instrument?.submitInteraction(action = "success", actionContext = if (isReadingChallenge) mapOf("invoke_source" to loginSource) else null)
        if (isReadingChallenge) {
            Prefs.readingChallengeEnrolled = true
            Prefs.readingChallengeEnrollmentDate = LocalDate.now().toString()
            lifecycleScope.launch {
                if (ReadingChallengeWidgetRepository.isWidgetInstalled()) {
                    ReadingChallengeWidgetRepository(this@LoginActivity).updateWidgetsAndSendAnalytics()
                }
            }
        }
        authenticatorResult = bundleOf(AccountManager.KEY_ACCOUNT_NAME to AccountUtil.userName,
            AccountManager.KEY_ACCOUNT_TYPE to AccountUtil.accountType())
        setResult(RESULT_LOGIN_SUCCESS)
        finish()
    }

    companion object {
        const val RESULT_LOGIN_SUCCESS = 1
        const val RESULT_LOGIN_FAIL = 2
        const val LOGIN_REQUEST_SOURCE = "login_request_source"
        const val SOURCE_NAV = "navigation"
        const val SOURCE_EDIT = "edit"
        const val SOURCE_SYSTEM = "system"
        const val SOURCE_ONBOARDING = "onboarding"
        const val SOURCE_SETTINGS = "settings"
        const val SOURCE_SUBSCRIBE = "subscribe"
        const val SOURCE_LOGOUT_BACKGROUND = "logout_background"
        const val SOURCE_SUGGESTED_EDITS = "suggestededits"
        const val SOURCE_TALK = "talk"
        const val SOURCE_ACTIVITY_TAB = "activity_tab"
        const val SOURCE_YEAR_IN_REVIEW = "yir"
        const val SOURCE_ON_THIS_DAY_GAME_RESULT = "on_this_day_game_result"
        const val SOURCE_READING_CHALLENGE = "widget_challenge"
        const val SOURCE_ENCOURAGE = "encourage"

        fun newIntent(context: Context, source: String): Intent {
            return Intent(context, LoginActivity::class.java)
                    .putExtra(LOGIN_REQUEST_SOURCE, source)
        }
    }
}
