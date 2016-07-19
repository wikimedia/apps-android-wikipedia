package org.wikipedia.createaccount;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.ConfirmPassword;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Password;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.NonEmptyValidator;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.ActivityUtil;
import org.wikipedia.activity.ThemedActionBarActivity;
import org.wikipedia.analytics.CreateAccountFunnel;
import org.wikipedia.editing.CaptchaHandler;
import org.wikipedia.editing.CaptchaResult;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.PasswordTextInput;

import java.util.List;

import static org.wikipedia.util.FeedbackUtil.setErrorPopup;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class CreateAccountActivity extends ThemedActionBarActivity {
    public static final int RESULT_ACCOUNT_CREATED = 1;
    public static final int RESULT_ACCOUNT_NOT_CREATED = 2;

    public static final int ACTION_CREATE_ACCOUNT = 1;

    public static final String LOGIN_REQUEST_SOURCE = "login_request_source";
    public static final String LOGIN_SESSION_TOKEN = "login_session_token";

    @NotEmpty
    private EditText usernameEdit;
    @Password()
    private EditText passwordEdit;
    @ConfirmPassword(messageResId = R.string.create_account_passwords_mismatch_error)
    private EditText passwordRepeatEdit;
    // TODO: remove and replace with @Optional annotation once it's available in the library
    // https://github.com/ragunathjawahar/android-saripaar/issues/102
    @OptionalEmail(messageResId = R.string.create_account_email_error)
    private EditText emailEdit;

    private Button createAccountButton;
    private Button createAccountButtonCaptcha;

    private ProgressDialog progressDialog;

    private CaptchaHandler captchaHandler;

    private CreateAccountResult createAccountResult;

    private Validator validator;

    private CreateAccountFunnel funnel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        usernameEdit = (EditText) findViewById(R.id.create_account_username);
        passwordRepeatEdit = (EditText) findViewById(R.id.create_account_password_repeat);
        emailEdit = (EditText) findViewById(R.id.create_account_email);
        createAccountButton = (Button) findViewById(R.id.create_account_submit_button);
        createAccountButtonCaptcha = (Button) findViewById(R.id.captcha_submit_button);
        EditText captchaText = (EditText) findViewById(R.id.captcha_text);
        View primaryContainer = findViewById(R.id.create_account_primary_container);
        PasswordTextInput passwordInput = (PasswordTextInput) findViewById(R.id.create_account_password_input);
        passwordEdit = passwordInput.getEditText();

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.dialog_create_account_checking_progress));

        captchaHandler = new CaptchaHandler(this, WikipediaApp.getInstance().getSite(),
                progressDialog, primaryContainer, getString(R.string.create_account_activity_title),
                getString(R.string.create_account_button));

        // We enable the menu item as soon as the username and password fields are filled
        // Tapping does further validation
        validator = new Validator(this);
        Validator.registerAnnotation(OptionalEmail.class);
        validator.setValidationListener(new Validator.ValidationListener() {
            @Override
            public void onValidationSucceeded() {
                if (captchaHandler.isActive() && captchaHandler.token() != null) {
                    doCreateAccount(captchaHandler.token());
                } else {
                    getCreateAccountInfo();
                }
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

        // Don't allow user to submit registration unless they've put in a username and password
        new NonEmptyValidator(new NonEmptyValidator.ValidationChangedCallback() {
            @Override
            public void onValidationChanged(boolean isValid) {
                createAccountButton.setEnabled(isValid);
            }
        }, usernameEdit, passwordEdit, passwordRepeatEdit);

        // Don't allow user to continue when they're shown a captcha until they fill it in
        new NonEmptyValidator(new NonEmptyValidator.ValidationChangedCallback() {
            @Override
            public void onValidationChanged(boolean isValid) {
                createAccountButtonCaptcha.setEnabled(isValid);
            }
        }, captchaText);

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validator.validate();
            }
        });

        createAccountButtonCaptcha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validator.validate();
            }
        });

        // Add listener so that when the user taps enter, it submits the captcha
        captchaText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_UP) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    validator.validate();
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey("result")) {
            createAccountResult = savedInstanceState.getParcelable("result");
        }

        findViewById(R.id.create_account_login_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // already coming from LoginActivity
                finish();
            }
        });

        funnel = new CreateAccountFunnel(WikipediaApp.getInstance(),
                getIntent().getStringExtra(LOGIN_REQUEST_SOURCE));

        // Only send the editing start log event if the activity is created for the first time
        if (savedInstanceState == null) {
            funnel.logStart(getIntent().getStringExtra(LOGIN_SESSION_TOKEN));
        }
        // Set default result to failed, so we can override if it did not
        setResult(RESULT_ACCOUNT_NOT_CREATED);
    }

    @Override
    protected void setTheme() {
        setActionBarTheme();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("result", createAccountResult);
    }

    public void showPrivacyPolicy(View v) {
        FeedbackUtil.showPrivacyPolicy(this);
    }

    public void handleError(@NonNull String message) {
        FeedbackUtil.showMessage(this, message);
        L.w("Account creation failed with result " + message);
    }

    public void getCreateAccountInfo() {
        new CreateAccountInfoTask() {
            @Override
            public void onCatch(Throwable caught) {
                handleError(caught.getMessage());
                L.e(caught);
            }

            @Override
            public void onFinish(CreateAccountInfoResult result) {
                if (result.token() == null) {
                    handleError(getString(R.string.create_account_generic_error));
                } else if (result.hasCaptcha()) {
                    captchaHandler.handleCaptcha(result.token(), new CaptchaResult(result.captchaId()));
                } else {
                    doCreateAccount(result.token());
                }
            }
        }.execute();
    }

    public void doCreateAccount(@NonNull String token) {
        String email = null;
        if (emailEdit.getText().length() != 0) {
            email = emailEdit.getText().toString();
        }
        new CreateAccountTask(usernameEdit.getText().toString(), passwordEdit.getText().toString(),
                              passwordRepeatEdit.getText().toString(), token, email) {
            @Override
            public void onBeforeExecute() {
                progressDialog.show();
            }

            @Override
            public RequestBuilder buildRequest(Api api) {
                if (captchaHandler.isActive()) {
                    return captchaHandler.populateBuilder(super.buildRequest(api));
                }
                return super.buildRequest(api);
            }

            @Override
            public void onCatch(Throwable caught) {
                L.d("Caught " + caught.toString());
                if (!progressDialog.isShowing()) {
                    // no longer attached to activity!
                    return;
                }
                progressDialog.dismiss();
                FeedbackUtil.showError(CreateAccountActivity.this, caught);
            }

            @Override
            public void onFinish(final CreateAccountResult result) {
                if (!progressDialog.isShowing()) {
                    // no longer attached to activity!
                    return;
                }
                createAccountResult = result;
                if (result instanceof CreateAccountSuccessResult) {
                    progressDialog.dismiss();
                    captchaHandler.cancelCaptcha();
                    funnel.logSuccess();
                    hideSoftKeyboard(CreateAccountActivity.this);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("username", ((CreateAccountSuccessResult) result).getUsername());
                    resultIntent.putExtra("password", passwordEdit.getText().toString());
                    setResult(RESULT_ACCOUNT_CREATED, resultIntent);
                    finish();
                } else {
                    progressDialog.dismiss();
                    captchaHandler.cancelCaptcha();
                    handleError(result.getMessage());
                }
            }
        }.execute();
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

    @Override
    public void onStop() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onStop();
    }
}
