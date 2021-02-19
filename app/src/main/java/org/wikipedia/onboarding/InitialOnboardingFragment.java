package org.wikipedia.onboarding;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.languages.WikipediaLanguagesActivity;
import org.wikipedia.util.FeedbackUtil;

import static org.wikipedia.util.UriUtil.handleExternalLink;

public class InitialOnboardingFragment extends OnboardingFragment implements OnboardingPageView.Callback {
    private OnboardingPageView onboardingPageView;

    @NonNull
    public static InitialOnboardingFragment newInstance() {
        return new InitialOnboardingFragment();
    }

    @Override
    protected FragmentStateAdapter getAdapter() {
        return new OnboardingPagerAdapter(this);
    }

    @Override
    protected int getDoneButtonText() {
        return R.string.onboarding_get_started;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == Constants.ACTIVITY_REQUEST_LOGIN
                && resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            FeedbackUtil.showMessage(this, R.string.login_success_toast);
            advancePage();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSwitchChange(@NonNull OnboardingPageView view, boolean checked) {
        if (OnboardingPage.of((int) view.getTag()).equals(OnboardingPage.PAGE_USAGE_DATA)) {
            Prefs.setEventLoggingEnabled(checked);
        }
    }

    @Override
    public void onLinkClick(@NonNull OnboardingPageView view, @NonNull String url) {
        switch (url) {
            case "#login":
                startActivityForResult(LoginActivity
                                .newIntent(requireContext(), LoginFunnel.SOURCE_ONBOARDING),
                        Constants.ACTIVITY_REQUEST_LOGIN);
                break;
            case "#privacy":
                FeedbackUtil.showPrivacyPolicy(getContext());
                break;
            case "#about":
                FeedbackUtil.showAboutWikipedia(getContext());
                break;
            case "#offline":
                FeedbackUtil.showOfflineReadingAndData(getContext());
                break;
            default:
                handleExternalLink(getActivity(), Uri.parse(url));
                break;
        }
    }

    @Override
    public void onListActionButtonClicked(@NonNull OnboardingPageView view) {
        onboardingPageView = view;
        requireContext().startActivity(WikipediaLanguagesActivity.newIntent(requireContext(), Constants.InvokeSource.ONBOARDING_DIALOG));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (onboardingPageView != null) {
            onboardingPageView.refreshLanguageList();
        }
    }

    private static class OnboardingPagerAdapter extends FragmentStateAdapter {
        OnboardingPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ItemFragment.newInstance(position);
        }

        @Override
        public int getItemCount() {
            return OnboardingPage.size();
        }
    }

    public static class ItemFragment extends Fragment {
        public static ItemFragment newInstance(int position) {
            ItemFragment instance = new ItemFragment();
            Bundle args = new Bundle();
            args.putInt("position", position);
            instance.setArguments(args);
            return instance;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            int position = getArguments().getInt("position", 0);
            OnboardingPageView view = (OnboardingPageView) inflater.inflate(OnboardingPage.of(position).getLayout(), container, false);
            if (OnboardingPage.PAGE_USAGE_DATA.code() == position) {
                view.setSwitchChecked(Prefs.isEventLoggingEnabled());
            }
            view.setTag(position);
            view.setCallback(getCallback());
            return view;
        }

        @Nullable private OnboardingPageView.Callback getCallback() {
            return FragmentUtil.getCallback(this, OnboardingPageView.Callback.class);
        }
    }

    enum OnboardingPage implements EnumCode {
        PAGE_WELCOME(R.layout.inflate_initial_onboarding_page_zero),
        PAGE_EXPLORE(R.layout.inflate_initial_onboarding_page_one),
        PAGE_READING_LISTS(R.layout.inflate_initial_onboarding_page_two),
        PAGE_USAGE_DATA(R.layout.inflate_initial_onboarding_page_three);

        private static EnumCodeMap<OnboardingPage> MAP
                = new EnumCodeMap<>(OnboardingPage.class);

        @LayoutRes
        private final int layout;

        int getLayout() {
            return layout;
        }

        @NonNull
        public static OnboardingPage of(int code) {
            return MAP.get(code);
        }

        public static int size() {
            return MAP.size();
        }

        @Override
        public int code() {
            return ordinal();
        }

        OnboardingPage(@LayoutRes int layout) {
            this.layout = layout;
        }
    }
}
