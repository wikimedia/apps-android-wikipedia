package org.wikipedia.createaccount

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.CreateAccountEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.captcha.CaptchaHandler
import org.wikipedia.captcha.CaptchaResult
import org.wikipedia.databinding.ActivityCreateAccountBinding
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil.visitInExternalBrowser
import org.wikipedia.util.log.L
import org.wikipedia.views.NonEmptyValidator
import java.util.regex.Pattern

class CreateAccountActivity : BaseActivity() {
    enum class ValidateResult {
        SUCCESS, INVALID_USERNAME, PASSWORD_TOO_SHORT, PASSWORD_IS_USERNAME, PASSWORD_MISMATCH, NO_EMAIL, INVALID_EMAIL
    }

    private lateinit var binding: ActivityCreateAccountBinding
    private lateinit var captchaHandler: CaptchaHandler
    private lateinit var createAccountEvent: CreateAccountEvent
    private var wiki = WikipediaApp.instance.wikiSite
    private var userNameTextWatcher: TextWatcher? = null
    private val viewModel: CreateAccountActivityViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        captchaHandler = CaptchaHandler(this, wiki, binding.captchaContainer.root, binding.createAccountPrimaryContainer, getString(R.string.create_account_activity_title), getString(R.string.create_account_button))
        // Don't allow user to submit registration unless they've put in a username and password
        NonEmptyValidator(binding.createAccountSubmitButton, binding.createAccountUsername, binding.createAccountPasswordInput)
        // Don't allow user to continue when they're shown a captcha until they fill it in
        NonEmptyValidator(binding.captchaContainer.captchaSubmitButton, binding.captchaContainer.captchaText)
        setClickListeners()
        createAccountEvent = CreateAccountEvent(intent.getStringExtra(LOGIN_REQUEST_SOURCE).orEmpty())
        // Only send the editing start log event if the activity is created for the first time
        if (savedInstanceState == null) {
            createAccountEvent.logStart()
        }

        if (AccountUtil.isTemporaryAccount) {
            binding.footerContainer.tempAccountInfoContainer.isVisible = true
            binding.footerContainer.tempAccountInfoText.text = StringUtil.fromHtml(getString(R.string.temp_account_login_status, AccountUtil.userName))
        } else {
            binding.footerContainer.tempAccountInfoContainer.isVisible = false
        }

