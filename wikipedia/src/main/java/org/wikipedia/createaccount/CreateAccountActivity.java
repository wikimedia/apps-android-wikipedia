package org.wikipedia.createaccount;

import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v7.app.*;
import android.view.*;
import android.widget.*;
import com.mobsandgeeks.saripaar.*;
import com.mobsandgeeks.saripaar.annotation.*;
import de.keyboardsurfer.android.widget.crouton.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.analytics.*;
import org.wikipedia.editing.*;

public class CreateAccountActivity extends ActionBarActivity {
    public static final int RESULT_ACCOUNT_CREATED = 1;
    public static final int RESULT_ACCOUNT_NOT_CREATED = 2;

    public static final int ACTION_CREATE_ACCOUNT = 1;

    public static final String LOGIN_SESSION_TOKEN = "login_session_token";

    @Required(order = 1)
    private EditText usernameEdit;
    @Required(order = 2)
    @Password(order = 3)
    private EditText passwordEdit;
    @ConfirmPassword(order = 4, messageResId = R.string.create_account_passwords_mismatch_error)
    private EditText passwordRepeatEdit;
    @Email(order = 5, messageResId = R.string.create_account_email_error)
    private EditText emailEdit;

    private CheckBox showPasswordCheck;
    private CheckBox showPasswordRepeatCheck;

    private View primaryContainer;

    private WikipediaApp app;

    private ProgressDialog progressDialog;

    private CaptchaHandler captchaHandler;

    private NonEmptyValidator nonEmptyValidator;

    private CreateAccountResult createAccountResult;

    private Validator validator;

    private CreateAccountFunnel funnel;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        app = (WikipediaApp) getApplicationContext();

        usernameEdit = (EditText) findViewById(R.id.create_account_username);
        passwordEdit = (EditText) findViewById(R.id.create_account_password);
        passwordRepeatEdit = (EditText) findViewById(R.id.create_account_password_repeat);
        emailEdit = (EditText) findViewById(R.id.create_account_email);
        primaryContainer = findViewById(R.id.create_account_primary_container);
        showPasswordCheck = (CheckBox) findViewById(R.id.create_account_show_password);
        showPasswordRepeatCheck = (CheckBox) findViewById(R.id.create_account_show_password_repeat);

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.dialog_create_account_checking_progress));

        captchaHandler = new CaptchaHandler(this, app.getPrimarySite(), progressDialog, primaryContainer, R.string.create_account_activity_title);

        // We enable the menu item as soon as the username and password fields are filled
        // Tapping does further validation
        validator = new Validator(this);
        validator.setValidationListener(new Validator.ValidationListener() {
            @Override
            public void onValidationSucceeded() {
                doCreateAccount();
            }

            @Override
            public void onValidationFailed(View view, Rule<?> rule) {
                if (view instanceof EditText) {
                    ((EditText) view).setError(rule.getFailureMessage());
                } else {
                    throw new RuntimeException("This should not be happening");
                }
            }
        });

        nonEmptyValidator = new NonEmptyValidator(new NonEmptyValidator.ValidationChangedCallback() {
            @Override
            public void onValidationChanged(boolean isValid) {
                supportInvalidateOptionsMenu();
            }
        }, usernameEdit, passwordEdit, passwordRepeatEdit);

        Utils.setupShowPasswordCheck(showPasswordCheck, passwordEdit);
        Utils.setupShowPasswordCheck(showPasswordRepeatCheck, passwordRepeatEdit);

        if (savedInstanceState != null && savedInstanceState.containsKey("result")) {
            createAccountResult = savedInstanceState.getParcelable("result");
            if (createAccountResult instanceof CreateAccountCaptchaResult) {
                captchaHandler.handleCaptcha(((CreateAccountCaptchaResult) createAccountResult).getCaptchaResult());
            }
        }

        funnel = new CreateAccountFunnel(app);

        funnel.logStart(getIntent().getStringExtra(LOGIN_SESSION_TOKEN));

        // Set default result to failed, so we can override if it did not
        setResult(RESULT_ACCOUNT_NOT_CREATED);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("result", createAccountResult);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_create_account, menu);
        menu.findItem(R.id.menu_create_account).setEnabled(nonEmptyValidator.isValid());
        return true;
    }

    public void handleError(CreateAccountResult result) {
        String errorCode = result.getResult();
        if (errorCode.equals("userexists")) {
            usernameEdit.setError(getString(R.string.create_account_username_exists_error));
        } else if (errorCode.equals("acct_creation_throttle_hit")) {
            Crouton.makeText(this, R.string.create_account_ip_throttle_error, Style.ALERT).show();
        } else if (errorCode.equals("sorbs_create_account_reason")) {
            Crouton.makeText(this, R.string.create_account_open_proxy_error, Style.ALERT).show();
        } else {
            Crouton.makeText(this, R.string.create_account_generic_error, Style.ALERT).show();
        }
    }

    public void doCreateAccount() {
        String email = null;
        if (emailEdit.getText().length() != 0) {
            email = emailEdit.getText().toString();
        }
        new CreateAccountTask(this, usernameEdit.getText().toString(), passwordEdit.getText().toString(), email) {
            @Override
            public void onBeforeExecute() {
                progressDialog.show();
            }

            @Override
            public RequestBuilder buildRequest(Api api) {
                if (createAccountResult != null && createAccountResult instanceof CreateAccountCaptchaResult) {
                   return captchaHandler.populateBuilder(super.buildRequest(api));
                }
                return super.buildRequest(api);
            }

            @Override
            public void onFinish(final CreateAccountResult result) {
                createAccountResult = result;
                if (result instanceof CreateAccountCaptchaResult) {
                    if (captchaHandler.isActive()) {
                        funnel.logCaptchaFailure();
                    } else {
                        funnel.logCaptchaShown();
                    }
                    captchaHandler.handleCaptcha(((CreateAccountCaptchaResult)result).getCaptchaResult());
                } else {
                    progressDialog.dismiss();
                    captchaHandler.cancelCaptcha();
                    // Returns lowercase 'success', unlike every other API. GRR man, GRR
                    // Replace wen https://bugzilla.wikimedia.org/show_bug.cgi?id=61663 is fixed?
                    if (result.getResult().toLowerCase().equals("success")) {
                        funnel.logSuccess();
                        Utils.hideSoftKeyboard(CreateAccountActivity.this);
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("username", usernameEdit.getText().toString());
                        resultIntent.putExtra("password", passwordEdit.getText().toString());
                        setResult(RESULT_ACCOUNT_CREATED, resultIntent);
                        Toast.makeText(CreateAccountActivity.this, R.string.create_account_account_created_toast, Toast.LENGTH_LONG).show();
                        finish();
                    } else if (result.getResult().equals("captcha-createaccount-fail")) {
                        // So for now we just need to do the entire set of requests again. sigh
                        // Eventually this should be fixed to have the new captcha info come back.
                        createAccountResult = null;
                        doCreateAccount();
                    } else {
                        funnel.logError(result.getResult());
                        handleError(result);
                    }
                }
            }
        }.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Utils.hideSoftKeyboard(this);
                finish();
                return true;
            case R.id.menu_create_account:
                validator.validate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}