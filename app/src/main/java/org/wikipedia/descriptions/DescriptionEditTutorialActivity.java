package org.wikipedia.descriptions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.onboarding.OnboardingFragment;
import org.wikipedia.util.ResourceUtil;

public class DescriptionEditTutorialActivity
        extends SingleFragmentActivity<DescriptionEditTutorialFragment>
        implements OnboardingFragment.Callback {

    public static final String DESCRIPTION_SELECTED_TEXT = "selectedText";

    @NonNull public static Intent newIntent(@NonNull Context context, @Nullable String selectedText) {
        return new Intent(context, DescriptionEditTutorialActivity.class).putExtra(DESCRIPTION_SELECTED_TEXT, selectedText);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(ResourceUtil.getThemedAttributeId(this, R.attr.paper_color));
    }

    @Override public void onComplete() {
        setResult(RESULT_OK, getIntent());
        finish();
    }

    @Override protected DescriptionEditTutorialFragment createFragment() {
        return DescriptionEditTutorialFragment.newInstance();
    }
}
