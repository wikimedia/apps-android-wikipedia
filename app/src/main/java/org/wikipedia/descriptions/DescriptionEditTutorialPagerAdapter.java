package org.wikipedia.descriptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.onboarding.OnboardingPageView;

class DescriptionEditTutorialPagerAdapter extends PagerAdapter {
    interface Callback {
        void onButtonClick(@NonNull DescriptionEditTutorialPage page);
        void onSkipClick(@NonNull DescriptionEditTutorialPage page);
    }

    @Nullable private Callback callback;
    @NonNull private final ViewCallback viewCallback = new ViewCallback();

    DescriptionEditTutorialPagerAdapter(@Nullable Callback callback) {
        this.callback = callback;
    }

    @Override public Object instantiateItem(ViewGroup container, int position) {
        DescriptionEditTutorialPage page = DescriptionEditTutorialPage.of(position);
        OnboardingPageView view = inflate(page, container);
        view.setTag(position);
        view.setCallback(viewCallback);
        return view;
    }

    @NonNull public OnboardingPageView inflate(@NonNull DescriptionEditTutorialPage page,
                                                            @NonNull ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        OnboardingPageView view = (OnboardingPageView) inflater.inflate(page.getLayout(), parent, false);
        parent.addView(view);
        return view;
    }

    @Override public void destroyItem(ViewGroup container, int position, Object object) {
        OnboardingPageView view = ((OnboardingPageView) object);
        view.setCallback(null);
        view.setTag(-1);
    }

    @Override public int getCount() {
        return DescriptionEditTutorialPage.size();
    }

    @Override public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    private class ViewCallback extends OnboardingPageView.DefaultCallback {
        @Override public void onButtonClick(@NonNull OnboardingPageView view) {
            if (callback != null) {
                callback.onButtonClick(DescriptionEditTutorialPage.of((int) view.getTag()));
            }
        }

        @Override
        public void onSkipClick(@NonNull OnboardingPageView view) {
            if (callback != null) {
                callback.onSkipClick(DescriptionEditTutorialPage.of((int) view.getTag()));
            }
        }
    }
}
