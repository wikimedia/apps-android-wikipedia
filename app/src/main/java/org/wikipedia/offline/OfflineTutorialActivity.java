package org.wikipedia.offline;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.onboarding.OnboardingFragment;

public class OfflineTutorialActivity extends SingleFragmentActivity<OfflineTutorialFragment>
        implements OnboardingFragment.Callback {
    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, OfflineTutorialActivity.class);
    }

    @Override
    protected OfflineTutorialFragment createFragment() {
        return OfflineTutorialFragment.newInstance();
    }

    @Override
    public void onComplete() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (getFragment().onBackPressed()) {
            return;
        }
        finish();
    }
}
