package org.wikipedia.nearby;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.wikipedia.WikipediaApp;

public class NearbyLazyLoadFragment extends Fragment {
    private static final int CONTAINER_VIEW_ID = 0x8675309;
    private static final String CHILD_FRAGMENT_TAG = "lazyChildFragment";
    @Nullable private Fragment childFragment;

    @NonNull public static NearbyLazyLoadFragment newInstance() {
        return new NearbyLazyLoadFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);
        view.setId(CONTAINER_VIEW_ID);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        maybeLoadChildFragment();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        WikipediaApp.getInstance().getRefWatcher().watch(this);
    }

    @Override public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        maybeLoadChildFragment();
    }

    private void maybeLoadChildFragment() {
        if (isAdded() && getUserVisibleHint() && childFragment == null) {
            childFragment = NearbyFragment.newInstance();
            getChildFragmentManager().beginTransaction().add(CONTAINER_VIEW_ID, childFragment, CHILD_FRAGMENT_TAG).commit();
        }
    }
}
