package org.wikipedia.offline;

import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.onboarding.OnboardingPageView;

class OfflineTutorialPagerAdapter extends PagerAdapter {
    @NonNull private final OnboardingPageView.DefaultCallback viewCallback
            = new OnboardingPageView.DefaultCallback();

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        OfflineTutorialPage page = OfflineTutorialPage.of(position);
        OnboardingPageView view = inflate(page, container);
        view.setTag(position);
        view.setCallback(viewCallback);
        return view;
    }

    @NonNull
    public OnboardingPageView inflate(@NonNull OfflineTutorialPage page,
                                               @NonNull ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        OnboardingPageView view = (OnboardingPageView) inflater.inflate(page.getLayout(), parent, false);
        parent.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        OnboardingPageView view = ((OnboardingPageView) object);
        view.setCallback(null);
        view.setTag(-1);
    }

    @Override
    public int getCount() {
        return OfflineTutorialPage.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view == o;
    }
}
