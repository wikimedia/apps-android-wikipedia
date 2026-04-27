package org.wikipedia.login

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.auth.AccountUtil
import org.wikipedia.auth.AccountUtil.updateAccount
import org.wikipedia.captcha.CaptchaHandler
import org.wikipedia.captcha.CaptchaResult
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.createaccount.CreateAccountActivity
import org.wikipedia.databinding.ActivityLoginBinding
import org.wikipedia.events.LoggedInEvent
import org.wikipedia.extensions.getInstrumentActionContext
import org.wikipedia.extensions.instrument
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.notifications.PollNotificationWorker
import org.wikipedia.push.WikipediaFirebaseMessagingService.Companion.updateSubscription
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil.visitInExternalBrowser
import org.wikipedia.util.log.L
import org.wikipedia.views.NonEmptyValidator
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository
import java.time.LocalDate

class LoginActivity : BaseActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var captchaHandler: CaptchaHandler
    private lateinit var loginSource: String

    private var wiki = WikipediaApp.instance.wikiSite
    private var uiPromptResult: LoginResult? = null
    private var captchaResult: CaptchaResult? = null
    private var firstStepToken: String? = null

    private val loginClient = LoginClient()
    private val loginCallback = LoginCallback()
    private val textEnteredEventSent = mutableMapOf<View, Boolean>()

    private val createAccountLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        when (it.resultCode) {
            CreateAccountActivity.RESULT_ACCOUNT_CREATED -> {
                binding.loginUsernameText.editText?.setText(it.data!!.getStringExtra(CreateAccountActivity.CREATE_ACCOUNT_RESULT_USERNAME))
                binding.loginPasswordInput.editText?.setText(it.data?.getStringExtra(CreateAccountActivity.CREATE_ACCOUNT_RESULT_PASSWORD))
                FeedbackUtil.showMessage(this, R.string.create_account_account_created_toast)
                doLogin()
            }
            CreateAccountActivity.RESULT_ACCOUNT_NOT_CREATED -> finish()
        }
    }

    private val resetPasswordLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            onLoginSuccess()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        _instrument = TestKitchenAdapter.client.getInstrument("apps-authentication")
            .setDefaultActionSource("login_form")
            .startFunnel("login_account")

        onBackPressedDispatcher.addCallback(this) {
            instrument?.submitInteraction("click", elementId = "back")
            finish()
        }

        captchaHandler = CaptchaHandler(this, wiki, binding.captchaContainer.root,
            binding.loginPrimaryContainer, getString(R.string.login_activity_title),
            submitButtonText = null, isModal = false, instrument = instrument)

        binding.viewLoginError.backClickListener = View.OnClickListener {
            instrument?.submitInteraction("click", elementId = "error_back_button")
            onBackPressedDispatcher.onBackPressed()
        }
        binding.viewLoginError.retryClickListener = View.OnClickListener {
            instrument?.submitInteraction("click", elementId = "error_retry_button")
            binding.viewLoginError.visibility = View.GONE
        }

        // Don't allow user to attempt login until they've put in a username and password
        NonEmptyValidator(binding.loginButton, binding.loginUsernameText, binding.loginPasswordInput, binding.login2faText)
        binding.loginPasswordInput.editText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateThenLogin()
                return@setOnEditorActionListener true
            }
            false
        }

        addFirstKeystrokeInstrumentation(binding.loginUsernameText.editText, "username")
        addFirstKeystrokeInstrumentation(binding.loginPasswordInput.editText, "password")
        addFirstKeystrokeInstrumentation(binding.login2faText.editText, "2fa")

        loginSource = intent.getStringExtra(LOGIN_REQUEST_SOURCE).orEmpty()
        if (loginSource.isNotEmpty() && loginSource == SOURCE_SUGGESTED_EDITS) {
            Prefs.isSuggestedEditsHighestPriorityEnabled = true
        }

        if (AccountUtil.isTemporaryAccount) {
            binding.footerContainer.tempAccountInfoContainer.isVisible = true
            binding.footerContainer.tempAccountInfoText.text = StringUtil.fromHtml(getString(R.string.temp_account_login_status, AccountUtil.userName))
        } else {
            binding.footerContainer.tempAccountInfoContainer.isVisible = false
        }

        // always go to account creation before logging in, unless we arrived here through the
        // system account creation workflow
        if (savedInstanceState == null && !intent.hasExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE) &&
                intent.getBooleanExtra(CREATE_ACCOUNT_FIRST, true)) {
            startCreateAccountActivity()
        }

        setAllViewsClickListener()
        resetAuthState()

        // Assume no login by default
        setResult(RESULT_LOGIN_FAIL)

        instrument?.submitInteraction("impression", actionContext = mapOf("invoke_source" to loginSource))
    }

    override fun onStop() {
        binding.viewProgressBar.visibility = View.GONE
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("loginShowing", true)
    }

    private fun setAllViewsClickListener() {
        binding.loginButton.setOnClickListener {
            instrument?.submitInteraction("click", elementId = "login_button")
            validateThenLogin()
        }
        binding.loginCreateAccountButton.setOnClickListener {
            instrument?.submitInteraction("click", elementId = "create_account_button")
            startCreateAccountActivity()
        }
        binding.footerContainer.privacyPolicyLink.setOnClickListener {
            instrument?.submitInteraction("click", elementId = "privacy_policy_link")
            FeedbackUtil.showPrivacyPolicy(this)
        }
        binding.footerContainer.forgotPasswordLink.setOnClickListener {
            instrument?.submitInteraction("click", elementId = "forgot_password_link")
            val forgotPasswordUrl = WikipediaApp.instance.getString(R.string.forget_password_link, wiki.languageCode)
            visitInExternalBrowser(this, forgotPasswordUrl.toUri())
        }
    }

    private fun addFirstKeystrokeInstrumentation(view: EditText?, elementId: String) {
        view?.addTextChangedListener {
            if (!it.isNullOrEmpty() && !(textEnteredEventSent[view] ?: false)) {
                instrument?.submitInteraction("type", elementId = elementId)
                textEnteredEventSent[view] = true
            }
        }
    }

    private fun getText(input: TextInputLayout): String {
        return input.editText?.text?.toString().orEmpty()
    }

    private fun resetAuthState() {
        binding.login2faText.isVisible = false
        binding.login2faText.editText?.setText("")
        firstStepToken = null
        uiPromptResult = null
        captchaResult = null
        captchaHandler.hideCaptcha()
    }

    private fun clearErrors() {
        binding.loginUsernameText.isErrorEnabled = false
        binding.loginPasswordInput.isErrorEnabled = false
        captchaHandler.setErrorText()
    }

    private fun validateThenLogin() {
        clearErrors()
        if (!CreateAccountActivity.USERNAME_PATTERN.matcher(getText(binding.loginUsernameText)).matches()) {
            instrument?.submitInteraction("error", actionContext = mapOf("validation_error" to "username_invalid"))
            binding.loginUsernameText.requestFocus()
            binding.loginUsernameText.error = getString(R.string.create_account_username_error)
            return
        }
        if (captchaHandler.isActive && captchaHandler.captchaWord().isNullOrEmpty()) {
            instrument?.submitInteraction("error", actionContext = mapOf("validation_error" to "fancy_captcha_empty"))
            captchaHandler.setErrorText(getString(R.string.edit_section_captcha_hint))
            captchaHandler.setFocus()
            return
        }
        doLogin()
    }

    private fun startCreateAccountActivity() {
        createAccountLauncher.launch(CreateAccountActivity.newIntent(this, loginSource))
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
            intent.removeExtra(LOGIN_REQUEST_SOURCE)
        }
        DeviceUtil.hideSoftKeyboard(this@LoginActivity)
        setResult(RESULT_LOGIN_SUCCESS)

        // Set reading list syncing to enabled (without the explicit setup instruction),
        // so that the sync adapter can run at least once and check whether syncing is enabled
        // on the server side.
        Prefs.isReadingListSyncEnabled = true
        Prefs.readingListPagesDeletedIds = emptySet()
        Prefs.readingListsDeletedIds = emptySet()
        Prefs.tempAccountWelcomeShown = false
        Prefs.tempAccountCreateDay = 0L
        Prefs.lastBackgroundLoginDateTime = ""
        ReadingListSyncAdapter.manualSyncWithForce()
        PollNotificationWorker.schedulePollNotificationJob(this)
        updateSubscription()
        FlowEventBus.post(LoggedInEvent())
        finish()
    }

    private fun doLogin() {
        val username = getText(binding.loginUsernameText)
        val password = getText(binding.loginPasswordInput)
        val twoFactorCode = getText(binding.login2faText)
        showProgressBar(true)

        if (uiPromptResult == null && captchaResult == null) {
            loginClient.login(lifecycleScope, WikipediaApp.instance.wikiSite, username, password, cb = loginCallback)
        } else {
            loginClient.login(lifecycleScope, WikipediaApp.instance.wikiSite, username, password, token = firstStepToken,
                captchaId = if (captchaResult != null) captchaHandler.captchaId() else null,
                captchaWord = if (captchaResult != null) captchaHandler.captchaWord() else null,
                twoFactorCode = if (uiPromptResult is LoginOATHResult) twoFactorCode else null,
                emailAuthCode = if (uiPromptResult is LoginEmailAuthResult) twoFactorCode else null,
                isContinuation = uiPromptResult != null,
                cb = loginCallback)
        }
    }

    private inner class LoginCallback : LoginClient.LoginCallback {
        override fun success(result: LoginResult) {
            showProgressBar(false)
            if (result.pass()) {
                val response = intent.parcelableExtra<AccountAuthenticatorResponse>(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
                updateAccount(response, result)
                onLoginSuccess()
            } else if (result.fail()) {
                instrument?.submitInteraction("error", actionContext = mapOf("code" to result.messageCode.orEmpty()))
                val message = result.message.orEmpty()
                FeedbackUtil.showMessage(this@LoginActivity, message)
                L.w("Login failed with result $message")
            }
        }

        override fun uiPrompt(result: LoginResult, caught: Throwable, captchaId: String?, token: String?) {
            showProgressBar(false)
            firstStepToken = token
            if (captchaId != null) {
                if (captchaResult != null) {
                    FeedbackUtil.showError(this@LoginActivity, caught)
                }
                captchaResult = CaptchaResult(captchaId)
                captchaHandler.handleCaptcha(token, captchaResult!!)
            }
            if (result is LoginEmailAuthResult || result is LoginOATHResult) {
                instrument?.submitInteraction("impression", elementId = if (result is LoginOATHResult) "2fa_oath" else "2fa_email")
                uiPromptResult = result
                binding.login2faText.hint =
                    getString(if (result is LoginEmailAuthResult) R.string.login_email_auth_hint else R.string.login_2fa_hint)
                binding.login2faText.visibility = View.VISIBLE
                binding.login2faText.editText?.setText("")
                binding.login2faText.requestFocus()
                FeedbackUtil.showError(this@LoginActivity, caught)
            }
            DeviceUtil.hideSoftKeyboard(this@LoginActivity)
        }

        override fun passwordResetPrompt(token: String?) {
            resetPasswordLauncher.launch(ResetPasswordActivity.newIntent(this@LoginActivity, getText(binding.loginUsernameText), token))
        }

        override fun error(caught: Throwable) {
            showProgressBar(false)
            resetAuthState()
            DeviceUtil.hideSoftKeyboard(this@LoginActivity)
            if (caught is LoginFailedException) {
                FeedbackUtil.showError(this@LoginActivity, caught)
            } else {
                showError(caught)
            }
        }
    }

    private fun showProgressBar(enable: Boolean) {
        binding.viewProgressBar.visibility = if (enable) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !enable
        binding.loginButton.setText(if (enable) R.string.login_in_progress_dialog_message else R.string.menu_login)
    }

    private fun showError(caught: Throwable) {
        instrument?.submitInteraction("error", actionContext = caught.getInstrumentActionContext())
        binding.viewLoginError.setError(caught)
        binding.viewLoginError.visibility = View.VISIBLE
    }

    companion object {
        const val RESULT_LOGIN_SUCCESS = 1
        const val RESULT_LOGIN_FAIL = 2
        const val LOGIN_REQUEST_SOURCE = "login_request_source"
        const val CREATE_ACCOUNT_FIRST = "create_account_first"
        const val SOURCE_NAV = "navigation"
        const val SOURCE_EDIT = "edit"
        const val SOURCE_SYSTEM = "system"
        const val SOURCE_ONBOARDING = "onboarding"
        const val SOURCE_SETTINGS = "settings"
        const val SOURCE_SUBSCRIBE = "subscribe"
        const val SOURCE_READING_MANUAL_SYNC = "reading_lists_manual_sync"
        const val SOURCE_LOGOUT_BACKGROUND = "logout_background"
        const val SOURCE_SUGGESTED_EDITS = "suggestededits"
        const val SOURCE_TALK = "talk"
        const val SOURCE_ACTIVITY_TAB = "activity_tab"
        const val SOURCE_YEAR_IN_REVIEW = "yir"
        const val SOURCE_ON_THIS_DAY_GAME_RESULT = "on_this_day_game_result"
        const val SOURCE_READING_CHALLENGE = "widget_challenge"

        fun newIntent(context: Context, source: String, createAccountFirst: Boolean = true): Intent {
            return Intent(context, LoginActivity::class.java)
                    .putExtra(LOGIN_REQUEST_SOURCE, source)
                    .putExtra(CREATE_ACCOUNT_FIRST, createAccountFirst)
        }
    }
}
