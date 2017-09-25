package org.wikipedia.onboarding;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.FeedbackUtil;

import static org.wikipedia.util.UriUtil.handleExternalLink;

public class InitialOnboardingFragment extends OnboardingFragment {
    private PageViewCallback pageViewCallback = new PageViewCallback();

    @NonNull public static InitialOnboardingFragment newInstance() {
        return new InitialOnboardingFragment();
    }

    @Override protected PagerAdapter getAdapter() {
        return new OnboardingPagerAdapter();
    }

    @Override protected int getDoneButtonText() {
        return R.string.onboarding_get_started;
    }

    @Override protected int getBackgroundResId() {
        return R.drawable.onboarding_gradient_background_135;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == Constants.ACTIVITY_REQUEST_LOGIN
                && resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            FeedbackUtil.showMessage(this, R.string.login_success_toast);
            advancePage();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class PageViewCallback implements OnboardingPageView.Callback {
        @Override public void onSwitchChange(@NonNull OnboardingPageView view, boolean checked) {
            if (OnboardingPage.of((int) view.getTag()).equals(OnboardingPage.PAGE_USAGE_DATA)) {
                Prefs.setEventLoggingEnabled(checked);
            }
        }

        @Override public void onLinkClick(@NonNull OnboardingPageView view, @NonNull String url) {
            if (url.equals("#login")) {
                startActivityForResult(LoginActivity
                        .newIntent(getContext(), LoginFunnel.SOURCE_ONBOARDING),
                        Constants.ACTIVITY_REQUEST_LOGIN);
            } else if (url.equals("#privacy")) {
                FeedbackUtil.showPrivacyPolicy(getContext());
            } else  {
                handleExternalLink(getActivity(), Uri.parse(url));
            }
        }
    }

    private class OnboardingPagerAdapter extends PagerAdapter {
        @Override public Object instantiateItem(ViewGroup container, int position) {
            OnboardingPage page = OnboardingPage.of(position);
            OnboardingPageView view = inflate(page, container);
            view.setTag(position);
            view.setCallback(pageViewCallback);
            if (page.equals(OnboardingPage.PAGE_USAGE_DATA)) {
                view.setSwitchChecked(Prefs.isEventLoggingEnabled());
            }
            return view;
        }

        @NonNull public OnboardingPageView inflate(@NonNull OnboardingPage page,
                                                                @NonNull ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            OnboardingPageView view =
                    (OnboardingPageView) inflater.inflate(page.getLayout(), parent, false);
            parent.addView(view);
            return view;
        }

        @Override public void destroyItem(ViewGroup container, int position, Object object) {
            OnboardingPageView view = ((OnboardingPageView) object);
            view.setCallback(null);
            view.setTag(-1);
        }

        @Override public int getCount() {
            return OnboardingPage.size();
        }

        @Override public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    private enum OnboardingPage implements EnumCode {
        PAGE_EXPLORE(R.layout.inflate_initial_onboarding_page_one),
        PAGE_READING_LISTS(R.layout.inflate_initial_onboarding_page_two),
        PAGE_USAGE_DATA(R.layout.inflate_initial_onboarding_page_three);

        private static EnumCodeMap<OnboardingPage> MAP
                = new EnumCodeMap<>(OnboardingPage.class);

        @LayoutRes private final int layout;

        int getLayout() {
            return layout;
        }

        @NonNull public static OnboardingPage of(int code) {
            return MAP.get(code);
        }

        public static int size() {
            return MAP.size();
        }

        @Override public int code() {
            return ordinal();
        }

        OnboardingPage(@LayoutRes int layout) {
            this.layout = layout;
        }
    }
}
