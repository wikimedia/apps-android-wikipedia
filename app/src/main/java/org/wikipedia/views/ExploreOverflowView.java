package org.wikipedia.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.PopupWindowCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.util.ReleaseUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ExploreOverflowView extends FrameLayout {

    public interface Callback {
        void loginClick();
        void logoutClick();
        void compilationsClick();
        void settingsClick();
        void donateClick();
    }

    @BindView(R.id.explore_overflow_compilations) View compilationsView;
    @BindView(R.id.explore_overflow_account_name) TextView accountName;
    @BindView(R.id.explore_overflow_log_out) View logout;
    @Nullable private Callback callback;
    @Nullable private PopupWindow popupWindowHost;

    public ExploreOverflowView(Context context) {
        super(context);
        init();
    }

    public void show(@NonNull View anchorView, @Nullable Callback callback) {
        this.callback = callback;
        popupWindowHost = new PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindowHost.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindowHost.setElevation(getResources().getDimension(R.dimen.overflow_elevation));
        }
        PopupWindowCompat.setOverlapAnchor(popupWindowHost, true);
        PopupWindowCompat.showAsDropDown(popupWindowHost, anchorView, 0, 0, Gravity.END);
    }

    @OnClick({R.id.explore_overflow_settings, R.id.explore_overflow_donate,
            R.id.explore_overflow_account_container, R.id.explore_overflow_log_out,
            R.id.explore_overflow_compilations})
    void onItemClick(View view) {
        if (popupWindowHost != null) {
            popupWindowHost.dismiss();
            popupWindowHost = null;
        }
        if (callback == null) {
            return;
        }
        switch (view.getId()) {
            case R.id.explore_overflow_account_container:
                if (!AccountUtil.isLoggedIn()) {
                    callback.loginClick();
                }
                break;
            case R.id.explore_overflow_compilations:
                callback.compilationsClick();
                break;
            case R.id.explore_overflow_settings:
                callback.settingsClick();
                break;
            case R.id.explore_overflow_donate:
                callback.donateClick();
                break;
            case R.id.explore_overflow_log_out:
                callback.logoutClick();
                break;
            default:
                break;
        }
    }

    private void init() {
        inflate(getContext(), R.layout.view_explore_overflow, this);
        ButterKnife.bind(this);

        // TODO: remove when ready
        if (!ReleaseUtil.isPreBetaRelease()) {
            compilationsView.setVisibility(GONE);
        }

        if (AccountUtil.isLoggedIn()) {
            accountName.setText(AccountUtil.getUserName());
            logout.setVisibility(VISIBLE);
        } else {
            accountName.setText(R.string.nav_item_login);
            logout.setVisibility(GONE);
        }
    }
}
