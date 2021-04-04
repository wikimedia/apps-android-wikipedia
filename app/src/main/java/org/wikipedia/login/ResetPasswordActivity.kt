package org.wikipedia.login

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputLayout
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.auth.AccountUtil.updateAccount
import org.wikipedia.createaccount.CreateAccountActivity.Companion.validateInput
import org.wikipedia.createaccount.CreateAccountActivity.ValidateResult
import org.wikipedia.databinding.ActivityResetPasswordBinding
import org.wikipedia.login.LoginClient.LoginFailedException
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.NonEmptyValidator

class ResetPasswordActivity : BaseActivity() {
    private lateinit var binding: ActivityResetPasswordBinding
    private lateinit var firstStepToken: String
    private lateinit var userName: String
    private var loginClient: LoginClient? = null
    private val loginCallback = LoginCallback()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewLoginError.backClickListener = View.OnClickListener { onBackPressed() }
        binding.viewLoginError.retryClickListener = View.OnClickListener { binding.viewLoginError.visibility = View.GONE }
        NonEmptyValidator(binding.loginButton, binding.resetPasswordInput, binding.resetPasswordRepeat)
        binding.resetPasswordInput.editText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateThenLogin()
                return@setOnEditorActionListener true
            }
            false
        }
        binding.loginButton.setOnClickListener { validateThenLogin() }
        userName = intent.getStringExtra(LOGIN_USER_NAME).orEmpty()
        firstStepToken = intent.getStringExtra(LOGIN_TOKEN).orEmpty()
    }

    override fun onBackPressed() {
        DeviceUtil.hideSoftKeyboard(this)
        super.onBackPressed()
    }

    override fun onStop() {
        showProgressBar(false)
        super.onStop()
    }

    private fun clearErrors() {
        binding.resetPasswordInput.isErrorEnabled = false
        binding.resetPasswordRepeat.isErrorEnabled = false
    }

    private fun validateThenLogin() {
        clearErrors()
        when (validateInput(userName, getText(binding.resetPasswordInput), getText(binding.resetPasswordRepeat), "")) {
            ValidateResult.INVALID_PASSWORD -> {
                binding.resetPasswordInput.requestFocus()
                binding.resetPasswordInput.error = getString(R.string.create_account_password_error)
                return
            }
            ValidateResult.PASSWORD_MISMATCH -> {
                binding.resetPasswordRepeat.requestFocus()
                binding.resetPasswordRepeat.error = getString(R.string.create_account_passwords_mismatch_error)
                return
            }
            else -> { }
        }
        doLogin()
    }

    private fun getText(input: TextInputLayout): String {
        return input.editText?.text?.toString().orEmpty()
    }

    private fun doLogin() {
        val password = getText(binding.resetPasswordInput)
        val retypedPassword = getText(binding.resetPasswordRepeat)
        val twoFactorCode = binding.login2faText.text.toString()
        showProgressBar(true)
        if (loginClient == null) {
            loginClient = LoginClient()
        }
        loginClient?.login(WikipediaApp.getInstance().wikiSite, userName, password,
                retypedPassword, twoFactorCode, firstStepToken, loginCallback)
    }

    private inner class LoginCallback : LoginClient.LoginCallback {
        override fun success(result: LoginResult) {
            showProgressBar(false)
            if (result.pass()) {
                val extras = intent.extras
                val response = extras?.getParcelable<AccountAuthenticatorResponse>(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
                updateAccount(response, result)
                DeviceUtil.hideSoftKeyboard(this@ResetPasswordActivity)
                setResult(RESULT_OK)
                finish()
            } else if (result.fail()) {
                val message = result.message
                FeedbackUtil.showMessage(this@ResetPasswordActivity, message!!)
                L.w("Login failed with result $message")
            }
        }

        override fun twoFactorPrompt(caught: Throwable, token: String?) {
            showProgressBar(false)
            firstStepToken = token.orEmpty()
            binding.login2faText.visibility = View.VISIBLE
            binding.login2faText.requestFocus()
            FeedbackUtil.showError(this@ResetPasswordActivity, caught)
        }

        override fun passwordResetPrompt(token: String?) {
            // This case should not happen here, and we wouldn't have much to do anyway.
        }

        override fun error(caught: Throwable) {
            showProgressBar(false)
            if (caught is LoginFailedException) {
                FeedbackUtil.showError(this@ResetPasswordActivity, caught)
            } else {
                showError(caught)
            }
        }
    }

    private fun showProgressBar(enable: Boolean) {
        binding.viewProgressBar.isVisible = enable
        binding.loginButton.isEnabled = !enable
        binding.loginButton.setText(if (enable) R.string.login_in_progress_dialog_message else R.string.menu_login)
    }

    private fun showError(caught: Throwable) {
        binding.viewLoginError.setError(caught)
        binding.viewLoginError.visibility = View.VISIBLE
    }

    companion object {
        const val LOGIN_USER_NAME = "userName"
        const val LOGIN_TOKEN = "token"

        fun newIntent(context: Context, userName: String, token: String?): Intent {
            return Intent(context, ResetPasswordActivity::class.java)
                    .putExtra(LOGIN_USER_NAME, userName)
                    .putExtra(LOGIN_TOKEN, token)
        }
    }
}
