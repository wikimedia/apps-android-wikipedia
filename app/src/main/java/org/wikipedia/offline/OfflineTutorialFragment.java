package org.wikipedia.offline;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.onboarding.OnboardingFragment;

public class OfflineTutorialFragment extends OnboardingFragment {
    @NonNull
    public static OfflineTutorialFragment newInstance() {
        return new OfflineTutorialFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.onboarding_gradient_background_90));
        return view;
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
