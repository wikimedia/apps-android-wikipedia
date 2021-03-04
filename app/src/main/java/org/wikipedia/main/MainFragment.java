package org.wikipedia.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.analytics.WatchlistFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.commons.FilePageActivity;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.events.LoggedOutInBackgroundEvent;
import org.wikipedia.feed.FeedFragment;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.news.NewsActivity;
import org.wikipedia.feed.news.NewsCard;
import org.wikipedia.feed.news.NewsItemView;
import org.wikipedia.gallery.GalleryActivity;
import org.wikipedia.gallery.ImagePipelineBitmapGetter;
import org.wikipedia.gallery.MediaDownloadReceiver;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.navtab.MenuNavTabDialog;
import org.wikipedia.navtab.NavTab;
import org.wikipedia.navtab.NavTabFragmentPagerAdapter;
import org.wikipedia.navtab.NavTabLayout;
import org.wikipedia.notifications.NotificationActivity;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.page.tabs.TabActivity;
import org.wikipedia.random.RandomActivity;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.MoveToReadingListDialog;
import org.wikipedia.readinglist.ReadingListBehaviorsUtil;
import org.wikipedia.readinglist.ReadingListsFragment;
import org.wikipedia.search.SearchActivity;
import org.wikipedia.search.SearchFragment;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.staticdata.UserTalkAliasData;
import org.wikipedia.suggestededits.SuggestedEditsTasksFragment;
import org.wikipedia.talk.TalkTopicsActivity;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.PermissionUtil;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.TabUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.watchlist.WatchlistActivity;

import java.io.File;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Consumer;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY;
import static org.wikipedia.Constants.InvokeSource.APP_SHORTCUTS;
import static org.wikipedia.Constants.InvokeSource.FEED;
import static org.wikipedia.Constants.InvokeSource.FEED_BAR;
import static org.wikipedia.Constants.InvokeSource.LINK_PREVIEW_MENU;
import static org.wikipedia.Constants.InvokeSource.NAV_MENU;
import static org.wikipedia.Constants.InvokeSource.VOICE;

