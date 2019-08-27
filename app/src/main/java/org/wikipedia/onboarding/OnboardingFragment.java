package org.wikipedia.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.rd.PageIndicatorView;

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
    @BindView(R.id.fragment_onboarding_skip_button) Button skipButton;
    @BindView(R.id.fragment_onboarding_forward_button) View forwardButton;
    @BindView(R.id.view_onboarding_page_indicator) PageIndicatorView pageIndicatorView;
    @BindView(R.id.fragment_onboarding_done_button) Button doneButton;
    private Unbinder unbinder;
    private PagerAdapter adapter;

    public interface Callback {
        void onComplete();
    }

    protected abstract PagerAdapter getAdapter();

    @StringRes protected abstract int getDoneButtonText();

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_onboarding_pager, container, false);
        unbinder = ButterKnife.bind(this, view);
        adapter = getAdapter();
        viewPager.setAdapter(adapter);
        doneButton.setText(getDoneButtonText());
        updateButtonState();
        updatePageIndicatorContentDescription();
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

    @OnPageChange(R.id.fragment_pager) void onPageChange() {
        updateButtonState();
        updatePageIndicatorContentDescription();
        // TODO: request focus to child view to make it readable after switched page.
    }


    void advancePage() {
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

    private void updatePageIndicatorContentDescription() {
        pageIndicatorView.setContentDescription(getString(R.string.content_description_for_page_indicator, viewPager.getCurrentItem() + 1, adapter.getCount()));
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
