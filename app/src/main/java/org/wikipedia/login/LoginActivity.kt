package org.wikipedia.login

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import com.google.android.material.textfield.TextInputLayout
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.LoginFunnel
import org.wikipedia.auth.AccountUtil.updateAccount
import org.wikipedia.createaccount.CreateAccountActivity
import org.wikipedia.databinding.ActivityLoginBinding
import org.wikipedia.login.LoginClient.LoginFailedException
import org.wikipedia.notifications.NotificationPollBroadcastReceiver.Companion.pollNotifications
import org.wikipedia.page.PageTitle
import org.wikipedia.push.WikipediaFirebaseMessagingService.Companion.updateSubscription
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil.visitInExternalBrowser
import org.wikipedia.util.log.L
import org.wikipedia.views.NonEmptyValidator

class LoginActivity : BaseActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var loginSource: String
    private var firstStepToken: String? = null
    private var funnel: LoginFunnel = LoginFunnel(WikipediaApp.getInstance())
    private val loginClient = LoginClient()
    private val loginCallback = LoginCallback()
    private var shouldLogLogin = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewLoginError.backClickListener = View.OnClickListener { onBackPressed() }
        binding.viewLoginError.retryClickListener = View.OnClickListener { binding.viewLoginError.visibility = View.GONE }

        // Don't allow user to attempt login until they've put in a username and password
        NonEmptyValidator(binding.loginButton, binding.loginUsernameText, binding.loginPasswordInput)
        binding.loginPasswordInput.editText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateThenLogin()
                return@setOnEditorActionListener true
            }
            false
        }

        loginSource = intent.getStringExtra(LOGIN_REQUEST_SOURCE).orEmpty()
        if (loginSource.isNotEmpty() && loginSource == LoginFunnel.SOURCE_SUGGESTED_EDITS) {
            Prefs.setSuggestedEditsHighestPriorityEnabled(true)
        }

        // always go to account creation before logging in
        if (savedInstanceState == null) {
            startCreateAccountActivity()
        }

        setAllViewsClickListener()

        // Assume no login by default
        setResult(RESULT_LOGIN_FAIL)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_REQUEST_CREATE_ACCOUNT) {
            logLoginStart()
            when (resultCode) {
                CreateAccountActivity.RESULT_ACCOUNT_CREATED -> {
                    binding.loginUsernameText.editText?.setText(data!!.getStringExtra(CreateAccountActivity.CREATE_ACCOUNT_RESULT_USERNAME))
                    binding.loginPasswordInput.editText?.setText(data?.getStringExtra(CreateAccountActivity.CREATE_ACCOUNT_RESULT_PASSWORD))
                    funnel.logCreateAccountSuccess()
                    FeedbackUtil.showMessage(this,
                            R.string.create_account_account_created_toast)
                    doLogin()
                }
                CreateAccountActivity.RESULT_ACCOUNT_NOT_CREATED -> finish()
                else -> funnel.logCreateAccountFailure()
            }
        } else if (requestCode == Constants.ACTIVITY_REQUEST_RESET_PASSWORD &&
                resultCode == ResetPasswordActivity.RESULT_PASSWORD_RESET_SUCCESS) {
            onLoginSuccess()
        }
    }

    override fun onBackPressed() {
        DeviceUtil.hideSoftKeyboard(this)
        super.onBackPressed()
    }

    override fun onStop() {
        binding.viewProgressBar.visibility = View.GONE
        loginClient.cancel()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("loginShowing", true)
    }

    private fun setAllViewsClickListener() {
        binding.loginButton.setOnClickListener { validateThenLogin() }
        binding.loginCreateAccountButton.setOnClickListener { startCreateAccountActivity() }
        binding.inflateLoginAndAccount.privacyPolicyLink.setOnClickListener { FeedbackUtil.showPrivacyPolicy(this) }
        binding.inflateLoginAndAccount.forgotPasswordLink.setOnClickListener {
            val title = PageTitle("Special:PasswordReset", WikipediaApp.getInstance().wikiSite)
            visitInExternalBrowser(this, Uri.parse(title.uri))
        }
    }

    private fun getText(input: TextInputLayout): String {
        return input.editText?.text?.toString().orEmpty()
    }

    private fun clearErrors() {
        binding.loginUsernameText.isErrorEnabled = false
        binding.loginPasswordInput.isErrorEnabled = false
    }

    private fun validateThenLogin() {
        clearErrors()
        if (!CreateAccountActivity.USERNAME_PATTERN.matcher(getText(binding.loginUsernameText)).matches()) {
            binding.loginUsernameText.requestFocus()
            binding.loginUsernameText.error = getString(R.string.create_account_username_error)
            return
        }
        doLogin()
    }

    private fun logLoginStart() {
        if (shouldLogLogin && loginSource.isNotEmpty()) {
            if (loginSource == LoginFunnel.SOURCE_EDIT) {
                funnel.logStart(
                        LoginFunnel.SOURCE_EDIT,
                        intent.getStringExtra(EDIT_SESSION_TOKEN)
                )
            } else {
                funnel.logStart(loginSource)
            }
            shouldLogLogin = false
        }
    }

    private fun startCreateAccountActivity() {
        funnel.logCreateAccountAttempt()
        val intent = Intent(this, CreateAccountActivity::class.java)
        intent.putExtra(CreateAccountActivity.LOGIN_SESSION_TOKEN, funnel.sessionToken)
        intent.putExtra(CreateAccountActivity.LOGIN_REQUEST_SOURCE, loginSource)
        startActivityForResult(intent, Constants.ACTIVITY_REQUEST_CREATE_ACCOUNT)
    }

    private fun onLoginSuccess() {
        funnel.logSuccess()
        DeviceUtil.hideSoftKeyboard(this@LoginActivity)
        setResult(RESULT_LOGIN_SUCCESS)

        // Set reading list syncing to enabled (without the explicit setup instruction),
        // so that the sync adapter can run at least once and check whether syncing is enabled
        // on the server side.
        Prefs.setReadingListSyncEnabled(true)
        Prefs.setReadingListPagesDeletedIds(emptySet())
        Prefs.setReadingListsDeletedIds(emptySet())
        ReadingListSyncAdapter.manualSyncWithForce()
        pollNotifications(WikipediaApp.getInstance())
        updateSubscription()
        finish()
    }

    private fun doLogin() {
        val username = getText(binding.loginUsernameText)
        val password = getText(binding.loginPasswordInput)
        val twoFactorCode = getText(binding.login2faText)
        showProgressBar(true)
        if (twoFactorCode.isNotEmpty() && !firstStepToken.isNullOrEmpty()) {
            loginClient.login(WikipediaApp.getInstance().wikiSite, username, password,
                    null, twoFactorCode, firstStepToken!!, loginCallback)
        } else {
            loginClient.request(WikipediaApp.getInstance().wikiSite, username, password, loginCallback)
        }
    }

    private inner class LoginCallback : LoginClient.LoginCallback {
        override fun success(result: LoginResult) {
            showProgressBar(false)
            if (result.pass()) {
                val response = intent.extras?.getParcelable<AccountAuthenticatorResponse>(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
                updateAccount(response, result)
                onLoginSuccess()
            } else if (result.fail()) {
                val message = result.message.orEmpty()
                FeedbackUtil.showMessage(this@LoginActivity, message)
                funnel.logError(message)
                L.w("Login failed with result $message")
            }
        }

        override fun twoFactorPrompt(caught: Throwable, token: String) {
            showProgressBar(false)
            firstStepToken = token
            binding.login2faText.visibility = View.VISIBLE
            binding.login2faText.requestFocus()
            FeedbackUtil.showError(this@LoginActivity, caught)
        }

        override fun passwordResetPrompt(token: String?) {
            startActivityForResult(ResetPasswordActivity.newIntent(this@LoginActivity,
                    getText(binding.loginUsernameText), token), Constants.ACTIVITY_REQUEST_RESET_PASSWORD)
        }

        override fun error(caught: Throwable) {
            showProgressBar(false)
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
        binding.viewLoginError.setError(caught)
        binding.viewLoginError.visibility = View.VISIBLE
    }

    companion object {
        const val RESULT_LOGIN_SUCCESS = 1
        const val RESULT_LOGIN_FAIL = 2
        const val LOGIN_REQUEST_SOURCE = "login_request_source"
        const val EDIT_SESSION_TOKEN = "edit_session_token"

        @JvmStatic
        @JvmOverloads
        fun newIntent(context: Context,
                      source: String,
                      token: String? = null): Intent {
            return Intent(context, LoginActivity::class.java)
                    .putExtra(LOGIN_REQUEST_SOURCE, source)
                    .putExtra(EDIT_SESSION_TOKEN, token)
        }
    }
}
