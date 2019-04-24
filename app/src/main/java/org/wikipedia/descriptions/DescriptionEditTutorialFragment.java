package org.wikipedia.descriptions;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

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

}
