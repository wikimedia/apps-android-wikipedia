package org.wikipedia.login;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.login.LoginTask;

import javax.sql.rowset.serial.SerialStruct;

public class LoginActivity extends ActionBarActivity {
    private EditText usernameText;
    private EditText passwordText;
    private CheckBox showPassword;

    private WikipediaApp app;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        app = (WikipediaApp)getApplicationContext();

        usernameText = (EditText) findViewById(R.id.login_username_text);
        passwordText = (EditText) findViewById(R.id.login_password_text);
        showPassword = (CheckBox) findViewById(R.id.login_show_password);

        TextWatcher enableActionWhenNonEmptyWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void afterTextChanged(Editable editable) {
                invalidateOptionsMenu();
            }
        };

        usernameText.addTextChangedListener(enableActionWhenNonEmptyWatcher);
        passwordText.addTextChangedListener(enableActionWhenNonEmptyWatcher);

        showPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // EditText loses the cursor position when you change the InputType
                int curPos = passwordText.getSelectionStart();
                if (isChecked) {
                    passwordText.setInputType(InputType.TYPE_CLASS_TEXT);
                } else {
                    passwordText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                passwordText.setSelection(curPos);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        menu.findItem(R.id.menu_login).setEnabled(
                usernameText.getText().length() != 0 && passwordText.getText().length() != 0
        );
        return true;
    }

    private void doLogin() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.login_in_progress_dialog_message));

        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();
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
            public void onFinish(String result) {
                progressDialog.dismiss();
                if (result.equals("Success")) {
                    Toast.makeText(LoginActivity.this, R.string.login_success_toast, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    handleError(result);
                }
            }
        }.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
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
            Crouton.makeText(this, R.string.login_error_wrong_password, Style.ALERT).show();
        } else if (result.equals("NotExists")) {
            usernameText.requestFocus();
            Crouton.makeText(this, R.string.login_error_wrong_username, Style.ALERT).show();
        } else if (result.equals("Blocked")) {
            Crouton.makeText(this, R.string.login_error_blocked, Style.ALERT).show();
        } else if (result.equals("Throttled")) {
            Crouton.makeText(this, R.string.login_error_throttled, Style.ALERT).show();
        } else {
            Crouton.makeText(this, R.string.login_error_unknown, Style.ALERT).show();
            Log.d("Wikipedia", "Login failed with result " + result);
        }
    }
}