        // Set default result to failed, so we can override if it did not
        setResult(RESULT_ACCOUNT_NOT_CREATED)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.createAccountInfoState.collect {
                        when (it) {
                            is CreateAccountActivityViewModel.AccountInfoState.DoCreateAccount -> {
                                doCreateAccount(it.token)
                            }
                            is CreateAccountActivityViewModel.AccountInfoState.HandleCaptcha -> {
                                captchaHandler.handleCaptcha(it.token, CaptchaResult(it.captchaId))
                            }
                            is CreateAccountActivityViewModel.AccountInfoState.InvalidToken -> {
                                handleAccountCreationError(getString(R.string.create_account_generic_error))
                            }
                            is CreateAccountActivityViewModel.AccountInfoState.Error -> {
                                showError(it.throwable)
                                L.e(it.throwable)
                            }
                        }
                    }
                }
                launch {
                    viewModel.doCreateAccountState.collect {
                        when (it) {
                            is CreateAccountActivityViewModel.CreateAccountState.Pass -> {
                                finishWithUserResult(it.userName)
                            }
                            is CreateAccountActivityViewModel.CreateAccountState.Error -> {
                                if (it.throwable is CreateAccountException) {
                                    createAccountEvent.logError(it.throwable.message)
                                }
                                L.e(it.throwable.toString())
                                createAccountEvent.logError(it.throwable.toString())
                                showProgressBar(false)
                                showError(it.throwable)
                            }
                        }
                    }
                }
                launch {
                    viewModel.verifyUserNameState.collect {
                        when (it) {
                            CreateAccountActivityViewModel.UserNameState.Initial,
                            CreateAccountActivityViewModel.UserNameState.Success -> {
                                binding.createAccountUsername.isErrorEnabled = false
                            }
                            is CreateAccountActivityViewModel.UserNameState.Blocked -> {
                                handleAccountCreationError(it.error)
                            }
                            is CreateAccountActivityViewModel.UserNameState.CannotCreate -> {
                                binding.createAccountUsername.error = getString(R.string.create_account_name_unavailable, it.userName)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setClickListeners() {
        binding.viewCreateAccountError.backClickListener = View.OnClickListener {
            binding.viewCreateAccountError.visibility = View.GONE
            captchaHandler.requestNewCaptcha()
        }
        binding.viewCreateAccountError.retryClickListener = View.OnClickListener { binding.viewCreateAccountError.visibility = View.GONE }
        binding.createAccountSubmitButton.setOnClickListener {
            validateThenCreateAccount()
        }
        binding.captchaContainer.captchaSubmitButton.setOnClickListener {
            validateThenCreateAccount()
        }
        binding.createAccountLoginButton.setOnClickListener {
            // This assumes that the CreateAccount activity was launched from the Login activity
            // (since there's currently no other mechanism to invoke CreateAccountActivity),
            // so finishing this activity will implicitly go back to Login.
            setResult(RESULT_ACCOUNT_LOGIN)
            finish()
        }
        binding.footerContainer.privacyPolicyLink.setOnClickListener {
            FeedbackUtil.showPrivacyPolicy(this)
        }
        binding.footerContainer.forgotPasswordLink.setOnClickListener {
            visitInExternalBrowser(this, Uri.parse(PageTitle("Special:PasswordReset", wiki).uri))
        }
        // Add listener so that when the user taps enter, it submits the captcha
        binding.captchaContainer.captchaText.setOnKeyListener { _: View, keyCode: Int, event: KeyEvent ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                validateThenCreateAccount()
                return@setOnKeyListener true
            }
            false
        }
        userNameTextWatcher = binding.createAccountUsername.editText?.doOnTextChanged { text, _, _, _ ->
            viewModel.verifyUserName(text)
        }
    }

    private fun handleAccountCreationError(message: String) {
        if (message.contains("blocked")) {
            FeedbackUtil.makeSnackbar(this, getString(R.string.create_account_ip_block_message))
                    .setAction(R.string.create_account_ip_block_details) {
                        visitInExternalBrowser(this,
                                Uri.parse(getString(R.string.create_account_ip_block_help_url)))
                    }
                    .show()
        } else {
            FeedbackUtil.showMessage(this, StringUtil.fromHtml(message))
        }
        L.w("Account creation failed with result $message")
    }

    private fun doCreateAccount(token: String) {
        showProgressBar(true)
        val email = getText(binding.createAccountEmail).ifEmpty { null }
        val password = getText(binding.createAccountPasswordInput)
        val repeat = getText(binding.createAccountPasswordRepeat)
        val userName = getText(binding.createAccountUsername)
        viewModel.doCreateAccount(token, captchaHandler.captchaId().toString(), captchaHandler.captchaWord().toString(), userName, password, repeat, email)
    }

    override fun onBackPressed() {
        if (captchaHandler.isActive) {
            captchaHandler.cancelCaptcha()
            showProgressBar(false)
            return
        }
        DeviceUtil.hideSoftKeyboard(this)
        super.onBackPressed()
    }

    public override fun onStop() {
        showProgressBar(false)
        super.onStop()
    }

    public override fun onDestroy() {
        captchaHandler.dispose()
        userNameTextWatcher?.let { binding.createAccountUsername.editText?.removeTextChangedListener(it) }
        super.onDestroy()
    }

    private fun clearErrors() {
        binding.createAccountUsername.isErrorEnabled = false
        binding.createAccountPasswordInput.isErrorEnabled = false
        binding.createAccountPasswordRepeat.isErrorEnabled = false
        binding.createAccountEmail.isErrorEnabled = false
    }

    private fun validateThenCreateAccount() {
        clearErrors()
        val result = validateInput(getText(binding.createAccountUsername), getText(binding.createAccountPasswordInput),
                getText(binding.createAccountPasswordRepeat), getText(binding.createAccountEmail))
        when (result) {
            ValidateResult.INVALID_USERNAME -> {
                binding.createAccountUsername.requestFocus()
                binding.createAccountUsername.error = getString(R.string.create_account_username_error)
                return
            }
            ValidateResult.PASSWORD_TOO_SHORT -> {
                binding.createAccountPasswordInput.requestFocus()
                binding.createAccountPasswordInput.error = getString(R.string.create_account_password_error)
                return
            }
            ValidateResult.PASSWORD_IS_USERNAME -> {
                binding.createAccountPasswordInput.requestFocus()
                binding.createAccountPasswordInput.error = getString(R.string.create_account_password_is_username)
                return
            }
            ValidateResult.PASSWORD_MISMATCH -> {
                binding.createAccountPasswordRepeat.requestFocus()
                binding.createAccountPasswordRepeat.error = getString(R.string.create_account_passwords_mismatch_error)
                return
            }
            ValidateResult.INVALID_EMAIL -> {
                binding.createAccountEmail.requestFocus()
                binding.createAccountEmail.error = getString(R.string.create_account_email_error)
                return
            }
            ValidateResult.NO_EMAIL -> MaterialAlertDialogBuilder(this)
                    .setCancelable(false)
                    .setTitle(R.string.email_recommendation_dialog_title)
                    .setMessage(StringUtil.fromHtml(resources.getString(R.string.email_recommendation_dialog_message)))
                    .setPositiveButton(R.string.email_recommendation_dialog_create_without_email_action
                    ) { _: DialogInterface, _: Int -> createAccount() }
                    .setNegativeButton(R.string.email_recommendation_dialog_create_with_email_action
                    ) { _: DialogInterface, _: Int -> binding.createAccountEmail.requestFocus() }
                    .show()
            ValidateResult.SUCCESS -> createAccount()
        }
    }

    private fun createAccount() {
        if (captchaHandler.isActive && captchaHandler.token != null) {
            doCreateAccount(captchaHandler.token!!)
        } else {
            viewModel.createAccountInfo()
        }
    }

    private fun getText(input: TextInputLayout): String {
        input.editText?.let {
            return it.text.toString()
        }
        return ""
    }

    private fun finishWithUserResult(userName: String) {
        val resultIntent = Intent()
        resultIntent.putExtra(CREATE_ACCOUNT_RESULT_USERNAME, userName)
        resultIntent.putExtra(CREATE_ACCOUNT_RESULT_PASSWORD, getText(binding.createAccountPasswordInput))
        setResult(RESULT_ACCOUNT_CREATED, resultIntent)
        showProgressBar(false)
        captchaHandler.cancelCaptcha()
        createAccountEvent.logSuccess()
        DeviceUtil.hideSoftKeyboard(this@CreateAccountActivity)
        finish()
    }

    private fun showProgressBar(enable: Boolean) {
        binding.viewProgressBar.visibility = if (enable) View.VISIBLE else View.GONE
        binding.captchaContainer.captchaSubmitButton.isEnabled = !enable
        binding.captchaContainer.captchaSubmitButton.setText(if (enable) R.string.dialog_create_account_checking_progress else R.string.create_account_button)
    }

    private fun showError(caught: Throwable) {
        binding.viewCreateAccountError.setError(caught)
        binding.viewCreateAccountError.visibility = View.VISIBLE
    }

    companion object {
        private const val PASSWORD_MIN_LENGTH = 8
        const val RESULT_ACCOUNT_CREATED = 1
        const val RESULT_ACCOUNT_NOT_CREATED = 2
        const val RESULT_ACCOUNT_LOGIN = 3
        const val LOGIN_REQUEST_SOURCE = "login_request_source"
        const val CREATE_ACCOUNT_RESULT_USERNAME = "username"
        const val CREATE_ACCOUNT_RESULT_PASSWORD = "password"

        val USERNAME_PATTERN: Pattern = Pattern.compile("[^#<>\\[\\]|{}/@]*")

        fun validateInput(username: CharSequence,
                          password: CharSequence,
                          passwordRepeat: CharSequence,
                          email: CharSequence): ValidateResult {
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                return ValidateResult.INVALID_USERNAME
            } else if (password.length < PASSWORD_MIN_LENGTH) {
                return ValidateResult.PASSWORD_TOO_SHORT
            } else if (password.toString().equals(username.toString(), true)) {
                return ValidateResult.PASSWORD_IS_USERNAME
            } else if (passwordRepeat.toString() != password.toString()) {
                return ValidateResult.PASSWORD_MISMATCH
            } else if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                return ValidateResult.INVALID_EMAIL
            } else if (email.isEmpty()) {
                return ValidateResult.NO_EMAIL
            }
            return ValidateResult.SUCCESS
        }

        fun newIntent(context: Context, source: String): Intent {
            return Intent(context, CreateAccountActivity::class.java)
                    .putExtra(LOGIN_REQUEST_SOURCE, source)
        }
    }
}
