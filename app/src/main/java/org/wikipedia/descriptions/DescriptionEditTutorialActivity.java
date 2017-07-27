package org.wikipedia.descriptions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.onboarding.OnboardingFragment;

public class DescriptionEditTutorialActivity
        extends SingleFragmentActivity<DescriptionEditTutorialFragment>
        implements OnboardingFragment.Callback {

    @NonNull public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, DescriptionEditTutorialActivity.class);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(R.color.green30);
    }

    @Override public void onComplete() {
        setResult(RESULT_OK);
        finish();
    }

    @Override protected DescriptionEditTutorialFragment createFragment() {
        return DescriptionEditTutorialFragment.newInstance();
    }
}
