package org.wikipedia.descriptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

class DescriptionEditTutorialPagerAdapter extends PagerAdapter {
    interface Callback {
        void onButtonClick(@NonNull DescriptionEditTutorialPage page);
    }

    @Nullable private Callback callback;
    @NonNull private final ViewCallback viewCallback = new ViewCallback();

    DescriptionEditTutorialPagerAdapter(@Nullable Callback callback) {
        this.callback = callback;
    }

    @Override public Object instantiateItem(ViewGroup container, int position) {
        DescriptionEditTutorialPage page = DescriptionEditTutorialPage.of(position);
        DescriptionEditTutorialPageView view = inflate(page, container);
        view.setTag(position);
        view.setCallback(viewCallback);
        return view;
    }

    @NonNull public DescriptionEditTutorialPageView inflate(@NonNull DescriptionEditTutorialPage page,
                                                            @NonNull ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        DescriptionEditTutorialPageView view =
                (DescriptionEditTutorialPageView) inflater.inflate(page.getLayout(), parent, false);
        parent.addView(view);
        return view;
    }

    @Override public void destroyItem(ViewGroup container, int position, Object object) {
        DescriptionEditTutorialPageView view = ((DescriptionEditTutorialPageView) object);
        view.setCallback(null);
        view.setTag(-1);
    }

    @Override public int getCount() {
        return DescriptionEditTutorialPage.size();
    }

    @Override public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    private class ViewCallback implements DescriptionEditTutorialPageView.Callback {
        @Override public void onButtonClick(@NonNull DescriptionEditTutorialPageView view) {
            if (callback != null) {
                callback.onButtonClick(DescriptionEditTutorialPage.of((int) view.getTag()));
            }
        }
    }
}