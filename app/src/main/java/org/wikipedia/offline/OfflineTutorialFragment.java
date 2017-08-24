package org.wikipedia.offline;

import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;

import org.wikipedia.R;
import org.wikipedia.onboarding.OnboardingFragment;

public class OfflineTutorialFragment extends OnboardingFragment {
    @NonNull
    public static OfflineTutorialFragment newInstance() {
        return new OfflineTutorialFragment();
    }

    @Override
    protected PagerAdapter getAdapter() {
        return new OfflineTutorialPagerAdapter();
    }

    @Override
    protected int getDoneButtonText() {
        return R.string.offline_library_onboarding_button_done;
    }
}
