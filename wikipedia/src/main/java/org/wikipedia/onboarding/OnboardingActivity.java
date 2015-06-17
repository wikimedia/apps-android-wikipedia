package org.wikipedia.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.ActivityUtil;
import org.wikipedia.util.L10nUtils;

import java.util.Locale;

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

        ActivityUtil.requestFullUserOrientation(this);

        setContentView(R.layout.activity_onboarding);

        final OnboardingFunnel funnel = new OnboardingFunnel((WikipediaApp) getApplicationContext());

        // Only send the onboarding start log event if the activity is created for the first time
        if (savedInstanceState == null) {
            funnel.logStart();
        }

        ImageView wikipediaWordMarkImage = (ImageView) findViewById(R.id.onboarding_wp_wordmark_img);
        TextView wikipediaWordMarkText = (TextView) findViewById(R.id.onboarding_wp_wordmark_text_fallback);
        View createAccountButton = findViewById(R.id.onboarding_create_account_button);
        View loginButton = findViewById(R.id.onboarding_login_button);
        View skipLink = findViewById(R.id.onboarding_skip_link);

        if (L10nUtils.canLangUseImageForWikipediaWordmark(this)) {
            wikipediaWordMarkText.setVisibility(View.GONE);
        } else {
            wikipediaWordMarkImage.setVisibility(View.GONE);
            wikipediaWordMarkText.setText(Html.fromHtml(getString(R.string.wp_stylized)));
            if ("iw".equals(Locale.getDefault().getLanguage())) {
                final float dp = WikipediaApp.getInstance().getScreenDensity();
                final int padAdjust = 10;
                // move wordmark a bit to the right so it lines up better with the slogan
                wikipediaWordMarkText.setPadding((int) (padAdjust * dp), 0, 0, 0);
            }
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
                startLoginActivity(false); // just take the user the login form
            }
        });

        skipLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                funnel.logSkip();
                done(); // take the user directly out of the funnel
            }
        });
    }

    /**
     * Starts LoginActivity.
     * @param createAccount true if the account creation form should be shown first, false otherwise
     */
    private void startLoginActivity(boolean createAccount) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.LOGIN_REQUEST_SOURCE, LoginFunnel.SOURCE_ONBOARDING);
        intent.putExtra(LoginActivity.ACTION_CREATE_ACCOUNT, createAccount);
        startActivity(intent);
        done();
    }

    /**
     * Prepares the activity for finishing and ensuring onboarding is not shown again.
     */
    private void done() {
        Prefs.setLoginOnboardingEnabled(false);
        finish();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Put item in bundle so start events are not fired purely because of activity recreation
        // This makes the event logging data more closely match user intent and behaviour
        outState.putBoolean("onboardingShowing", true);
    }
}
