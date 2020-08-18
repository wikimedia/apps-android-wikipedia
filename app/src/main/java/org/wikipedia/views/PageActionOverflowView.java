package org.wikipedia.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.PopupWindowCompat;

import com.google.android.material.textview.MaterialTextView;

import org.wikipedia.R;
import org.wikipedia.analytics.ABTestExploreVsHomeFunnel;
import org.wikipedia.page.tabs.Tab;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PageActionOverflowView extends FrameLayout {

    public interface Callback {
        void forwardClick();
        void findInPageClick();
        void shareClick();
        void newTabClick();
        void feedClick();
    }

    @Nullable private Callback callback;
    @Nullable private PopupWindow popupWindowHost;
    @BindView(R.id.overflow_forward) MaterialTextView forwardButton;
    @BindView(R.id.overflow_feed) MaterialTextView exploreButton;

    public PageActionOverflowView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_page_action_overflow, this);
        ButterKnife.bind(this);

        ABTestExploreVsHomeFunnel funnel = new ABTestExploreVsHomeFunnel();
        if (funnel.shouldSeeHome()) {
            exploreButton.setText(R.string.home);
            exploreButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_home_24, 0, 0, 0);
        }
    }

    public void show(@NonNull View anchorView, @Nullable Callback callback, @NonNull Tab currentTab) {
        this.callback = callback;
        popupWindowHost = new PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindowHost.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        PopupWindowCompat.setOverlapAnchor(popupWindowHost, true);
        PopupWindowCompat.showAsDropDown(popupWindowHost, anchorView, 0, 0, Gravity.END);

        final float disabledAlpha = 0.5f;
        forwardButton.setEnabled(currentTab.canGoForward());
        forwardButton.setAlpha(forwardButton.isEnabled() ? 1.0f : disabledAlpha);
    }

    @OnClick({R.id.overflow_forward, R.id.overflow_find_in_page, R.id.overflow_new_tab, R.id.overflow_share, R.id.overflow_feed})
    void onItemClick(View view) {
        if (popupWindowHost != null) {
            popupWindowHost.dismiss();
            popupWindowHost = null;
        }
        if (callback == null) {
            return;
        }
        switch (view.getId()) {
            case R.id.overflow_forward:
                callback.forwardClick();
                break;
            case R.id.overflow_find_in_page:
                callback.findInPageClick();
                break;
            case R.id.overflow_new_tab:
                callback.newTabClick();
                break;
            case R.id.overflow_share:
                callback.shareClick();
                break;
            case R.id.overflow_feed:
                callback.feedClick();
                break;
            default:
                break;
        }
    }
}
