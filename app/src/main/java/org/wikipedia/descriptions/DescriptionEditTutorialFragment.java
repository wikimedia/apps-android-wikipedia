package org.wikipedia.descriptions;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.activity.FragmentUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DescriptionEditTutorialFragment extends Fragment {
    @BindView(R.id.fragment_description_edit_tutorial_view_pager) ViewPager viewPager;
    private Unbinder unbinder;

    private PagerAdapter adapter;

    public interface Callback {
        void onStartEditingClick();
    }

    @NonNull public static DescriptionEditTutorialFragment newInstance() {
        return new DescriptionEditTutorialFragment();
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new DescriptionEditTutorialPagerAdapter(new PageViewCallback());
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_description_edit_tutorial, container, false);
        unbinder = ButterKnife.bind(this, view);
        viewPager.setAdapter(adapter);
        return view;
    }

    @Override public void onDestroyView() {
        viewPager.setAdapter(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override public void onDestroy() {
        adapter = null;
        super.onDestroy();
    }

    private void onStartEditingClick() {
        if (callback() != null) {
            callback().onStartEditingClick();
        }
    }

    private void advancePage() {
        int nextPageIndex = viewPager.getCurrentItem() + 1;
        int lastPageIndex = viewPager.getAdapter().getCount() - 1;
        viewPager.setCurrentItem(Math.min(nextPageIndex, lastPageIndex), true);
    }

    private Callback callback() {
        return FragmentUtil.getCallback(this, DescriptionEditTutorialFragment.Callback.class);
    }

    private class PageViewCallback implements DescriptionEditTutorialPagerAdapter.Callback {
        @Override public void onButtonClick(@NonNull DescriptionEditTutorialPage page) {
            if (page.isLast()) {
                onStartEditingClick();
            } else {
                advancePage();
            }
        }
    }
}
