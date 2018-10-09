package org.wikipedia.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.settings.Prefs;

public class InitialOnboardingActivity
        extends SingleFragmentActivity<InitialOnboardingFragment>
        implements InitialOnboardingFragment.Callback {

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(R.color.base100);
    }

    @NonNull public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, InitialOnboardingActivity.class);
    }

    @Override public void onComplete() {
        setResult(RESULT_OK);
        Prefs.setInitialOnboardingEnabled(false);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (getFragment().onBackPressed()) {
            return;
        }
        setResult(RESULT_OK);
        finish();
    }

    @Override protected InitialOnboardingFragment createFragment() {
        return InitialOnboardingFragment.newInstance();
    }
}
