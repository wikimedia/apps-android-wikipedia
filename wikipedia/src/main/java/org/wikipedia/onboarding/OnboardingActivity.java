package org.wikipedia.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.login.LoginActivity;

/**
 * Onboarding screen, which shows up the first time this app is started unless the user is already logged in.
 * From there you can create an account, login, or skip to the main activity.
 * It is assumed that this activity is started by the main activity,
 * so that when create account, login or skip are done we can just call finish.
 */
public class OnboardingActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        ImageView wikipediaWordMarkImage = (ImageView) findViewById(R.id.onboarding_wp_wordmark_img);
        TextView wikipediaWordMarkText = (TextView) findViewById(R.id.onboarding_wp_wordmark_text_fallback);
        View createAccountButton = findViewById(R.id.onboarding_create_account_button);
        View loginButton = findViewById(R.id.onboarding_login_button);
        View skipLink = findViewById(R.id.onboarding_skip_link);

        if ("<big>W</big>IKIPEDI<big>A</big>".equals(getString(R.string.wp_stylized))) {
            wikipediaWordMarkText.setVisibility(View.GONE);
        } else {
            wikipediaWordMarkImage.setVisibility(View.GONE);
            wikipediaWordMarkText.setText(Html.fromHtml(getString(R.string.wp_stylized)));
        }

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLoginActivity(true); // launch login but go straight to create account activity
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLoginActivity(false);
            }
        });

        skipLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                done();
            }
        });
    }

    private void startLoginActivity(boolean createAccount) {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        // TODO: add onboarding LoginFunnel source on server then replace the next line with the following one
        loginIntent.putExtra(LoginActivity.LOGIN_REQUEST_SOURCE, LoginFunnel.SOURCE_NAV);
//        loginIntent.putExtra(LoginActivity.LOGIN_REQUEST_SOURCE, LoginFunnel.SOURCE_ONBOARDING);
        loginIntent.putExtra(LoginActivity.ACTION_CREATE_ACCOUNT, createAccount);
        startActivity(loginIntent);
        done();
    }

    private void done() {
        markAllAboard();
        finish();
    }

    private void markAllAboard() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(WikipediaApp.PREFERENCE_ONBOARD, true).commit();
    }
}
