package org.wikipedia.descriptions;

import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;

import org.wikipedia.R;
import org.wikipedia.onboarding.OnboardingFragment;

public class DescriptionEditTutorialFragment extends OnboardingFragment {

    @NonNull public static DescriptionEditTutorialFragment newInstance() {
        return new DescriptionEditTutorialFragment();
    }

    @Override
    protected PagerAdapter getAdapter() {
        return new DescriptionEditTutorialPagerAdapter();
    }

    @Override
    protected int getDoneButtonText() {
        return R.string.description_edit_tutorial_button_label_start_editing;
    }

    @Override
    protected int getBackgroundResId() {
        return R.drawable.onboarding_gradient_background_90;
    }
}
