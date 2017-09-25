package org.wikipedia.onboarding;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.activity.FragmentUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnPageChange;
import butterknife.Unbinder;

public abstract class OnboardingFragment extends Fragment implements BackPressedHandler {
    @BindView(R.id.fragment_pager) ViewPager viewPager;
    @BindView(R.id.fragment_onboarding_skip_button) View skipButton;
    @BindView(R.id.fragment_onboarding_forward_button) View forwardButton;
    @BindView(R.id.fragment_onboarding_done_button) TextView doneButton;
    @BindView(R.id.fragment_onboarding_pager_container) FrameLayout layout;
    private Unbinder unbinder;
    private PagerAdapter adapter;

    private GradientDrawable background;

    public interface Callback {
        void onComplete();
    }

    protected abstract PagerAdapter getAdapter();

    @StringRes protected abstract int getDoneButtonText();

    @DrawableRes protected abstract int getBackgroundResId();

    protected ViewPager getViewPager() {
        return viewPager;
    }

    protected GradientDrawable getBackground() {
        return background;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_onboarding_pager, container, false);
        unbinder = ButterKnife.bind(this, view);
        background = (GradientDrawable) ContextCompat.getDrawable(getContext(), getBackgroundResId());
        background.mutate();
        layout.setBackground(background);
        adapter = getAdapter();
        viewPager.setAdapter(adapter);
        doneButton.setText(getDoneButtonText());
        updateButtonState();
        return view;
    }

    @Override public void onDestroyView() {
        viewPager.setAdapter(null);
        adapter = null;
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override public boolean onBackPressed() {
        if (viewPager.getCurrentItem() > 0) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
            return true;
        }
        return false;
    }

    @OnClick({R.id.fragment_onboarding_forward_button, R.id.fragment_onboarding_done_button})
    public void onForwardClick() {
        if (atLastPage()) {
            finish();
        } else {
            advancePage();
        }
    }

    @OnClick(R.id.fragment_onboarding_skip_button) void onSkipClick() {
        finish();
    }

    @OnPageChange(R.id.fragment_pager) void onPageChange() {
        updateButtonState();
    }


    protected void advancePage() {
        if (!isAdded()) {
            return;
        }
        int nextPageIndex = viewPager.getCurrentItem() + 1;
        int lastPageIndex = viewPager.getAdapter().getCount() - 1;
        viewPager.setCurrentItem(Math.min(nextPageIndex, lastPageIndex), true);
    }

    protected Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private void finish() {
        if (callback() != null) {
            callback().onComplete();
        }
    }

    private boolean atLastPage() {
        return viewPager.getCurrentItem() == viewPager.getAdapter().getCount() - 1;
    }

    private void updateButtonState() {
        if (atLastPage()) {
            skipButton.setVisibility(View.GONE);
            forwardButton.setVisibility(View.GONE);
            doneButton.setVisibility(View.VISIBLE);
        } else {
            skipButton.setVisibility(View.VISIBLE);
            forwardButton.setVisibility(View.VISIBLE);
            doneButton.setVisibility(View.GONE);
        }
    }
}
