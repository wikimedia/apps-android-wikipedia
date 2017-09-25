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
import org.wikipedia.views.BackgroundGradientOnPageChangeListener;

import java.util.ArrayList;
import java.util.List;

public class OfflineTutorialFragment extends OnboardingFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        BackgroundGradientOnPageChangeListener.MutatableGradientColors colors = getBackgroundGradientColors();
        getViewPager().addOnPageChangeListener(new BackgroundGradientOnPageChangeListener(getAdapter(), getBackground(), colors));
        return view;
    }

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

    @Override
    protected int getBackgroundResId() {
        return R.drawable.onboarding_gradient_background_offline;
    }

    private BackgroundGradientOnPageChangeListener.MutatableGradientColors getBackgroundGradientColors() {
        List<Integer> startColors = new ArrayList<>();
        List<Integer> centerColors = new ArrayList<>();
        List<Integer> endColors = new ArrayList<>();
        for (int i = 0; i < getAdapter().getCount(); i++) {
            OfflineTutorialPage page = OfflineTutorialPage.of(i);
            startColors.add(ContextCompat.getColor(getContext(), page.getGradientStart()));
            centerColors.add(ContextCompat.getColor(getContext(), page.getGradientCenter()));
            endColors.add(ContextCompat.getColor(getContext(), page.getGradentEnd()));
        }
        return new BackgroundGradientOnPageChangeListener.MutatableGradientColors(startColors, centerColors, endColors);
    }
}
