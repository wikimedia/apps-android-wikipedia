package org.wikipedia.login;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.createaccount.CreateAccountActivity;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.NonEmptyValidator;
import org.wikipedia.views.WikiErrorView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class ResetPasswordActivity extends BaseActivity {
    public static final int RESULT_PASSWORD_RESET_SUCCESS = 1;
    public static final String LOGIN_USER_NAME = "userName";
    public static final String LOGIN_TOKEN = "token";

    @BindView(R.id.reset_password_input) TextInputLayout passwordInput;
    @BindView(R.id.reset_password_repeat) TextInputLayout passwordRepeatInput;
    @BindView(R.id.login_2fa_text) EditText twoFactorText;
    @BindView(R.id.view_login_error) WikiErrorView errorView;
    @BindView(R.id.login_button) Button loginButton;
    @BindView(R.id.view_progress_bar) ProgressBar progressBar;

    @Nullable private String firstStepToken;
    private LoginClient loginClient;
    private LoginCallback loginCallback = new LoginCallback();
    private String userName;

    public static Intent newIntent(@NonNull Context context, @NonNull String userName,
                                   @Nullable String token) {
        return new Intent(context, ResetPasswordActivity.class)
                .putExtra(LOGIN_USER_NAME, userName)
                .putExtra(LOGIN_TOKEN, token);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        ButterKnife.bind(this);

        errorView.setBackClickListener((v) -> onBackPressed());
        errorView.setRetryClickListener((v) -> errorView.setVisibility(View.GONE));

        new NonEmptyValidator((isValid) -> loginButton.setEnabled(isValid), passwordInput, passwordRepeatInput);

        passwordInput.getEditText().setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateThenLogin();
                return true;
            }
            return false;
        });

        userName = getIntent().getStringExtra(LOGIN_USER_NAME);
        firstStepToken = getIntent().getStringExtra(LOGIN_TOKEN);
    }

    @OnClick(R.id.login_button) void onLoginClick() {
        validateThenLogin();
    }

    private void clearErrors() {
        passwordInput.setErrorEnabled(false);
        passwordRepeatInput.setErrorEnabled(false);
    }

    private void validateThenLogin() {
        clearErrors();
        CreateAccountActivity.ValidateResult result = CreateAccountActivity.validateInput(userName,
                getText(passwordInput), getText(passwordRepeatInput), "");

        switch (result) {
            case INVALID_PASSWORD:
                passwordInput.requestFocus();
                passwordInput.setError(getString(R.string.create_account_password_error));
                return;
            case PASSWORD_MISMATCH:
                passwordRepeatInput.requestFocus();
                passwordRepeatInput.setError(getString(R.string.create_account_passwords_mismatch_error));
                return;
            default:
                break;
        }

        doLogin();
    }

    @NonNull private CharSequence getText(@NonNull TextInputLayout input) {
        return input.getEditText() != null ? input.getEditText().getText() : "";
    }

    private void doLogin() {
        String password = getText(passwordInput).toString();
        String retypedPassword = getText(passwordRepeatInput).toString();
        String twoFactorCode = twoFactorText.getText().toString();

        showProgressBar(true);

        if (loginClient == null) {
            loginClient = new LoginClient();
        }

        loginClient.login(WikipediaApp.getInstance().getWikiSite(), userName, password,
                retypedPassword, twoFactorCode, firstStepToken, loginCallback);
    }

    private class LoginCallback implements LoginClient.LoginCallback {
        @Override
        public void success(@NonNull LoginResult result) {
            showProgressBar(false);
            if (result.pass()) {
                Bundle extras = getIntent().getExtras();
                AccountAuthenticatorResponse response = extras == null
                        ? null
                        : extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                AccountUtil.updateAccount(response, result);

                hideSoftKeyboard(ResetPasswordActivity.this);
                setResult(RESULT_PASSWORD_RESET_SUCCESS);
                finish();

            } else if (result.fail()) {
                String message = result.getMessage();
                FeedbackUtil.showMessage(ResetPasswordActivity.this, message);
                L.w("Login failed with result " + message);
            }
        }

        @Override
        public void twoFactorPrompt(@NonNull Throwable caught, @NonNull String token) {
            showProgressBar(false);
            firstStepToken = token;
            twoFactorText.setVisibility(View.VISIBLE);
            twoFactorText.requestFocus();
            FeedbackUtil.showError(ResetPasswordActivity.this, caught);
        }

        @Override public void passwordResetPrompt(@Nullable String token) {
            // This case should not happen here, and we wouldn't have much to do anyway.
        }

        @Override
        public void error(@NonNull Throwable caught) {
            showProgressBar(false);
            if (caught instanceof LoginClient.LoginFailedException) {
                FeedbackUtil.showError(ResetPasswordActivity.this, caught);
            } else {
                showError(caught);
            }
        }
    }

    private void showProgressBar(boolean enable) {
        progressBar.setVisibility(enable ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!enable);
        loginButton.setText(enable ? R.string.login_in_progress_dialog_message : R.string.menu_login);
    }

    @Override
    public void onBackPressed() {
        hideSoftKeyboard(this);
        super.onBackPressed();
    }

    @Override
    public void onStop() {
        showProgressBar(false);
        super.onStop();
    }

    private void showError(@NonNull Throwable caught) {
        errorView.setError(caught);
        errorView.setVisibility(View.VISIBLE);
    }
}
