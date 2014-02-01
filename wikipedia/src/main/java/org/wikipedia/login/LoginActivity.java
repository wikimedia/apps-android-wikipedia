package org.wikipedia.login;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import org.wikipedia.R;

public class LoginActivity extends ActionBarActivity {
    private EditText usernameText;
    private EditText passwordText;
    private CheckBox showPassword;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        usernameText = (EditText) findViewById(R.id.login_username_text);
        passwordText = (EditText) findViewById(R.id.login_password_text);
        showPassword = (CheckBox) findViewById(R.id.login_show_password);

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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_login:
                // Do something!
                break;
            default:
                throw new RuntimeException("Some menu item case is not handled");
        }
        return true;
    }
}