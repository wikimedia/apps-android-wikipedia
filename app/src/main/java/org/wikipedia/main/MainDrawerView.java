package org.wikipedia.main;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.databinding.ViewMainDrawerBinding;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.UriUtil;

public class MainDrawerView extends ScrollView {
    public interface Callback {
        void loginLogoutClick();
        void notificationsClick();
        void settingsClick();
        void configureFeedClick();
        void aboutClick();
    }

    private TextView accountNameView;
    private Button loginLogoutButton;
    private ImageView accountAvatar;
    private ImageView accountWikiGlobe;
    private ViewGroup notificationsContainer;
    @Nullable Callback callback;

    public MainDrawerView(Context context) {
        super(context);
        init();
    }

    public MainDrawerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MainDrawerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void updateState() {
        if (AccountUtil.isLoggedIn()) {
            accountNameView.setText(AccountUtil.getUserName());
            accountNameView.setVisibility(VISIBLE);
            loginLogoutButton.setText(getContext().getString(R.string.preference_title_logout));
            loginLogoutButton.setTextColor(ResourceUtil.getThemedColor(getContext(), R.attr.colorError));
            accountAvatar.setVisibility(View.VISIBLE);
            accountWikiGlobe.setVisibility(View.GONE);
            notificationsContainer.setVisibility(View.VISIBLE);
        } else {
            accountNameView.setVisibility(GONE);
            loginLogoutButton.setText(getContext().getString(R.string.main_drawer_login));
            loginLogoutButton.setTextColor(ResourceUtil.getThemedColor(getContext(), R.attr.colorAccent));
            accountAvatar.setVisibility(View.GONE);
            accountWikiGlobe.setVisibility(View.VISIBLE);
            notificationsContainer.setVisibility(View.GONE);
        }
    }

    private void init() {
        final ViewMainDrawerBinding binding = ViewMainDrawerBinding.inflate(LayoutInflater.from(getContext()));

        accountNameView = binding.mainDrawerAccountName;
        loginLogoutButton = binding.mainDrawerLoginButton;
        accountAvatar = binding.mainDrawerAccountAvatar;
        accountWikiGlobe = binding.mainDrawerAccountWikiGlobe;
        notificationsContainer = binding.mainDrawerNotificationsContainer;

        binding.mainDrawerSettingsContainer.setOnClickListener(v -> {
            if (callback != null) {
                callback.settingsClick();
            }
        });
        binding.mainDrawerConfigureContainer.setOnClickListener(v -> {
            if (callback != null) {
                callback.configureFeedClick();
            }
        });
        notificationsContainer.setOnClickListener(v -> {
            if (callback != null) {
                callback.notificationsClick();
            }
        });
        binding.mainDrawerDonateContainer.setOnClickListener(v -> UriUtil.visitInExternalBrowser(getContext(),
                Uri.parse(getContext().getString(R.string.donate_url,
                        BuildConfig.VERSION_NAME, WikipediaApp.getInstance().language().getSystemLanguageCode()))));
        binding.mainDrawerAboutContainer.setOnClickListener(v -> {
            if (callback != null) {
                callback.aboutClick();
            }
        });
        binding.mainDrawerHelpContainer.setOnClickListener(v -> UriUtil.visitInExternalBrowser(getContext(),
                Uri.parse(getContext().getString(R.string.android_app_faq_url))));
        loginLogoutButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.loginLogoutClick();
            }
        });
    }
}
