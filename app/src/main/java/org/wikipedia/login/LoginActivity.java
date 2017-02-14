package org.wikipedia.login;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.PasswordTextInput;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Pattern;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.ActivityUtil;
import org.wikipedia.activity.ThemedActionBarActivity;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.createaccount.CreateAccountActivity;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.NonEmptyValidator;

import java.util.List;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.FeedbackUtil.setErrorPopup;

public class LoginActivity extends ThemedActionBarActivity {
    public static final int RESULT_LOGIN_SUCCESS = 1;
    public static final int RESULT_LOGIN_FAIL = 2;

    public static final String LOGIN_REQUEST_SOURCE = "login_request_source";
    public static final String EDIT_SESSION_TOKEN = "edit_session_token";
    public static final String ACTION_CREATE_ACCOUNT = "action_create_account";

    @Pattern(regex = "[^#<>\\[\\]|{}\\/@]*", messageResId = R.string.create_account_username_error)
    private EditText usernameText;
    private EditText passwordText;
    private EditText twoFactorText;
    private View loginButton;
    private ProgressDialog progressDialog;
    @Nullable private String firstStepToken;

    private LoginFunnel funnel;
    private String loginSource;
    private LoginClient loginClient;
    private boolean wentStraightToCreateAccount;
    private Validator validator;

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
        setContentView(R.layout.activity_wiki_login);

        usernameText = (EditText) findViewById(R.id.login_username_text);
        passwordText = ((PasswordTextInput) findViewById(R.id.login_password_input)).getEditText();
        twoFactorText = (EditText) findViewById(R.id.login_2fa_text);
        View createAccountLink = findViewById(R.id.login_create_account_link);

        // We enable the login button as soon as the username and password fields are filled
        // Tapping does further validation
        validator = new Validator(this);
        validator.setValidationListener(new Validator.ValidationListener() {
            @Override
            public void onValidationSucceeded() {
                doLogin();
            }

            @Override
            public void onValidationFailed(List<ValidationError> errors) {
                for (ValidationError error : errors) {
                    View view = error.getView();
                    String message = error.getCollatedErrorMessage(view.getContext());
                    if (view instanceof EditText) {
                        //Request focus on the EditText before setting error, so that error is visible
                        view.requestFocus();
                        setErrorPopup((EditText) view, message);
                    } else {
                        throw new RuntimeException("This should not be happening");
                    }
                }
            }
        });

        // Don't allow user to attempt login until they've put in a username and password
        new NonEmptyValidator(new NonEmptyValidator.ValidationChangedCallback() {
            @Override
            public void onValidationChanged(boolean isValid) {
                loginButton.setEnabled(isValid);
            }
        }, usernameText, passwordText);

        passwordText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    validator.validate();
                    return true;
                }
                return false;
            }
        });

        loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validator.validate();
            }
        });

        createAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCreateAccountActivity();
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.login_in_progress_dialog_message));
        progressDialog.setCancelable(false);

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

    public void showPrivacyPolicy(View v) {
        FeedbackUtil.showPrivacyPolicy(this);
    }

    @Override
    protected void setTheme() {
        setActionBarTheme();
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
        startActivityForResult(intent, CreateAccountActivity.ACTION_CREATE_ACCOUNT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CreateAccountActivity.ACTION_CREATE_ACCOUNT) {
            if (wentStraightToCreateAccount) {
                logLoginStart();
            }
            if (resultCode == CreateAccountActivity.RESULT_ACCOUNT_CREATED) {
                usernameText.setText(data.getStringExtra(CreateAccountActivity.CREATE_ACCOUNT_RESULT_USERNAME));
                passwordText.setText(data.getStringExtra(CreateAccountActivity.CREATE_ACCOUNT_RESULT_PASSWORD));
                funnel.logCreateAccountSuccess();
                FeedbackUtil.showMessage(this,
                        R.string.create_account_account_created_toast);
                doLogin();
            } else {
                funnel.logCreateAccountFailure();
            }
        }
    }

    private void doLogin() {
        final String username = usernameText.getText().toString();
        final String password = passwordText.getText().toString();
        final String twoFactorCode = twoFactorText.getText().toString();

        if (loginClient == null) {
            loginClient = new LoginClient();
        }
        progressDialog.show();

        if (!twoFactorCode.isEmpty()) {
            loginClient.login(WikipediaApp.getInstance().getWikiSite(), username, password,
                    twoFactorCode, firstStepToken, getCallback(username, password));
        } else {
            loginClient.request(WikipediaApp.getInstance().getWikiSite(), username, password,
                    getCallback(username, password));
        }
    }

    private LoginClient.LoginCallback getCallback(@NonNull final String username,
                                                  @NonNull final String password) {
        return new LoginClient.LoginCallback() {
            @Override
            public void success(@NonNull LoginResult result) {
                if (!progressDialog.isShowing()) {
                    // no longer attached to activity!
                    return;
                }
                progressDialog.dismiss();
                if (result.pass()) {
                    funnel.logSuccess();

                    Bundle extras = getIntent().getExtras();
                    AccountAuthenticatorResponse response = extras == null
                            ? null
                            : extras.<AccountAuthenticatorResponse>getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                    AccountUtil.createAccount(response, username, password);

                    hideSoftKeyboard(LoginActivity.this);
                    setResult(RESULT_LOGIN_SUCCESS);

                    finish();
                } else if (result.fail()) {
                    funnel.logError(result.getMessage());
                    handleError(result.getMessage());
                }
            }

            @Override
            public void twoFactorPrompt(@NonNull Throwable caught, @NonNull String token) {
                if (!progressDialog.isShowing()) {
                    // no longer attached to activity!
                    return;
                }
                progressDialog.dismiss();
                firstStepToken = token;
                twoFactorText.setVisibility(View.VISIBLE);
                twoFactorText.requestFocus();
                FeedbackUtil.showError(LoginActivity.this, caught);
            }

            @Override
            public void error(@NonNull Throwable caught) {
                if (!progressDialog.isShowing()) {
                    // no longer attached to activity!
                    return;
                }
                progressDialog.dismiss();
                FeedbackUtil.showError(LoginActivity.this, caught);
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return ActivityUtil.defaultOnOptionsItemSelected(this, item)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        hideSoftKeyboard(this);
        super.onBackPressed();
    }

    private void handleError(String message) {
        FeedbackUtil.showMessage(this, message);
        L.e("Login failed with result " + message);
    }

    @Override
    public void onStop() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("loginShowing", true);
    }
}
