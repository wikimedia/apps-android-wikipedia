package org.wikipedia.createaccount

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputLayout
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.CreateAccountFunnel
import org.wikipedia.captcha.CaptchaHandler
import org.wikipedia.captcha.CaptchaResult
import org.wikipedia.databinding.ActivityCreateAccountBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil.visitInExternalBrowser
import org.wikipedia.util.log.L
import org.wikipedia.views.NonEmptyValidator
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class CreateAccountActivity : BaseActivity() {
    enum class ValidateResult {
        SUCCESS, INVALID_USERNAME, INVALID_PASSWORD, PASSWORD_MISMATCH, NO_EMAIL, INVALID_EMAIL
    }

    private lateinit var binding: ActivityCreateAccountBinding
    private lateinit var captchaHandler: CaptchaHandler
    private lateinit var funnel: CreateAccountFunnel
    private val disposables = CompositeDisposable()
    private var wiki = WikipediaApp.getInstance().wikiSite
    private var userNameTextWatcher: TextWatcher? = null
    private val userNameVerifyRunnable = UserNameVerifyRunnable()

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
        funnel = CreateAccountFunnel(WikipediaApp.getInstance(), intent.getStringExtra(LOGIN_REQUEST_SOURCE)!!)
        // Only send the editing start log event if the activity is created for the first time
        if (savedInstanceState == null) {
            funnel.logStart(intent.getStringExtra(LOGIN_SESSION_TOKEN))
        }
        // Set default result to failed, so we can override if it did not
        setResult(RESULT_ACCOUNT_NOT_CREATED)
    }

    private fun setClickListeners() {
        binding.viewCreateAccountError.backClickListener = View.OnClickListener {
            binding.viewCreateAccountError.visibility = View.GONE
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
            binding.createAccountUsername.removeCallbacks(userNameVerifyRunnable)
            binding.createAccountUsername.isErrorEnabled = false
            if (text.isNullOrEmpty()) {
                return@doOnTextChanged
            }
            userNameVerifyRunnable.setUserName(text.toString())
            binding.createAccountUsername.postDelayed(userNameVerifyRunnable, TimeUnit.SECONDS.toMillis(1))
        }
    }

    fun handleAccountCreationError(message: String) {
        if (message.contains("blocked")) {
            FeedbackUtil.makeSnackbar(this, getString(R.string.create_account_ip_block_message), FeedbackUtil.LENGTH_DEFAULT)
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

    private val createAccountInfo: Unit
        get() {
            disposables.add(ServiceFactory.get(wiki).authManagerInfo
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        val token = response.query?.createAccountToken()
                        val captchaId = response.query?.captchaId()
                        if (token.isNullOrEmpty()) {
                            handleAccountCreationError(getString(R.string.create_account_generic_error))
                        } else if (!captchaId.isNullOrEmpty()) {
                            captchaHandler.handleCaptcha(token, CaptchaResult(captchaId))
                        } else {
                            doCreateAccount(token)
                        }
                    }) { caught ->
                        showError(caught)
                        L.e(caught)
                    })
        }

    private fun doCreateAccount(token: String) {
        showProgressBar(true)
        var email: String? = null
        if (getText(binding.createAccountEmail).isNotEmpty()) {
            email = getText(binding.createAccountEmail)
        }
        val password = getText(binding.createAccountPasswordInput)
        val repeat = getText(binding.createAccountPasswordRepeat)
        disposables.add(ServiceFactory.get(wiki).postCreateAccount(getText(binding.createAccountUsername), password, repeat, token, Service.WIKIPEDIA_URL,
                email,
                if (captchaHandler.isActive) captchaHandler.captchaId() else "null",
                if (captchaHandler.isActive) captchaHandler.captchaWord() else "null")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    if ("PASS" == response.status) {
                        finishWithUserResult(response.user)
                    } else {
                        throw CreateAccountException(response.message)
                    }
                }) { caught ->
                    L.e(caught.toString())
                    showProgressBar(false)
                    showError(caught)
                })
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
        disposables.clear()
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
            ValidateResult.INVALID_PASSWORD -> {
                binding.createAccountPasswordInput.requestFocus()
                binding.createAccountPasswordInput.error = getString(R.string.create_account_password_error)
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
            ValidateResult.NO_EMAIL -> AlertDialog.Builder(this)
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
            createAccountInfo
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
        funnel.logSuccess()
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

    private inner class UserNameVerifyRunnable : Runnable {
        private lateinit var userName: String

        fun setUserName(userName: String) {
            this.userName = userName
        }

        override fun run() {
            disposables.add(ServiceFactory.get(wiki).getUserList(userName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        response.query?.getUserResponse(userName)?.let {
                            binding.createAccountUsername.isErrorEnabled = false
                            if (it.isBlocked) {
                                handleAccountCreationError(it.error)
                            } else if (!it.canCreate()) {
                                binding.createAccountUsername.error = getString(R.string.create_account_name_unavailable, userName)
                            }
                        }
                    }) { obj -> L.e(obj) })
        }
    }

    companion object {
        private const val PASSWORD_MIN_LENGTH = 6
        const val RESULT_ACCOUNT_CREATED = 1
        const val RESULT_ACCOUNT_NOT_CREATED = 2
        const val RESULT_ACCOUNT_LOGIN = 3
        const val LOGIN_REQUEST_SOURCE = "login_request_source"
        const val LOGIN_SESSION_TOKEN = "login_session_token"
        const val CREATE_ACCOUNT_RESULT_USERNAME = "username"
        const val CREATE_ACCOUNT_RESULT_PASSWORD = "password"

        @JvmField
        val USERNAME_PATTERN: Pattern = Pattern.compile("[^#<>\\[\\]|{}/@]*")

        @JvmStatic
        fun validateInput(username: CharSequence,
                          password: CharSequence,
                          passwordRepeat: CharSequence,
                          email: CharSequence): ValidateResult {
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                return ValidateResult.INVALID_USERNAME
            } else if (password.length < PASSWORD_MIN_LENGTH) {
                return ValidateResult.INVALID_PASSWORD
            } else if (passwordRepeat.toString() != password.toString()) {
                return ValidateResult.PASSWORD_MISMATCH
            } else if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                return ValidateResult.INVALID_EMAIL
            } else if (email.isEmpty()) {
                return ValidateResult.NO_EMAIL
            }
            return ValidateResult.SUCCESS
        }
    }
}
