package org.wikipedia.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.ResourceUtil;

public class InitialOnboardingActivity
        extends SingleFragmentActivity<InitialOnboardingFragment>
        implements InitialOnboardingFragment.Callback {

    @NonNull public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, InitialOnboardingActivity.class);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(ResourceUtil.getThemedAttributeId(this, R.attr.main_status_bar_color));
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
        finish();
    }

    @Override protected InitialOnboardingFragment createFragment() {
        return InitialOnboardingFragment.newInstance();
    }
}
