package org.wikipedia.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.analytics.OnboardingFunnel;
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
        setTheme(WikipediaApp.getInstance().getCurrentTheme());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        final OnboardingFunnel funnel = new OnboardingFunnel((WikipediaApp) getApplicationContext());
        funnel.logStart();

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
                funnel.logCreateAccount();
                startLoginActivity(true); // launch login but go straight to create account activity
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                funnel.logLogin();
                startLoginActivity(false);
            }
        });

        skipLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                funnel.logSkip();
                done();
            }
        });
    }

    private void startLoginActivity(boolean createAccount) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.LOGIN_REQUEST_SOURCE, LoginFunnel.SOURCE_ONBOARDING);
        intent.putExtra(LoginActivity.ACTION_CREATE_ACCOUNT, createAccount);
        startActivity(intent);
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
