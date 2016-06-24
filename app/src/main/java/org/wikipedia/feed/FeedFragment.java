package org.wikipedia.feed;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.MainActivityToolbarProvider;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.CallbackFragment;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.FeedView;
import org.wikipedia.settings.Prefs;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.DimenUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class FeedFragment extends Fragment implements BackPressedHandler,
        MainActivityToolbarProvider,
        CallbackFragment<CallbackFragment.Callback> {
    @BindView(R.id.feed_app_bar_layout) AppBarLayout appBarLayout;
    @BindView(R.id.feed_swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.fragment_feed_feed) FeedView feedView;
    @BindView(R.id.feed_toolbar) Toolbar toolbar;
    private Unbinder unbinder;
    private WikipediaApp app;
    private FeedCoordinator coordinator;
    private FeedViewCallback feedCallback = new FeedCallback();
    private FeedHeaderOffsetChangedListener headerOffsetChangedListener = new FeedHeaderOffsetChangedListener();
    private int searchIconShowThresholdPx;
    private boolean searchIconVisible;

    public interface Callback extends CallbackFragment.Callback {
        void onFeedSearchRequested();
        void onFeedVoiceSearchRequested();
        void onFeedSelectPage(PageTitle title);
        void onFeedAddPageToList(PageTitle title);
        void onFeedSharePage(PageTitle title);
    }

    public static FeedFragment newInstance() {
        return new FeedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        coordinator = new FeedCoordinator(getContext());
        Prefs.pageLastShown(0);
    }

    @Nullable @Override public View onCreateView(LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        unbinder = ButterKnife.bind(this, view);
        feedView.set(coordinator, feedCallback);
        appBarLayout.addOnOffsetChangedListener(headerOffsetChangedListener);
        searchIconShowThresholdPx = (int) getResources().getDimension(R.dimen.view_feed_header_height) - DimenUtil.getContentTopOffsetPx(getContext());

        swipeRefreshLayout.setProgressViewOffset(true,
                (int) getResources().getDimension(R.dimen.view_feed_refresh_offset_start),
                (int) getResources().getDimension(R.dimen.view_feed_refresh_offset_end));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                coordinator.reset();
                coordinator.more(app.getSite());
            }
        });

        coordinator.setFeedUpdateListener(new FeedCoordinator.FeedUpdateListener() {
            @Override
            public void update(List<Card> cards) {
                if (isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    feedView.update();
                }
            }
        });

        coordinator.more(app.getSite());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        appBarLayout.removeOnOffsetChangedListener(headerOffsetChangedListener);
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        if (searchIconVisible) {
            inflater.inflate(R.menu.menu_feed, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_feed_search:
                if (getCallback() != null) {
                    getCallback().onFeedSearchRequested();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }

    @Override @Nullable public Callback getCallback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private class FeedCallback implements FeedViewCallback {
        @Override
        public void onRequestMore() {
            coordinator.more(app.getSite());
        }

        @Override
        public void onSelectPage(@NonNull PageTitle title) {
            if (getCallback() != null) {
                getCallback().onFeedSelectPage(title);
            }
        }

        @Override
        public void onAddPageToList(@NonNull PageTitle title) {
            if (getCallback() != null) {
                getCallback().onFeedAddPageToList(title);
            }
        }

        @Override
        public void onSharePage(@NonNull PageTitle title) {
            if (getCallback() != null) {
                getCallback().onFeedSharePage(title);
            }
        }

        @Override
        public void onSearchRequested() {
            if (getCallback() != null) {
                getCallback().onFeedSearchRequested();
            }
        }

        @Override
        public void onVoiceSearchRequested() {
            if (getCallback() != null) {
                getCallback().onFeedVoiceSearchRequested();
            }
        }
    }

    private class FeedHeaderOffsetChangedListener implements AppBarLayout.OnOffsetChangedListener {
        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            boolean shouldShowSearchIcon = !((searchIconShowThresholdPx + verticalOffset) > 0);
            if (shouldShowSearchIcon != searchIconVisible) {
                searchIconVisible = shouldShowSearchIcon;
                getActivity().supportInvalidateOptionsMenu();
            }
            swipeRefreshLayout.setEnabled(verticalOffset == 0);
        }
    }
}
