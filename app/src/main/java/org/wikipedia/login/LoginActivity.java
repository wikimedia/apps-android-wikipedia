package org.wikipedia.login;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.createaccount.CreateAccountActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.NonEmptyValidator;
import org.wikipedia.views.WikiErrorView;

import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class LoginActivity extends BaseActivity {
    public static final int RESULT_LOGIN_SUCCESS = 1;
    public static final int RESULT_LOGIN_FAIL = 2;

    public static final String LOGIN_REQUEST_SOURCE = "login_request_source";
    public static final String EDIT_SESSION_TOKEN = "edit_session_token";
    public static final String ACTION_CREATE_ACCOUNT = "action_create_account";

    @BindView(R.id.login_username_text) TextInputLayout usernameInput;
    @BindView(R.id.login_password_input) TextInputLayout passwordInput;
    @BindView(R.id.login_2fa_text) TextInputLayout twoFactorText;
    @BindView(R.id.view_login_error) WikiErrorView errorView;
    @BindView(R.id.login_button) Button loginButton;
    @BindView(R.id.view_progress_bar) ProgressBar progressBar;

    @Nullable private String firstStepToken;
    private LoginFunnel funnel;
    private String loginSource;
    private LoginClient loginClient = new LoginClient();
    private LoginCallback loginCallback = new LoginCallback();
    private boolean wentStraightToCreateAccount;

    public static Intent newIntent(@NonNull Context context, @NonNull String source) {
        return newIntent(context, source, null);
    }

    public static Intent newIntent(@NonNull Context context, @NonNull String source,
                                   @Nullable String token) {
        return new Intent(context, LoginActivity.class)
                .putExtra(LOGIN_REQUEST_SOURCE, source)
                .putExtra(EDIT_SESSION_TOKEN, token);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        errorView.setBackClickListener((v) -> onBackPressed());

        errorView.setRetryClickListener((v) -> errorView.setVisibility(View.GONE));

        // Don't allow user to attempt login until they've put in a username and password
        new NonEmptyValidator((isValid) -> loginButton.setEnabled(isValid), usernameInput, passwordInput);

        passwordInput.getEditText().setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateThenLogin();
                return true;
            }
            return false;
        });

        funnel = new LoginFunnel(WikipediaApp.getInstance());

        loginSource = getIntent().getStringExtra(LOGIN_REQUEST_SOURCE);

        if (getIntent().getBooleanExtra(ACTION_CREATE_ACCOUNT, false)) {
            wentStraightToCreateAccount = true;
            startCreateAccountActivity();
        } else if (savedInstanceState == null) {
            // Only send the login start log event if the activity is created for the first time
            logLoginStart();
        }

        // Assume no login by default
        setResult(RESULT_LOGIN_FAIL);
    }

    @OnClick(R.id.login_button) void onLoginClick() {
        validateThenLogin();
    }

    @OnClick(R.id.login_create_account_button) void onCreateAccountClick() {
        startCreateAccountActivity();
    }

    @OnClick(R.id.privacy_policy_link) void onPrivacyPolicyClick() {
        FeedbackUtil.showPrivacyPolicy(this);
    }

    @OnClick(R.id.forgot_password_link) void onForgotPasswordClick() {
        PageTitle title = new PageTitle("Special:PasswordReset", WikipediaApp.getInstance().getWikiSite());
        visitInExternalBrowser(this, Uri.parse(title.getMobileUri()));
    }

    @NonNull private CharSequence getText(@NonNull TextInputLayout input) {
        return input.getEditText() != null ? input.getEditText().getText() : "";
    }

    private void clearErrors() {
        usernameInput.setErrorEnabled(false);
        passwordInput.setErrorEnabled(false);
    }

    private void validateThenLogin() {
        clearErrors();
        if (!CreateAccountActivity.USERNAME_PATTERN.matcher(getText(usernameInput)).matches()) {
            usernameInput.requestFocus();
            usernameInput.setError(getString(R.string.create_account_username_error));
            return;
        }
        doLogin();
    }

    private void logLoginStart() {
        if (loginSource.equals(LoginFunnel.SOURCE_EDIT)) {
            funnel.logStart(
                    LoginFunnel.SOURCE_EDIT,
                    getIntent().getStringExtra(EDIT_SESSION_TOKEN)
            );
        } else {
            funnel.logStart(loginSource);
        }
    }

    private void startCreateAccountActivity() {
        funnel.logCreateAccountAttempt();
        Intent intent = new Intent(this, CreateAccountActivity.class);
        intent.putExtra(CreateAccountActivity.LOGIN_SESSION_TOKEN, funnel.getSessionToken());
        intent.putExtra(CreateAccountActivity.LOGIN_REQUEST_SOURCE, loginSource);
        startActivityForResult(intent, Constants.ACTIVITY_REQUEST_CREATE_ACCOUNT);
    }

    private void onLoginSuccess() {
        funnel.logSuccess();

        hideSoftKeyboard(LoginActivity.this);
        setResult(RESULT_LOGIN_SUCCESS);

        // Set reading list syncing to enabled (without the explicit setup instruction),
        // so that the sync adapter can run at least once and check whether syncing is enabled
        // on the server side.
        Prefs.setReadingListSyncEnabled(true);
        Prefs.shouldShowReadingListSyncMergePrompt(true);
        Prefs.setReadingListPagesDeletedIds(Collections.emptySet());
        Prefs.setReadingListsDeletedIds(Collections.emptySet());
        ReadingListSyncAdapter.manualSyncWithForce();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.ACTIVITY_REQUEST_CREATE_ACCOUNT) {
            if (wentStraightToCreateAccount) {
                logLoginStart();
            }
            if (resultCode == CreateAccountActivity.RESULT_ACCOUNT_CREATED) {
                usernameInput.getEditText().setText(data.getStringExtra(CreateAccountActivity.CREATE_ACCOUNT_RESULT_USERNAME));
                passwordInput.getEditText().setText(data.getStringExtra(CreateAccountActivity.CREATE_ACCOUNT_RESULT_PASSWORD));
                funnel.logCreateAccountSuccess();
                FeedbackUtil.showMessage(this,
                        R.string.create_account_account_created_toast);
                doLogin();
            } else {
                funnel.logCreateAccountFailure();
            }
        } else if (requestCode == Constants.ACTIVITY_REQUEST_RESET_PASSWORD
                && resultCode == ResetPasswordActivity.RESULT_PASSWORD_RESET_SUCCESS) {
            onLoginSuccess();
        }
    }

    private void doLogin() {
        final String username = getText(usernameInput).toString();
        final String password = getText(passwordInput).toString();
        final String twoFactorCode = getText(twoFactorText).toString();

        showProgressBar(true);

        if (!TextUtils.isEmpty(twoFactorCode) && !TextUtils.isEmpty(firstStepToken)) {
            loginClient.login(WikipediaApp.getInstance().getWikiSite(), username, password,
                    null, twoFactorCode, firstStepToken, loginCallback);
        } else {
            loginClient.request(WikipediaApp.getInstance().getWikiSite(), username, password,
                    loginCallback);
        }
    }

    private class LoginCallback implements LoginClient.LoginCallback {
        @Override
        public void success(@NonNull LoginResult result) {
            showProgressBar(false);
            if (result.pass()) {

                Bundle extras = getIntent().getExtras();
                AccountAuthenticatorResponse response = extras == null ? null
                        : extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                AccountUtil.updateAccount(response, result);

                onLoginSuccess();

            } else if (result.fail()) {
                String message = result.getMessage();
                FeedbackUtil.showMessage(LoginActivity.this, message);
                funnel.logError(message);
                L.w("Login failed with result " + message);
            }
        }

        @Override
        public void twoFactorPrompt(@NonNull Throwable caught, @Nullable String token) {
            showProgressBar(false);
            firstStepToken = token;
            twoFactorText.setVisibility(View.VISIBLE);
            twoFactorText.requestFocus();
            FeedbackUtil.showError(LoginActivity.this, caught);
        }

        @Override
        public void passwordResetPrompt(@Nullable String token) {
            startActivityForResult(ResetPasswordActivity.newIntent(LoginActivity.this,
                    getText(usernameInput).toString(), token), Constants.ACTIVITY_REQUEST_RESET_PASSWORD);
        }

        @Override
        public void error(@NonNull Throwable caught) {
            showProgressBar(false);
            if (caught instanceof LoginClient.LoginFailedException) {
                FeedbackUtil.showError(LoginActivity.this, caught);
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
        loginClient.cancel();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("loginShowing", true);
    }

    private void showError(@NonNull Throwable caught) {
        errorView.setError(caught);
        errorView.setVisibility(View.VISIBLE);
    }
}
