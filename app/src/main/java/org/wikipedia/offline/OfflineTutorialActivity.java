package org.wikipedia.offline;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.WindowManager;

import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.onboarding.OnboardingFragment;

public class OfflineTutorialActivity extends SingleFragmentActivity<OfflineTutorialFragment>
        implements OnboardingFragment.Callback {
    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, OfflineTutorialActivity.class);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
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
}