public class MainFragment extends Fragment implements BackPressedHandler, FeedFragment.Callback,
        HistoryFragment.Callback, LinkPreviewDialog.Callback, MenuNavTabDialog.Callback {
    @BindView(R.id.fragment_main_view_pager) ViewPager2 viewPager;
    @BindView(R.id.fragment_main_nav_tab_container) LinearLayout navTabContainer;
    @BindView(R.id.fragment_main_nav_tab_layout) NavTabLayout tabLayout;
    @BindView(R.id.nav_more_container) View moreContainer;
    private Unbinder unbinder;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private MediaDownloadReceiver downloadReceiver = new MediaDownloadReceiver();
    private MediaDownloadReceiverCallback downloadReceiverCallback = new MediaDownloadReceiverCallback();
    private PageChangeCallback pageChangeCallback = new PageChangeCallback();
    private CompositeDisposable disposables = new CompositeDisposable();

    // Actually shows on the 3rd time of using the app. The Pref.incrementExploreFeedVisitCount() gets call after MainFragment.onResume()
    private static final int SHOW_EDITS_SNACKBAR_COUNT = 2;

    // The permissions request API doesn't take a callback, so in the event we have to
    // ask for permission to download a featured image from the feed, we'll have to hold
    // the image we're waiting for permission to download as a bit of state here. :(
    @Nullable private FeaturedImage pendingDownloadImage;

    public interface Callback {
        void onTabChanged(@NonNull NavTab tab);
        void updateTabCountsView();
        void updateToolbarElevation(boolean elevate);
    }

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        unbinder = ButterKnife.bind(this, view);
        disposables.add(WikipediaApp.getInstance().getBus().subscribe(new EventBusConsumer()));

        viewPager.setUserInputEnabled(false);
        viewPager.setAdapter(new NavTabFragmentPagerAdapter(this));
        viewPager.registerOnPageChangeCallback(pageChangeCallback);
        FeedbackUtil.setButtonLongPressToast(moreContainer);

        tabLayout.setOnNavigationItemSelectedListener(item -> {
            if (getCurrentFragment() instanceof FeedFragment && item.getOrder() == 0) {
                ((FeedFragment) getCurrentFragment()).scrollToTop();
            }
            if (getCurrentFragment() instanceof HistoryFragment && item.getOrder() == NavTab.SEARCH.code()) {
                openSearchActivity(NAV_MENU, null, null);
                return true;
            }
            viewPager.setCurrentItem(item.getOrder(), false);
            return true;
        });

        maybeShowEditsTooltip();

        if (savedInstanceState == null) {
            handleIntent(requireActivity().getIntent());
        }
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        downloadReceiver.setCallback(null);
        requireContext().unregisterReceiver(downloadReceiver);
    }

    @OnClick(R.id.nav_more_container) void onMoreClicked(View v) {
        bottomSheetPresenter.show(getChildFragmentManager(), MenuNavTabDialog.newInstance());
    }

    @Override public void onResume() {
        super.onResume();
        requireContext().registerReceiver(downloadReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        downloadReceiver.setCallback(downloadReceiverCallback);
        // reset the last-page-viewed timer
        Prefs.pageLastShown(0);
        maybeShowWatchlistTooltip();
    }

    @Override public void onDestroyView() {
        Prefs.setSuggestedEditsHighestPriorityEnabled(false);
        viewPager.setAdapter(null);
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        unbinder.unbind();
        unbinder = null;
        disposables.dispose();
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == Constants.ACTIVITY_REQUEST_VOICE_SEARCH
                && resultCode == Activity.RESULT_OK && data != null
                && data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) != null) {
            String searchQuery = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            openSearchActivity(VOICE, searchQuery, null);
        } else if (requestCode == Constants.ACTIVITY_REQUEST_GALLERY
                && resultCode == GalleryActivity.ACTIVITY_RESULT_PAGE_SELECTED) {
            startActivity(data);
        } else if (requestCode == Constants.ACTIVITY_REQUEST_LOGIN
                && resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            refreshContents();
            if (!Prefs.shouldShowSuggestedEditsTooltip()) {
                FeedbackUtil.showMessage(this, R.string.login_success_toast);
            }
        } else if (requestCode == Constants.ACTIVITY_REQUEST_BROWSE_TABS) {
            if (WikipediaApp.getInstance().getTabCount() == 0) {
                // They browsed the tabs and cleared all of them, without wanting to open a new tab.
                return;
            }
            if (resultCode == TabActivity.RESULT_NEW_TAB) {
                HistoryEntry entry = new HistoryEntry(new PageTitle(SiteInfoClient.getMainPageForLang(WikipediaApp.getInstance().getAppOrSystemLanguageCode()),
                        WikipediaApp.getInstance().getWikiSite()), HistoryEntry.SOURCE_MAIN_PAGE);
                startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.getTitle()));
            } else if (resultCode == TabActivity.RESULT_LOAD_FROM_BACKSTACK) {
                startActivity(PageActivity.newIntent(requireContext()));
            }
        } else if ((requestCode == Constants.ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY && resultCode == SearchFragment.RESULT_LANG_CHANGED)
                || (requestCode == Constants.ACTIVITY_REQUEST_SETTINGS
                && (resultCode == SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED || resultCode == SettingsActivity.ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED))) {
            refreshContents();
            if (resultCode == SettingsActivity.ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED) {
                updateFeedHiddenCards();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION:
                if (PermissionUtil.isPermitted(grantResults)) {
                    if (pendingDownloadImage != null) {
                        download(pendingDownloadImage);
                    }
                } else {
                    setPendingDownload(null);
                    L.i("Write permission was denied by user");
                    FeedbackUtil.showMessage(this,
                            R.string.gallery_save_image_write_permission_rationale);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void handleIntent(Intent intent) {
        if (intent.hasExtra(Constants.INTENT_APP_SHORTCUT_RANDOMIZER)) {
            startActivity(RandomActivity.newIntent(requireActivity(), WikipediaApp.getInstance().getWikiSite(), APP_SHORTCUTS));
        } else if (intent.hasExtra(Constants.INTENT_APP_SHORTCUT_SEARCH)) {
            openSearchActivity(APP_SHORTCUTS, null, null);
        } else if (intent.hasExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING)) {
            startActivity(PageActivity.newIntent(requireActivity()));
        } else if (intent.hasExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST)) {
            goToTab(NavTab.READING_LISTS);
        } else if (intent.hasExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB)
                && !((tabLayout.getSelectedItemId() == NavTab.EXPLORE.code())
                && intent.getIntExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.EXPLORE.code()) == NavTab.EXPLORE.code())) {
            goToTab(NavTab.of(intent.getIntExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.EXPLORE.code())));
        } else if (intent.hasExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB)) {
            goToTab(NavTab.of(intent.getIntExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB, NavTab.EDITS.code())));
        } else if (lastPageViewedWithin(1) && !intent.hasExtra(Constants.INTENT_RETURN_TO_MAIN) && WikipediaApp.getInstance().getTabCount() > 0) {
            startActivity(PageActivity.newIntent(requireContext()));
        }
    }

    @Override public void onFeedSearchRequested(View view) {
        openSearchActivity(FEED_BAR, null, view);
    }

    @Override public void onFeedVoiceSearchRequested() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        try {
            startActivityForResult(intent, Constants.ACTIVITY_REQUEST_VOICE_SEARCH);
        } catch (ActivityNotFoundException a) {
            FeedbackUtil.showMessage(this, R.string.error_voice_search_not_available);
        }
    }

    @Override public void onFeedSelectPage(HistoryEntry entry, boolean openInNewBackgroundTab) {
        if (openInNewBackgroundTab) {
            TabUtil.openInNewBackgroundTab(entry);
            callback().updateTabCountsView();
        } else {
            startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.getTitle()));
        }
    }

    @Override public final void onFeedSelectPageWithAnimation(HistoryEntry entry, Pair<View, String>[] sharedElements) {
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), sharedElements);
        Intent intent = PageActivity.newIntentForNewTab(requireContext(), entry, entry.getTitle());
        if (sharedElements.length > 0) {
            intent.putExtra(Constants.INTENT_EXTRA_HAS_TRANSITION_ANIM, true);
        }
        startActivity(intent, DimenUtil.isLandscape(requireContext()) || sharedElements.length == 0 ? null : options.toBundle());
    }

    @Override public void onFeedAddPageToList(HistoryEntry entry, boolean addToDefault) {
        if (addToDefault) {
            ReadingListBehaviorsUtil.INSTANCE.addToDefaultList(requireActivity(), entry.getTitle(), FEED, readingListId -> onFeedMovePageToList(readingListId, entry));
        } else {
            bottomSheetPresenter.show(getChildFragmentManager(),
                    AddToReadingListDialog.newInstance(entry.getTitle(), FEED));
        }
    }

    @Override public void onFeedMovePageToList(long sourceReadingListId, HistoryEntry entry) {
        bottomSheetPresenter.show(getChildFragmentManager(),
                MoveToReadingListDialog.newInstance(sourceReadingListId, entry.getTitle(), FEED));
    }

    @Override public void onFeedNewsItemSelected(@NonNull NewsCard newsCard, @NonNull NewsItemView view) {
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(requireActivity(), view.getImageView(), getString(R.string.transition_news_item));
        startActivity(NewsActivity.newIntent(requireActivity(), view.getNewsItem(), newsCard.wikiSite()),
                view.getNewsItem().thumb() != null ? options.toBundle() : null);
    }

    @Override
    public void onFeedSeCardFooterClicked() {
        goToTab(NavTab.EDITS);
    }

    @Override public void onFeedShareImage(final FeaturedImageCard card) {
        final String thumbUrl = card.baseImage().getThumbnailUrl();
        final String fullSizeUrl = card.baseImage().getOriginal().getSource();
        new ImagePipelineBitmapGetter(thumbUrl) {
            @Override
            public void onSuccess(@Nullable Bitmap bitmap) {
                if (bitmap != null) {
                    ShareUtil.shareImage(requireContext(),
                            bitmap,
                            new File(thumbUrl).getName(),
                            ShareUtil.getFeaturedImageShareSubject(requireContext(), card.age()),
                            fullSizeUrl);
                } else {
                    FeedbackUtil.showMessage(MainFragment.this, getString(R.string.gallery_share_error, card.baseImage().title()));
                }
            }
        }.get(requireContext());
    }

    @Override public void onFeedDownloadImage(FeaturedImage image) {
        if (!(PermissionUtil.hasWriteExternalStoragePermission(requireContext()))) {
            setPendingDownload(image);
            requestWriteExternalStoragePermission();
        } else {
            download(image);
        }
    }

    @Override public void onFeaturedImageSelected(FeaturedImageCard card) {
        startActivity(FilePageActivity.newIntent(requireActivity(), new PageTitle(card.filename(), card.wikiSite())));
    }

    @Override
    public void onLoginRequested() {
        startActivityForResult(LoginActivity.newIntent(requireContext(), LoginFunnel.SOURCE_NAV),
                Constants.ACTIVITY_REQUEST_LOGIN);
    }

    @Override
    public void updateToolbarElevation(boolean elevate) {
        if (callback() != null) {
            callback().updateToolbarElevation(elevate);
        }
    }

    public void requestUpdateToolbarElevation() {
        Fragment fragment = getCurrentFragment();
        updateToolbarElevation((fragment instanceof FeedFragment && ((FeedFragment) fragment).shouldElevateToolbar()));
    }

    @Override
    public void onLoadPage(@NonNull HistoryEntry entry) {
        startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.getTitle()));
    }

    public void onLinkPreviewLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean inNewTab) {
        if (inNewTab) {
            startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.getTitle()));
        } else {
            startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.getTitle()));
        }
    }

    @Override
    public void onLinkPreviewCopyLink(@NonNull PageTitle title) {
        copyLink(title.getUri());
    }

    @Override
    public void onLinkPreviewAddToList(@NonNull PageTitle title) {
        bottomSheetPresenter.show(getChildFragmentManager(),
                AddToReadingListDialog.newInstance(title, LINK_PREVIEW_MENU));
    }

    @Override
    public void onLinkPreviewShareLink(@NonNull PageTitle title) {
        ShareUtil.shareText(requireContext(), title);
    }

    @Override
    public boolean onBackPressed() {
        Fragment fragment = getCurrentFragment();
        return fragment instanceof BackPressedHandler && ((BackPressedHandler) fragment).onBackPressed();
    }

    @Override
    public void loginLogoutClick() {
        if (AccountUtil.isLoggedIn()) {
            new AlertDialog.Builder(requireContext())
                    .setMessage(R.string.logout_prompt)
                    .setNegativeButton(R.string.logout_dialog_cancel_button_text, null)
                    .setPositiveButton(R.string.preference_title_logout, (dialog, which) -> {
                        WikipediaApp.getInstance().logOut();
                        FeedbackUtil.showMessage(requireActivity(), R.string.toast_logout_complete);
                        Prefs.setReadingListsLastSyncTime(null);
                        Prefs.setReadingListSyncEnabled(false);
                        Prefs.setSuggestedEditsHighestPriorityEnabled(false);
                        refreshContents();
                    }).show();
        } else {
            onLoginRequested();
        }
    }

    @Override
    public void notificationsClick() {
        if (AccountUtil.isLoggedIn()) {
            startActivity(NotificationActivity.newIntent(requireActivity()));
        }
    }

    @Override
    public void talkClick() {
        if (AccountUtil.isLoggedIn() && AccountUtil.getUserName() != null) {
            startActivity(TalkTopicsActivity.newIntent(requireActivity(),
                    new PageTitle(UserTalkAliasData.valueFor(WikipediaApp.getInstance().language().getAppLanguageCode()),
                            AccountUtil.getUserName(),
                            WikiSite.forLanguageCode(WikipediaApp.getInstance().getAppOrSystemLanguageCode())),
                    NAV_MENU));
        }
    }

    @Override
    public void historyClick() {
        if (!(getCurrentFragment() instanceof HistoryFragment)) {
            goToTab(NavTab.SEARCH);
        }
    }

    @Override
    public void settingsClick() {
        startActivityForResult(SettingsActivity.newIntent(requireActivity()), Constants.ACTIVITY_REQUEST_SETTINGS);
    }

    @Override
    public void watchlistClick() {
        if (AccountUtil.isLoggedIn()) {
            new WatchlistFunnel().logViewWatchlist();
            startActivity(WatchlistActivity.Companion.newIntent(requireActivity()));
        }
    }

    public void setBottomNavVisible(boolean visible) {
        navTabContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void onGoOffline() {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof FeedFragment) {
            ((FeedFragment) fragment).onGoOffline();
        } else if (fragment instanceof HistoryFragment) {
            ((HistoryFragment) fragment).refresh();
        }
    }

    public void onGoOnline() {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof FeedFragment) {
            ((FeedFragment) fragment).onGoOnline();
        } else if (fragment instanceof HistoryFragment) {
            ((HistoryFragment) fragment).refresh();
        }
    }

    private void copyLink(@NonNull String url) {
        ClipboardUtil.setPlainText(requireContext(), null, url);
        FeedbackUtil.showMessage(this, R.string.address_copied);
    }

    private boolean lastPageViewedWithin(int days) {
        return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Prefs.pageLastShown()) < days;
    }

    private void download(@NonNull FeaturedImage image) {
        setPendingDownload(null);
        downloadReceiver.download(requireContext(), image);
        FeedbackUtil.showMessage(this, R.string.gallery_save_progress);
    }

    private void setPendingDownload(@Nullable FeaturedImage image) {
        pendingDownloadImage = image;
    }

    private void requestWriteExternalStoragePermission() {
        PermissionUtil.requestWriteStorageRuntimePermissions(this,
                Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
    }

    public void openSearchActivity(@NonNull Constants.InvokeSource source, @Nullable String query, @Nullable View transitionView) {
        Intent intent = SearchActivity.newIntent(requireActivity(), source, query);
        ActivityOptionsCompat options = null;
        if (transitionView != null) {
            options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), transitionView, getString(R.string.transition_search_bar));
        }
        startActivityForResult(intent, ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY, options != null ? options.toBundle() : null);
    }

    private void goToTab(@NonNull NavTab tab) {
        tabLayout.setSelectedItemId(tabLayout.getMenu().getItem(tab.code()).getItemId());
    }

    private void refreshContents() {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof FeedFragment) {
            ((FeedFragment) fragment).refresh();
        } else if (fragment instanceof ReadingListsFragment) {
            ((ReadingListsFragment) fragment).updateLists();
        } else if (fragment instanceof HistoryFragment) {
            ((HistoryFragment) fragment).refresh();
        } else if (fragment instanceof SuggestedEditsTasksFragment) {
            ((SuggestedEditsTasksFragment) fragment).refreshContents();
        }
    }

    private void updateFeedHiddenCards() {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof FeedFragment) {
            ((FeedFragment) fragment).updateHiddenCards();
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void maybeShowEditsTooltip() {
        if (!(getCurrentFragment() instanceof SuggestedEditsTasksFragment) && Prefs.shouldShowSuggestedEditsTooltip()
                && Prefs.getExploreFeedVisitCount() == SHOW_EDITS_SNACKBAR_COUNT) {
            tabLayout.postDelayed(() -> {
                if (!isAdded()) {
                    return;
                }
                Prefs.setShouldShowSuggestedEditsTooltip(false);
                FeedbackUtil.showTooltip(requireActivity(), tabLayout.findViewById(NavTab.EDITS.id()), AccountUtil.isLoggedIn()
                        ? getString(R.string.main_tooltip_text, AccountUtil.getUserName())
                        : getString(R.string.main_tooltip_text_v2), true, false);
            }, 500);
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void maybeShowWatchlistTooltip() {
        // TODO remove feature flag when ready
        if (ReleaseUtil.isPreBetaRelease()
                && Prefs.isWatchlistPageOnboardingTooltipShown()
                && !Prefs.isWatchlistMainOnboardingTooltipShown()
                && AccountUtil.isLoggedIn()) {
            moreContainer.postDelayed(() -> {
                if (!isAdded()) {
                    return;
                }
                new WatchlistFunnel().logShowTooltipMore();
                Prefs.setWatchlistMainOnboardingTooltipShown(true);
                FeedbackUtil.showTooltip(requireActivity(), moreContainer, R.layout.view_watchlist_main_tooltip, 0, 0, true, false);
            }, 500);
        }
    }

    @Nullable
    public Fragment getCurrentFragment() {
        return ((NavTabFragmentPagerAdapter) viewPager.getAdapter()).getFragmentAt(viewPager.getCurrentItem());
    }

    private class PageChangeCallback extends ViewPager2.OnPageChangeCallback {
        @Override
        public void onPageSelected(int position) {
            Callback callback = callback();
            if (callback != null) {
                NavTab tab = NavTab.of(position);
                callback.onTabChanged(tab);
            }
        }
    }

    private class MediaDownloadReceiverCallback implements MediaDownloadReceiver.Callback {
        @Override
        public void onSuccess() {
            FeedbackUtil.showMessage(requireActivity(), R.string.gallery_save_success);
        }
    }

    private class EventBusConsumer implements Consumer<Object> {
        @Override
        public void accept(Object event) {
            if (event instanceof LoggedOutInBackgroundEvent) {
                refreshContents();
            }
        }
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
