package org.wikipedia.feed;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.BuildConfig;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.FeedFunnel;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.news.NewsItemCard;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.feed.view.FeedView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.views.ExploreOverflowView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class FeedFragment extends Fragment implements BackPressedHandler {
    @BindView(R.id.feed_swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.fragment_feed_feed) FeedView feedView;
    @BindView(R.id.fragment_feed_header) View feedHeader;
    private Unbinder unbinder;
    private FeedAdapter<?> feedAdapter;
    private WikipediaApp app;
    private FeedCoordinator coordinator;
    private FeedFunnel funnel;
    private final FeedAdapter.Callback feedCallback = new FeedCallback();
    private FeedScrollListener feedScrollListener = new FeedScrollListener();
    private OverflowCallback overflowCallback = new OverflowCallback();
    private boolean searchIconVisible;

    public interface Callback {
        void onFeedTabListRequested();
        void onFeedSearchRequested();
        void onFeedVoiceSearchRequested();
        void onFeedSelectPage(HistoryEntry entry);
        void onFeedAddPageToList(HistoryEntry entry);
        void onFeedSharePage(HistoryEntry entry);
        void onFeedNewsItemSelected(NewsItemCard card);
        void onFeedShareImage(FeaturedImageCard card);
        void onFeedDownloadImage(FeaturedImage image);
        void onFeaturedImageSelected(FeaturedImageCard card);
        @NonNull View getOverflowMenuAnchor();
    }

    @NonNull public static FeedFragment newInstance() {
        FeedFragment fragment = new FeedFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        coordinator = new FeedCoordinator(getContext());
        coordinator.more(app.getSite());
        funnel = new FeedFunnel(app);
    }

    @Nullable @Override public View onCreateView(LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        unbinder = ButterKnife.bind(this, view);
        feedAdapter = new FeedAdapter<>(coordinator, feedCallback);
        feedView.setAdapter(feedAdapter);
        feedView.setCallback(feedCallback);
        feedView.addOnScrollListener(feedScrollListener);

        swipeRefreshLayout.setColorSchemeResources(R.color.foundation_blue);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                funnel.refresh(coordinator.getAge());
                coordinator.reset();
                coordinator.more(app.getSite());
            }
        });

        coordinator.setFeedUpdateListener(new FeedCoordinator.FeedUpdateListener() {
            @Override
            public void update(List<Card> cards) {
                if (isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    if (feedView != null && feedAdapter != null) {
                        feedAdapter.notifyDataSetChanged();
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        coordinator.setFeedUpdateListener(null);
        swipeRefreshLayout.setOnRefreshListener(null);
        feedView.removeOnScrollListener(feedScrollListener);
        feedView.setCallback((FeedAdapter.Callback) null);
        feedView.setAdapter(null);
        feedAdapter = null;
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_feed, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.menu_feed_search);
        MenuItem tabsItem = menu.findItem(R.id.menu_feed_tabs);
        if (searchItem != null) {
            searchItem.setVisible(searchIconVisible);
        }
        if (tabsItem != null) {
            int tabCount = Prefs.getTabCount();
            tabsItem.setIcon(ResourceUtil.getTabListIcon(getContext(), tabCount));
            tabsItem.setVisible(tabCount > 0);
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
            case R.id.menu_feed_tabs:
                if (getCallback() != null) {
                    getCallback().onFeedTabListRequested();
                }
                return true;
            case R.id.menu_overflow_button:
                Callback callback = getCallback();
                if (callback == null) {
                    return false;
                }
                showOverflowMenu(callback.getOverflowMenuAnchor());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Nullable private Callback getCallback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private class FeedCallback implements FeedAdapter.Callback {
        @Override
        public void onRequestMore() {
            funnel.requestMore(coordinator.getAge());
            coordinator.more(app.getSite());
        }

        @Override
        public void onSelectPage(@NonNull HistoryEntry entry) {
            if (getCallback() != null) {
                getCallback().onFeedSelectPage(entry);
            }
        }

        @Override
        public void onAddPageToList(@NonNull HistoryEntry entry) {
            if (getCallback() != null) {
                getCallback().onFeedAddPageToList(entry);
            }
        }

        @Override
        public void onSharePage(@NonNull HistoryEntry entry) {
            if (getCallback() != null) {
                getCallback().onFeedSharePage(entry);
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

        @Override
        public boolean onRequestDismissCard(@NonNull Card card) {
            int position = coordinator.dismissCard(card);
            funnel.dismissCard(card.type(), position);
            showDismissCardUndoSnackbar(card, position);
            return true;
        }

        @Override
        public void onSwiped(@IntRange(from = 0) int itemPos) {
            onRequestDismissCard(coordinator.getCards().get(itemPos));
        }

        @Override
        public void onNewsItemSelected(@NonNull NewsItemCard card) {
            if (getCallback() != null) {
                getCallback().onFeedNewsItemSelected(card);
            }
        }

        @Override
        public void onShareImage(@NonNull FeaturedImageCard card) {
            if (getCallback() != null) {
                getCallback().onFeedShareImage(card);
            }
        }

        @Override
        public void onDownloadImage(@NonNull FeaturedImage image) {
            if (getCallback() != null) {
                getCallback().onFeedDownloadImage(image);
            }
        }

        @Override
        public void onFeaturedImageSelected(@NonNull FeaturedImageCard card) {
            if (getCallback() != null) {
                getCallback().onFeaturedImageSelected(card);
            }
        }
    }

    private class FeedScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            int yOffset = feedView.computeVerticalScrollOffset() * 2;
            if (yOffset <= feedHeader.getHeight()
                    || feedHeader.getTranslationY() > -feedHeader.getHeight()) {
                feedHeader.setTranslationY(-yOffset);
            }
            boolean shouldShowSearchIcon = feedView.getFirstVisibleItemPosition() != 0;
            if (shouldShowSearchIcon != searchIconVisible) {
                searchIconVisible = shouldShowSearchIcon;
                getActivity().supportInvalidateOptionsMenu();
            }
        }
    }

    private void showDismissCardUndoSnackbar(final Card card, final int position) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getView(),
                getString(R.string.menu_feed_card_dismissed),
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.feed_undo_dismiss_card, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                coordinator.insertCard(card, position);
            }
        });
        snackbar.show();
    }

    private void showOverflowMenu(@NonNull View anchor) {
        ExploreOverflowView overflowView = new ExploreOverflowView(getContext());
        overflowView.show(anchor, overflowCallback);
    }

    private class OverflowCallback implements ExploreOverflowView.Callback {
        @Override
        public void loginClick() {
            startActivityForResult(LoginActivity.newIntent(getContext(), LoginFunnel.SOURCE_NAV),
                    Constants.ACTIVITY_REQUEST_LOGIN);
        }

        @Override
        public void settingsClick() {
            startActivityForResult(SettingsActivity.newIntent(getContext()),
                    SettingsActivity.ACTIVITY_REQUEST_SHOW_SETTINGS);
        }

        @Override
        public void donateClick() {
            UriUtil.visitInExternalBrowser(getContext(),
                    Uri.parse(String.format(getString(R.string.donate_url),
                            BuildConfig.VERSION_NAME,
                            WikipediaApp.getInstance().getSystemLanguageCode())));
        }

        @Override
        public void logoutClick() {
            WikipediaApp.getInstance().logOut();
            FeedbackUtil.showMessage(FeedFragment.this, R.string.toast_logout_complete);
        }
    }
}
