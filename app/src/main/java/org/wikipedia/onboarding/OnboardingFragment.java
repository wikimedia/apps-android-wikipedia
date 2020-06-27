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
import org.wikipedia.databinding.FragmentOnboardingPagerBinding;

public abstract class OnboardingFragment extends Fragment implements BackPressedHandler {
    private FragmentOnboardingPagerBinding binding;
    private ViewPager2 viewPager;
    private Button skipButton;
    private View forwardButton;
    private TabLayout pageIndicatorView;
    private Button doneButton;
    private FragmentStateAdapter adapter;
    private PageChangeCallback pageChangeCallback = new PageChangeCallback();

    public interface Callback {
        void onComplete();
    }

    protected abstract FragmentStateAdapter getAdapter();

    @StringRes protected abstract int getDoneButtonText();

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        binding = FragmentOnboardingPagerBinding.inflate(inflater, container, false);

        viewPager = binding.fragmentPager;
        skipButton = binding.fragmentOnboardingSkipButton;
        forwardButton = binding.fragmentOnboardingForwardButton;
        pageIndicatorView = binding.viewOnboardingPageIndicator;
        doneButton = binding.fragmentOnboardingDoneButton;

        final View.OnClickListener buttonClickListener = v -> {
            if (atLastPage()) {
                finish();
            } else {
                advancePage();
            }
        };
        forwardButton.setOnClickListener(buttonClickListener);
        doneButton.setOnClickListener(buttonClickListener);
        skipButton.setOnClickListener(v -> finish());

        adapter = getAdapter();
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(pageChangeCallback);

        new TabLayoutMediator(pageIndicatorView, viewPager, (tab, position) -> { }).attach();

        doneButton.setText(getDoneButtonText());
        updateButtonState();
        updatePageIndicatorContentDescription();
        return binding.getRoot();
    }

    @Override public void onDestroyView() {
        viewPager.setAdapter(null);
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        adapter = null;
        binding = null;
        super.onDestroyView();
    }

    @Override public boolean onBackPressed() {
        if (viewPager.getCurrentItem() > 0) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
            return true;
        }
        return false;
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
            skipButton.setVisibility(View.VISIBLE);
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
