package org.wikipedia.feed;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
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
import org.wikipedia.feed.configure.ConfigureActivity;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.mostread.MostReadArticlesActivity;
import org.wikipedia.feed.mostread.MostReadListCard;
import org.wikipedia.feed.news.NewsItemCard;
import org.wikipedia.feed.random.RandomCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.feed.view.FeedView;
import org.wikipedia.feed.view.HorizontalScrollingListCardItemView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.offline.LocalCompilationsActivity;
import org.wikipedia.offline.OfflineTutorialActivity;
import org.wikipedia.random.RandomActivity;
import org.wikipedia.readinglist.ReadingListSyncBehaviorDialogs;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.views.ExploreOverflowView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static android.app.Activity.RESULT_OK;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_FEED_CONFIGURE;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_OFFLINE_TUTORIAL;

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
        void onFeedSelectPageFromExistingTab(HistoryEntry entry);
        void onFeedAddPageToList(HistoryEntry entry);
        void onFeedRemovePageFromList(HistoryEntry entry);
        void onFeedSharePage(HistoryEntry entry);
        void onFeedNewsItemSelected(NewsItemCard card, HorizontalScrollingListCardItemView view);
        void onFeedShareImage(FeaturedImageCard card);
        void onFeedDownloadImage(FeaturedImage image);
        void onFeaturedImageSelected(FeaturedImageCard card);
        void onLoginRequested();
        @NonNull View getOverflowMenuAnchor();
        void updateToolbarElevation(boolean elevate);
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
        coordinator = new FeedCoordinator(app);
        coordinator.more(app.getWikiSite());
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

        swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(getContext(), R.attr.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        coordinator.setFeedUpdateListener(new FeedCoordinator.FeedUpdateListener() {
            @Override public void insert(Card card, int pos) {
                if (isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    if (feedView != null && feedAdapter != null) {
                        feedAdapter.notifyItemInserted(pos);
                    }
                }
            }

            @Override public void remove(Card card, int pos) {
                if (isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    if (feedView != null && feedAdapter != null) {
                        feedAdapter.notifyItemRemoved(pos);
                    }
                }
            }
        });

        feedHeader.setBackgroundColor(ResourceUtil.getThemedColor(getContext(), R.attr.main_toolbar_color));
        if (getCallback() != null) {
            getCallback().updateToolbarElevation(shouldElevateToolbar());
        }

        ReadingListSyncAdapter.manualSync();

        return view;
    }

    public boolean shouldElevateToolbar() {
        return searchIconVisible;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        funnel.enter();

    }

    @Override
    public void onPause() {
        super.onPause();
        funnel.exit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_OFFLINE_TUTORIAL && resultCode == RESULT_OK) {
            Prefs.setOfflineTutorialCardEnabled(false);
            Prefs.setOfflineTutorialEnabled(false);
            refresh();
            feedCallback.onViewCompilations();
        } else if (requestCode == ACTIVITY_REQUEST_FEED_CONFIGURE
                && resultCode == ConfigureActivity.CONFIGURATION_CHANGED_RESULT) {
            coordinator.updateHiddenCards();
            refresh();
        }
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);
        if (!isAdded()) {
            return;
        }
        if (visible) {
            funnel.enter();
        } else {
            funnel.exit();
        }
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
    public void onDestroy() {
        super.onDestroy();
        app.getRefWatcher().watch(this);
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
            tabsItem.setIcon(ResourceUtil.getTabListIcon(tabCount));
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

    public void scrollToTop() {
        feedView.smoothScrollToPosition(0);
    }

    public void onGoOffline() {
        refresh();
    }

    public void onGoOnline() {
        refresh();
    }

    private void refresh() {
        funnel.refresh(coordinator.getAge());
        coordinator.reset();
        feedAdapter.notifyDataSetChanged();
        coordinator.more(app.getWikiSite());
    }

    @Nullable private Callback getCallback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private class FeedCallback implements FeedAdapter.Callback {
        @Override
        public void onShowCard(@Nullable Card card) {
            if (card != null) {
                funnel.cardShown(card.type());
            }
        }

        @Override
        public void onRequestMore() {
            funnel.requestMore(coordinator.getAge());
            feedView.post(() -> {
                if (isAdded()) {
                    coordinator.incrementAge();
                    coordinator.more(app.getWikiSite());
                }
            });
        }

        @Override
        public void onRetryFromOffline() {
            refresh();
        }

        @Override
        public void onError(@NonNull Throwable t) {
            FeedbackUtil.showError(getActivity(), t);
        }

        @Override
        public void onSelectPage(@NonNull Card card, @NonNull HistoryEntry entry) {
            if (getCallback() != null) {
                getCallback().onFeedSelectPage(entry);
                funnel.cardClicked(card.type());
            }
        }

        @Override
        public void onSelectPageFromExistingTab(@NonNull Card card, @NonNull HistoryEntry entry) {
            if (getCallback() != null) {
                getCallback().onFeedSelectPageFromExistingTab(entry);
                funnel.cardClicked(card.type());
            }
        }

        @Override
        public void onAddPageToList(@NonNull HistoryEntry entry) {
            if (getCallback() != null) {
                getCallback().onFeedAddPageToList(entry);
            }
        }

        @Override
        public void onRemovePageFromList(@NonNull HistoryEntry entry) {
            if (getCallback() != null) {
                getCallback().onFeedRemovePageFromList(entry);
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
        public void onRequestCustomize(@NonNull Card card) {
            showConfigureActivity(card.type().code());
        }

        @Override
        public void onSwiped(@IntRange(from = 0) int itemPos) {
            onRequestDismissCard(coordinator.getCards().get(itemPos));
        }

        @Override
        public void onNewsItemSelected(@NonNull NewsItemCard card, @NonNull HorizontalScrollingListCardItemView view) {
            if (getCallback() != null) {
                funnel.cardClicked(card.type());
                getCallback().onFeedNewsItemSelected(card, view);
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
                funnel.cardClicked(card.type());
                getCallback().onFeaturedImageSelected(card);
            }
        }

        @Override
        public void onAnnouncementPositiveAction(@NonNull Card card, @NonNull Uri uri) {
            funnel.cardClicked(card.type());
            if (uri.toString().equals(UriUtil.LOCAL_URL_OFFLINE_LIBRARY)) {
                onViewCompilations();
            } else if (uri.toString().equals(UriUtil.LOCAL_URL_LOGIN)) {
                if (getCallback() != null) {
                    getCallback().onLoginRequested();
                }
            } else if (uri.toString().equals(UriUtil.LOCAL_URL_SETTINGS)) {
                startActivityForResult(SettingsActivity.newIntent(getContext()),
                        SettingsActivity.ACTIVITY_REQUEST_SHOW_SETTINGS);
            } else if (uri.toString().equals(UriUtil.LOCAL_URL_CUSTOMIZE_FEED)) {
                showConfigureActivity(card.type().code());
            } else {
                UriUtil.handleExternalLink(getContext(), uri);
            }
        }

        @Override
        public void onAnnouncementNegativeAction(@NonNull Card card) {
            onRequestDismissCard(card);
        }

        @Override
        public void onRandomClick(@NonNull RandomCardView view) {
            if (!DeviceUtil.isOnline()) {
                view.getRandomPage();
            } else {
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(getActivity(), view, getString(R.string.transition_random_activity));
                startActivity(RandomActivity.newIntent(getActivity(), RandomActivity.INVOKE_SOURCE_FEED), options.toBundle());
            }
        }

        @Override
        public void onGetRandomError(@NonNull Throwable t, @NonNull final RandomCardView view) {
            Snackbar snackbar = FeedbackUtil.makeSnackbar(getActivity(), ThrowableUtil.isOffline(t)
                    ? getString(R.string.view_wiki_error_message_offline) : t.getMessage(),
                    FeedbackUtil.LENGTH_DEFAULT);
            snackbar.setAction(R.string.page_error_retry, (v) -> view.getRandomPage());
            snackbar.show();
        }

        public void onViewCompilations() {
            if (Prefs.isOfflineTutorialEnabled()) {
                startActivityForResult(OfflineTutorialActivity.newIntent(getContext()),
                        ACTIVITY_REQUEST_OFFLINE_TUTORIAL);
            } else {
                startActivity(LocalCompilationsActivity.newIntent(getContext()));
            }
        }

        @Override
        public void onMoreContentSelected(@NonNull Card card) {
            startActivity(MostReadArticlesActivity.newIntent(getContext(), (MostReadListCard) card));
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
                getActivity().invalidateOptionsMenu();
                if (getCallback() != null) {
                    getCallback().updateToolbarElevation(shouldElevateToolbar());
                }
            }
        }
    }

    private void showDismissCardUndoSnackbar(final Card card, final int position) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getActivity(),
                getString(R.string.menu_feed_card_dismissed),
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.feed_undo_dismiss_card, (v) -> coordinator.undoDismissCard(card, position));
        snackbar.show();
    }

    private void showConfigureActivity(int invokeSource) {
        startActivityForResult(ConfigureActivity.newIntent(getActivity(), invokeSource),
                Constants.ACTIVITY_REQUEST_FEED_CONFIGURE);
    }

    private void showOverflowMenu(@NonNull View anchor) {
        ExploreOverflowView overflowView = new ExploreOverflowView(getContext());
        overflowView.show(anchor, overflowCallback);
    }

    private class OverflowCallback implements ExploreOverflowView.Callback {
        @Override
        public void loginClick() {
            if (getCallback() != null) {
                getCallback().onLoginRequested();
            }
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
        public void configureCardsClick() {
            showConfigureActivity(-1);
        }

        @Override
        public void logoutClick() {
            WikipediaApp.getInstance().logOut();
            FeedbackUtil.showMessage(FeedFragment.this, R.string.toast_logout_complete);

            if (Prefs.isReadingListSyncEnabled() && !ReadingListDbHelper.instance().isEmpty()) {
                ReadingListSyncBehaviorDialogs.removeExistingListsOnLogoutDialog(getActivity());
            }
            Prefs.setReadingListsLastSyncTime(null);
            Prefs.setReadingListSyncEnabled(false);
        }

        @Override
        public void compilationsClick() {
            feedCallback.onViewCompilations();
        }
    }
}
