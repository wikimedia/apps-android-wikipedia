package org.wikipedia.createaccount;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.CreateAccountFunnel;
import org.wikipedia.captcha.CaptchaHandler;
import org.wikipedia.captcha.CaptchaResult;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.NonEmptyValidator;
import org.wikipedia.views.WikiErrorView;

import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class CreateAccountActivity extends BaseActivity {
    public static final int RESULT_ACCOUNT_CREATED = 1;
    public static final int RESULT_ACCOUNT_NOT_CREATED = 2;

    public static final int ACTION_CREATE_ACCOUNT = 1;

    public static final String LOGIN_REQUEST_SOURCE = "login_request_source";
    public static final String LOGIN_SESSION_TOKEN = "login_session_token";
    public static final String CREATE_ACCOUNT_RESULT_USERNAME = "username";
    public static final String CREATE_ACCOUNT_RESULT_PASSWORD = "password";

    public static final Pattern USERNAME_PATTERN = Pattern.compile("[^#<>\\[\\]|{}\\/@]*");
    private static final int PASSWORD_MIN_LENGTH = 6;

    enum ValidateResult {
        SUCCESS, INVALID_USERNAME, INVALID_PASSWORD, PASSWORD_MISMATCH, INVALID_EMAIL
    }

    private CreateAccountInfoClient createAccountInfoClient;
    private CreateAccountClient createAccountClient;

    @BindView(R.id.create_account_primary_container) View primaryContainer;
    @BindView(R.id.create_account_onboarding_container) View onboardingContainer;
    @BindView(R.id.create_account_username) TextInputLayout usernameInput;
    @BindView(R.id.create_account_password_input) TextInputLayout passwordInput;
    @BindView(R.id.create_account_password_repeat) TextInputLayout passwordRepeatInput;
    @BindView(R.id.create_account_email) TextInputLayout emailInput;
    @BindView(R.id.create_account_submit_button) TextView createAccountButton;
    @BindView(R.id.view_create_account_error) WikiErrorView errorView;
    @BindView(R.id.captcha_text) TextInputLayout captchaText;
    @BindView(R.id.captcha_submit_button) TextView createAccountButtonCaptcha;

    private ProgressDialog progressDialog;
    private CaptchaHandler captchaHandler;
    private CreateAccountResult createAccountResult;
    private CreateAccountFunnel funnel;
    private WikiSite wiki;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        ButterKnife.bind(this);

        if (ReadingListSyncAdapter.isDisabledByRemoteConfig()) {
            onboardingContainer.setVisibility(View.GONE);
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.dialog_create_account_checking_progress));

        errorView.setBackClickListener((v) -> onBackPressed());
        errorView.setRetryClickListener((v) -> errorView.setVisibility(View.GONE));

        wiki = WikipediaApp.getInstance().getWikiSite();
        createAccountInfoClient = new CreateAccountInfoClient();
        createAccountClient = new CreateAccountClient();

        captchaHandler = new CaptchaHandler(this, WikipediaApp.getInstance().getWikiSite(),
                progressDialog, primaryContainer, getString(R.string.create_account_activity_title),
                getString(R.string.create_account_button));

        // Don't allow user to submit registration unless they've put in a username and password
        new NonEmptyValidator((isValid) -> createAccountButton.setEnabled(isValid), usernameInput, passwordInput);

        // Don't allow user to continue when they're shown a captcha until they fill it in
        new NonEmptyValidator((isValid) -> createAccountButtonCaptcha.setEnabled(isValid), captchaText);

        // Add listener so that when the user taps enter, it submits the captcha
        captchaText.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_UP) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                validateThenCreateAccount();
                return true;
            }
            return false;
        });

        if (savedInstanceState != null && savedInstanceState.containsKey("result")) {
            createAccountResult = savedInstanceState.getParcelable("result");
        }

        funnel = new CreateAccountFunnel(WikipediaApp.getInstance(),
                getIntent().getStringExtra(LOGIN_REQUEST_SOURCE));

        // Only send the editing start log event if the activity is created for the first time
        if (savedInstanceState == null) {
            funnel.logStart(getIntent().getStringExtra(LOGIN_SESSION_TOKEN));
        }
        // Set default result to failed, so we can override if it did not
        setResult(RESULT_ACCOUNT_NOT_CREATED);
    }

    @OnClick({R.id.create_account_submit_button, R.id.captcha_submit_button}) void onCreateAccountClick() {
        validateThenCreateAccount();
    }

    @OnClick(R.id.create_account_login_button) void onLoginClick() {
        // This assumes that the CreateAccount activity was launched from the Login activity
        // (since there's currently no other mechanism to invoke CreateAccountActivity),
        // so finishing this activity will implicitly go back to Login.
        finish();
    }

    @OnClick(R.id.privacy_policy_link) void onPrivacyPolicyClick() {
        FeedbackUtil.showPrivacyPolicy(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("result", createAccountResult);
    }

    public void handleAccountCreationError(@NonNull String message) {
        FeedbackUtil.showMessage(this, message);
        L.w("Account creation failed with result " + message);
    }

    public void getCreateAccountInfo() {
        createAccountInfoClient.request(wiki, new CreateAccountInfoClient.Callback() {
            @Override
            public void success(@NonNull Call<MwQueryResponse> call,
                                @NonNull CreateAccountInfoResult result) {
                if (result.token() == null) {
                    handleAccountCreationError(getString(R.string.create_account_generic_error));
                } else if (result.hasCaptcha()) {
                    captchaHandler.handleCaptcha(result.token(), new CaptchaResult(result.captchaId()));
                } else {
                    doCreateAccount(result.token());
                }
            }

            @Override
            public void failure(@NonNull Call<MwQueryResponse> call,
                                @NonNull Throwable caught) {
                showError(caught);
                L.e(caught);
            }
        });
    }

    public void doCreateAccount(@NonNull String token) {
        progressDialog.show();

        String email = null;
        if (getText(emailInput).length() != 0) {
            email = getText(emailInput).toString();
        }
        String password = getText(passwordInput).toString();
        String repeat = getText(passwordRepeatInput).toString();

        createAccountClient.request(wiki, getText(usernameInput).toString(),
                password, repeat, token, email,
                captchaHandler.isActive() ? captchaHandler.captchaId() : "null",
                captchaHandler.isActive() ? captchaHandler.captchaWord() : "null",
                new CreateAccountClient.Callback() {
                    @Override
                    public void success(@NonNull Call<CreateAccountResponse> call,
                                        @NonNull final CreateAccountSuccessResult result) {
                        if (!progressDialog.isShowing()) {
                            // no longer attached to activity!
                            return;
                        }
                        finishWithUserResult(result);

                    }

                    @Override
                    public void failure(@NonNull Call<CreateAccountResponse> call, @NonNull Throwable caught) {
                        L.e(caught.toString());
                        if (!progressDialog.isShowing()) {
                            // no longer attached to activity!
                            return;
                        }
                        progressDialog.dismiss();
                        if (caught instanceof CreateAccountException) {
                            handleAccountCreationError(caught.getMessage());
                        } else {
                            showError(caught);
                        }
                    }
                });
    }

    @Override
    public void onBackPressed() {
        hideSoftKeyboard(this);
        super.onBackPressed();
    }

    @Override
    public void onStop() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onStop();
    }

    private void clearErrors() {
        usernameInput.setErrorEnabled(false);
        passwordInput.setErrorEnabled(false);
        passwordRepeatInput.setErrorEnabled(false);
        emailInput.setErrorEnabled(false);
    }

    private void validateThenCreateAccount() {
        clearErrors();
        ValidateResult result = validateInput(getText(usernameInput), getText(passwordInput),
                getText(passwordRepeatInput), getText(emailInput));

        switch (result) {
            case INVALID_USERNAME:
                usernameInput.requestFocus();
                usernameInput.setError(getString(R.string.create_account_username_error));
                return;
            case INVALID_PASSWORD:
                passwordInput.requestFocus();
                passwordInput.setError(getString(R.string.create_account_password_error));
                return;
            case PASSWORD_MISMATCH:
                passwordRepeatInput.requestFocus();
                passwordRepeatInput.setError(getString(R.string.create_account_passwords_mismatch_error));
                return;
            case INVALID_EMAIL:
                emailInput.requestFocus();
                emailInput.setError(getString(R.string.create_account_email_error));
                return;
            case SUCCESS:
            default:
                break;
        }

        if (captchaHandler.isActive() && captchaHandler.token() != null) {
            doCreateAccount(captchaHandler.token());
        } else {
            getCreateAccountInfo();
        }
    }

    @VisibleForTesting
    static ValidateResult validateInput(@NonNull CharSequence username,
                                         @NonNull CharSequence password,
                                         @NonNull CharSequence passwordRepeat,
                                         @NonNull CharSequence email) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return ValidateResult.INVALID_USERNAME;
        } else if (password.length() < PASSWORD_MIN_LENGTH) {
            return ValidateResult.INVALID_PASSWORD;
        } else if (!passwordRepeat.toString().equals(password.toString())) {
            return ValidateResult.PASSWORD_MISMATCH;
        } else if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidateResult.INVALID_EMAIL;
        }
        return ValidateResult.SUCCESS;
    }

    @NonNull private CharSequence getText(@NonNull TextInputLayout input) {
        return input.getEditText() != null ? input.getEditText().getText() : "";
    }

    private void finishWithUserResult(@NonNull CreateAccountSuccessResult result) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(CREATE_ACCOUNT_RESULT_USERNAME, result.getUsername());
        resultIntent.putExtra(CREATE_ACCOUNT_RESULT_PASSWORD, getText(passwordInput).toString());
        setResult(RESULT_ACCOUNT_CREATED, resultIntent);

        createAccountResult = result;
        progressDialog.dismiss();
        captchaHandler.cancelCaptcha();
        funnel.logSuccess();
        hideSoftKeyboard(CreateAccountActivity.this);
        finish();
    }

    private void showError(@NonNull Throwable caught) {
        errorView.setError(caught);
        errorView.setVisibility(View.VISIBLE);
    }
}
