package org.wikipedia.createaccount;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.ConfirmPassword;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mobsandgeeks.saripaar.annotation.Required;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.*;
import org.wikipedia.analytics.CreateAccountFunnel;
import org.wikipedia.editing.CaptchaHandler;


public class CreateAccountActivity extends ThemedActionBarActivity {
    public static final int RESULT_ACCOUNT_CREATED = 1;
    public static final int RESULT_ACCOUNT_NOT_CREATED = 2;

    public static final int ACTION_CREATE_ACCOUNT = 1;

    public static final String LOGIN_REQUEST_SOURCE = "login_request_source";
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

    private Button createAccountButton;
    private Button createAccountButtonCaptcha;

    private WikipediaApp app;

    private ProgressDialog progressDialog;

    private CaptchaHandler captchaHandler;

    private CreateAccountResult createAccountResult;

    private Validator validator;

    private CreateAccountFunnel funnel;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (WikipediaApp) getApplicationContext();

        setContentView(R.layout.activity_create_account);

        usernameEdit = (EditText) findViewById(R.id.create_account_username);
        passwordEdit = (EditText) findViewById(R.id.create_account_password);
        passwordRepeatEdit = (EditText) findViewById(R.id.create_account_password_repeat);
        emailEdit = (EditText) findViewById(R.id.create_account_email);
        showPasswordCheck = (CheckBox) findViewById(R.id.create_account_show_password);
        createAccountButton = (Button) findViewById(R.id.create_account_submit_button);
        createAccountButtonCaptcha = (Button) findViewById(R.id.captcha_submit_button);
        EditText captchaText = (EditText) findViewById(R.id.captcha_text);
        View primaryContainer = findViewById(R.id.create_account_primary_container);

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.dialog_create_account_checking_progress));

        captchaHandler = new CaptchaHandler(this, app.getPrimarySite(), progressDialog, primaryContainer,
                                            getString(R.string.create_account_activity_title),
                                            getString(R.string.create_account_button));

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
                    //Request focus on the EditText before setting error, so that error is visible
                    view.requestFocus();
                    ((EditText) view).setError(rule.getFailureMessage());
                } else {
                    throw new RuntimeException("This should not be happening");
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

        Utils.setupShowPasswordCheck(showPasswordCheck, passwordEdit);
        showPasswordCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (showPasswordCheck.isChecked()) {
                    ViewAnimations.slideOutRight(passwordRepeatEdit, new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            // give it nonempty text, to appease NonEmptyValidator
                            passwordRepeatEdit.setText(" ");
                            passwordRepeatEdit.setVisibility(View.GONE);
                        }
                    });
                } else {
                    ViewAnimations.slideIn(passwordRepeatEdit, new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            passwordRepeatEdit.setText("");
                            passwordRepeatEdit.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey("result")) {
            createAccountResult = savedInstanceState.getParcelable("result");
            if (createAccountResult instanceof CreateAccountCaptchaResult) {
                captchaHandler.handleCaptcha(((CreateAccountCaptchaResult) createAccountResult).getCaptchaResult());
            }
        }

        findViewById(R.id.create_account_login_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // already coming from LoginActivity
                finish();
            }
        });

        funnel = new CreateAccountFunnel(app, getIntent().getStringExtra(LOGIN_REQUEST_SOURCE));

        // Only send the editing start log event if the activity is created for the first time
        if (savedInstanceState == null) {
            funnel.logStart(getIntent().getStringExtra(LOGIN_SESSION_TOKEN));
        }
        // Set default result to failed, so we can override if it did not
        setResult(RESULT_ACCOUNT_NOT_CREATED);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("result", createAccountResult);
    }

    public void handleError(CreateAccountResult result) {
        String errorCode = result.getResult();
        switch (errorCode) {
            case "blocked":
                if (app.getUserInfoStorage().isLoggedIn()) {
                    Crouton.makeText(this, R.string.create_account_blocked_error, Style.ALERT).show();
                } else {
                    Crouton.makeText(this, R.string.create_account_blocked_anon_error, Style.ALERT).show();
                }
                break;
            case "acct_creation_throttle_hit":
                Crouton.makeText(this, R.string.create_account_ip_throttle_error, Style.ALERT).show();
                break;
            case "sorbs_create_account_reason":
                Crouton.makeText(this, R.string.create_account_open_proxy_error, Style.ALERT).show();
                break;
            case "userexists":
                //Request focus before displaying error message, so that it pops up on its own
                usernameEdit.requestFocus();
                usernameEdit.setError(getString(R.string.create_account_username_exists_error));
                break;
            case "noname":
                Crouton.makeText(this, R.string.create_account_noname_error, Style.ALERT).show();
                break;
            case "invalidemailaddress":
                Crouton.makeText(this, R.string.create_account_invalid_email_error, Style.ALERT).show();
                break;
            case "passwordtooshort":
                //FIXME: Find the value of $wgMinimalPasswordLength and tell the user the minimum pwd length
                Crouton.makeText(this, R.string.create_account_password_too_short_error, Style.ALERT).show();
                break;
            case "password-name-match":
                Crouton.makeText(this, R.string.create_account_password_name_match_error, Style.ALERT).show();
                break;
            default:
                Crouton.makeText(this, R.string.create_account_generic_error, Style.ALERT).show();
                break;
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
            public void onCatch(Throwable caught) {
                Log.d("Wikipedia", "Caught " + caught.toString());
                if (!progressDialog.isShowing()) {
                    // no longer attached to activity!
                    return;
                }
                progressDialog.dismiss();
                Crouton.makeText(CreateAccountActivity.this, R.string.create_account_no_network, Style.ALERT).show();
            }

            @Override
            public void onFinish(final CreateAccountResult result) {
                if (!progressDialog.isShowing()) {
                    // no longer attached to activity!
                    return;
                }
                createAccountResult = result;
                if (result instanceof CreateAccountCaptchaResult) {
                    if (captchaHandler.isActive()) {
                        funnel.logCaptchaFailure();
                    } else {
                        funnel.logCaptchaShown();
                    }
                    captchaHandler.handleCaptcha(((CreateAccountCaptchaResult)result).getCaptchaResult());
                } else if (result instanceof CreateAccountSuccessResult) {
                    progressDialog.dismiss();
                    captchaHandler.cancelCaptcha();
                    funnel.logSuccess();
                    Utils.hideSoftKeyboard(CreateAccountActivity.this);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("username", ((CreateAccountSuccessResult) result).getUsername());
                    resultIntent.putExtra("password", passwordEdit.getText().toString());
                    setResult(RESULT_ACCOUNT_CREATED, resultIntent);
                    Toast.makeText(CreateAccountActivity.this, R.string.create_account_account_created_toast, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    progressDialog.dismiss();
                    captchaHandler.cancelCaptcha();
                    if (result.getResult().equals("captcha-createaccount-fail")) {
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStop() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onStop();
    }
}
