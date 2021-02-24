package org.wikipedia.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.activity.FragmentUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public abstract class OnboardingFragment extends Fragment implements BackPressedHandler {
    @BindView(R.id.fragment_pager) ViewPager2 viewPager;
    @BindView(R.id.fragment_onboarding_skip_button) Button skipButton;
    @BindView(R.id.fragment_onboarding_forward_button) View forwardButton;
    @BindView(R.id.view_onboarding_page_indicator) TabLayout pageIndicatorView;
    @BindView(R.id.fragment_onboarding_done_button) Button doneButton;
    private Unbinder unbinder;
    private boolean enableSkip = true;
    private FragmentStateAdapter adapter;
    private PageChangeCallback pageChangeCallback = new PageChangeCallback();

    public OnboardingFragment() {
    }

    public OnboardingFragment(boolean enableSkip) {
        this.enableSkip = enableSkip;
    }

    public interface Callback {
        void onComplete();
    }

    protected abstract FragmentStateAdapter getAdapter();

    @StringRes protected abstract int getDoneButtonText();

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_onboarding_pager, container, false);
        unbinder = ButterKnife.bind(this, view);
        adapter = getAdapter();
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(pageChangeCallback);

        new TabLayoutMediator(pageIndicatorView, viewPager, (tab, position) -> { }).attach();

        doneButton.setText(getDoneButtonText());

        if (savedInstanceState == null) {
            updateButtonState();
        }

        updatePageIndicatorContentDescription();
        return view;
    }

    @Override public void onDestroyView() {
        viewPager.setAdapter(null);
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
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

    @OnClick({R.id.fragment_onboarding_forward_button, R.id.fragment_onboarding_done_button}) void onForwardClick() {
        if (atLastPage()) {
            finish();
        } else {
            advancePage();
        }
    }

    @OnClick(R.id.fragment_onboarding_skip_button) void onSkipClick() {
        finish();
    }

    void advancePage() {
        if (!isAdded()) {
            return;
        }
        int nextPageIndex = viewPager.getCurrentItem() + 1;
        int lastPageIndex = viewPager.getAdapter().getItemCount() - 1;
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
        return viewPager.getCurrentItem() == viewPager.getAdapter().getItemCount() - 1;
    }

    private void updatePageIndicatorContentDescription() {
        pageIndicatorView.setContentDescription(getString(R.string.content_description_for_page_indicator, viewPager.getCurrentItem() + 1, adapter.getItemCount()));
    }

    private void updateButtonState() {
        if (atLastPage()) {
            skipButton.setVisibility(View.GONE);
            forwardButton.setVisibility(View.GONE);
            doneButton.setVisibility(View.VISIBLE);
        } else {
            skipButton.setVisibility(enableSkip ? View.VISIBLE : View.GONE);
            forwardButton.setVisibility(View.VISIBLE);
            doneButton.setVisibility(View.GONE);
        }
    }

    private class PageChangeCallback extends ViewPager2.OnPageChangeCallback {
        @Override public void onPageSelected(int position) {
            updateButtonState();
            updatePageIndicatorContentDescription();
            // TODO: request focus to child view to make it readable after switched page.
        }
    }
}
