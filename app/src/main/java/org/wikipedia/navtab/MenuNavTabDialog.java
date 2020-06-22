package org.wikipedia.navtab;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.UriUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MenuNavTabDialog extends ExtendedBottomSheetDialogFragment {
    public interface Callback {
        void loginLogoutClick();
        void notificationsClick();
        void settingsClick();
        void aboutClick();
    }

    @BindView(R.id.main_drawer_account_name) TextView accountNameView;
    @BindView(R.id.main_drawer_login_button) Button loginLogoutButton;
    @BindView(R.id.main_drawer_account_avatar) ImageView accountAvatar;
    @BindView(R.id.main_drawer_account_wiki_globe) ImageView accountWikiGlobe;
    @BindView(R.id.main_drawer_notifications_container) ViewGroup notificationsContainer;
    @Nullable Callback callback;

    public static MenuNavTabDialog newInstance(Callback drawerViewCallback) {
        MenuNavTabDialog dialog = new MenuNavTabDialog();
        dialog.callback = drawerViewCallback;
        return dialog;
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.view_main_drawer, container);
        ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override public void onResume() {
        super.onResume();
        updateState();
    }

    public void updateState() {
        if (AccountUtil.isLoggedIn()) {
            accountNameView.setText(AccountUtil.getUserName());
            accountNameView.setVisibility(VISIBLE);
            loginLogoutButton.setText(requireContext().getString(R.string.preference_title_logout));
            loginLogoutButton.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.colorError));
            accountAvatar.setVisibility(VISIBLE);
            accountWikiGlobe.setVisibility(GONE);
            notificationsContainer.setVisibility(VISIBLE);
        } else {
            accountNameView.setVisibility(GONE);
            loginLogoutButton.setText(requireContext().getString(R.string.main_drawer_login));
            loginLogoutButton.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent));
            accountAvatar.setVisibility(GONE);
            accountWikiGlobe.setVisibility(VISIBLE);
            notificationsContainer.setVisibility(GONE);
        }
    }

    @OnClick(R.id.main_drawer_settings_container) void onSettingsClick() {
        if (callback != null) {
            callback.settingsClick();
            dismiss();
        }
    }

    @OnClick(R.id.main_drawer_notifications_container) void onNotificationsClick() {
        if (callback != null) {
            callback.notificationsClick();
            dismiss();
        }
    }

    @OnClick(R.id.main_drawer_donate_container) void onDonateClick() {
        UriUtil.visitInExternalBrowser(requireContext(),
                Uri.parse(requireContext().getString(R.string.donate_url,
                        BuildConfig.VERSION_NAME, WikipediaApp.getInstance().language().getSystemLanguageCode())));
        dismiss();
    }

    @OnClick(R.id.main_drawer_about_container) void onAboutClick() {
        if (callback != null) {
            callback.aboutClick();
            dismiss();
        }
    }

    @OnClick(R.id.main_drawer_help_container) void onHelpClick() {
        UriUtil.visitInExternalBrowser(requireContext(),
                Uri.parse(getContext().getString(R.string.android_app_faq_url)));
        dismiss();
    }

    @OnClick(R.id.main_drawer_login_button) void onLoginClick() {
        if (callback != null) {
            callback.loginLogoutClick();
            dismiss();
        }
    }
}
