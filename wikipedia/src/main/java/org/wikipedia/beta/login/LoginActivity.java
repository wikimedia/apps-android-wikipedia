package org.wikipedia.beta.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import org.wikipedia.beta.NonEmptyValidator;
import org.wikipedia.beta.R;
import org.wikipedia.beta.Utils;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.analytics.LoginFunnel;
import org.wikipedia.beta.createaccount.CreateAccountActivity;

public class LoginActivity extends ActionBarActivity {
    public static final int REQUEST_LOGIN = 1;

    public static final int RESULT_LOGIN_SUCCESS = 1;
    public static final int RESULT_LOGIN_FAIL = 2;

    public static final String LOGIN_REQUEST_SOURCE = "login_request_source";
    public static final String EDIT_SESSION_TOKEN = "edit_session_token";

    private EditText usernameText;
    private EditText passwordText;
    private CheckBox showPassword;
    private View createAccountLink;

    private NonEmptyValidator nonEmptyValidator;

    private WikipediaApp app;

    private LoginFunnel funnel;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        app = (WikipediaApp)getApplicationContext();

        usernameText = (EditText) findViewById(R.id.login_username_text);
        passwordText = (EditText) findViewById(R.id.login_password_text);
        showPassword = (CheckBox) findViewById(R.id.login_show_password);
        createAccountLink = findViewById(R.id.login_create_account_link);

        nonEmptyValidator = new NonEmptyValidator(new NonEmptyValidator.ValidationChangedCallback() {
            @Override
            public void onValidationChanged(boolean isValid) {
                supportInvalidateOptionsMenu();
            }
        }, usernameText, passwordText);

        passwordText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doLogin();
                    return true;
                }
                return false;
            }
        });

        Utils.setupShowPasswordCheck(showPassword, passwordText);

        createAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                funnel.logCreateAccountAttempt();
                Intent intent = new Intent(LoginActivity.this, CreateAccountActivity.class);
                intent.putExtra(CreateAccountActivity.LOGIN_SESSION_TOKEN, funnel.getLoginSessionToken());
                startActivityForResult(intent, CreateAccountActivity.ACTION_CREATE_ACCOUNT);
            }
        });

        funnel = new LoginFunnel(app);

        // Log the start!
        if (getIntent().getStringExtra(LOGIN_REQUEST_SOURCE).equals(LoginFunnel.SOURCE_EDIT)) {
            funnel.logStart(
                    LoginFunnel.SOURCE_EDIT,
                    getIntent().getStringExtra(EDIT_SESSION_TOKEN)
            );
        } else {
            funnel.logStart(getIntent().getStringExtra(LOGIN_REQUEST_SOURCE));
        }

        // Assume no login by default
        setResult(RESULT_LOGIN_FAIL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == CreateAccountActivity.RESULT_ACCOUNT_CREATED) {
            usernameText.setText(data.getStringExtra("username"));
            passwordText.setText(data.getStringExtra("password"));
            funnel.logCreateAccountSuccess();
            doLogin();
        } else {
            funnel.logCreateAccountFailure();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        menu.findItem(R.id.menu_login).setEnabled(nonEmptyValidator.isValid());
        return true;
    }

    private void doLogin() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.login_in_progress_dialog_message));
        progressDialog.setCancelable(false);

        final String username = usernameText.getText().toString();
        final String password = passwordText.getText().toString();
        new LoginTask(this, app.getPrimarySite(), username, password) {
            @Override
            public void onBeforeExecute() {
                progressDialog.show();
            }

            @Override
            public void onCatch(Throwable caught) {
                Log.d("Wikipedia", "Caught " + caught.toString());
                progressDialog.dismiss();
                Crouton.makeText(LoginActivity.this, R.string.login_error_no_network, Style.ALERT).show();
            }

            @Override
            public void onFinish(LoginResult result) {
                super.onFinish(result);
                progressDialog.dismiss();
                if (result.getCode().equals("Success")) {
                    funnel.logSuccess();
                    Toast.makeText(LoginActivity.this, R.string.login_success_toast, Toast.LENGTH_LONG).show();

                    Utils.hideSoftKeyboard(LoginActivity.this);
                    setResult(RESULT_LOGIN_SUCCESS);

                    finish();
                } else {
                    funnel.logError(result.getCode());
                    handleError(result.getCode());
                }
            }
        }.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Utils.hideSoftKeyboard(LoginActivity.this);
                finish();
                break;
            case R.id.menu_login:
                doLogin();
                break;
            default:
                throw new RuntimeException("Some menu item case is not handled");
        }
        return true;
    }

    private void handleError(String result) {
        if (result.equals("WrongPass")) {
            passwordText.requestFocus();
            passwordText.setError(getString(R.string.login_error_wrong_password));
        } else if (result.equals("NotExists")) {
            usernameText.requestFocus();
            usernameText.setError(getString(R.string.login_error_wrong_username));
        } else if (result.equals("Blocked")) {
            Crouton.makeText(this, R.string.login_error_blocked, Style.ALERT).show();
        } else if (result.equals("Throttled")) {
            Crouton.makeText(this, R.string.login_error_throttled, Style.ALERT).show();
        } else {
            Crouton.makeText(this, R.string.login_error_unknown, Style.ALERT).show();
            Log.d("Wikipedia", "Login failed with result " + result);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Crouton.cancelAllCroutons();
    }
}