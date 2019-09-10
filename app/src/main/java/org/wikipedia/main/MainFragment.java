package org.wikipedia.main;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.feed.FeedFragment;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.mainpage.MainPageClient;
import org.wikipedia.feed.news.NewsActivity;
import org.wikipedia.feed.news.NewsItemCard;
import org.wikipedia.feed.view.HorizontalScrollingListCardItemView;
import org.wikipedia.gallery.GalleryActivity;
import org.wikipedia.gallery.ImagePipelineBitmapGetter;
import org.wikipedia.gallery.MediaDownloadReceiver;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.navtab.NavTab;
import org.wikipedia.navtab.NavTabFragmentPagerAdapter;
import org.wikipedia.navtab.NavTabLayout;
import org.wikipedia.nearby.NearbyFragment;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.page.tabs.TabActivity;
import org.wikipedia.random.RandomActivity;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.search.SearchActivity;
import org.wikipedia.search.SearchFragment;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.PermissionUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.log.L;

import java.io.File;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnPageChange;
import butterknife.Unbinder;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY;
import static org.wikipedia.Constants.InvokeSource.APP_SHORTCUTS;
import static org.wikipedia.Constants.InvokeSource.FEED;
import static org.wikipedia.Constants.InvokeSource.FEED_BAR;
import static org.wikipedia.Constants.InvokeSource.LINK_PREVIEW_MENU;
import static org.wikipedia.Constants.InvokeSource.VOICE;

public class MainFragment extends Fragment implements BackPressedHandler, FeedFragment.Callback,
        NearbyFragment.Callback, HistoryFragment.Callback, LinkPreviewDialog.Callback {
    @BindView(R.id.fragment_main_view_pager) ViewPager viewPager;
    @BindView(R.id.fragment_main_nav_tab_layout) NavTabLayout tabLayout;
    private Unbinder unbinder;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private MediaDownloadReceiver downloadReceiver = new MediaDownloadReceiver();
    private MediaDownloadReceiverCallback downloadReceiverCallback = new MediaDownloadReceiverCallback();

    // The permissions request API doesn't take a callback, so in the event we have to
    // ask for permission to download a featured image from the feed, we'll have to hold
    // the image we're waiting for permission to download as a bit of state here. :(
    @Nullable private FeaturedImage pendingDownloadImage;

    public interface Callback {
        void onTabChanged(@NonNull NavTab tab);
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

        viewPager.setAdapter(new NavTabFragmentPagerAdapter(getChildFragmentManager()));
        viewPager.setOffscreenPageLimit(2);
        tabLayout.setOnNavigationItemSelectedListener(item -> {
            if (getCurrentFragment() instanceof FeedFragment && item.getOrder() == 0) {
                ((FeedFragment) getCurrentFragment()).scrollToTop();
            }
            viewPager.setCurrentItem(item.getOrder());
            return true;
        });

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

    @Override public void onResume() {
        super.onResume();
        requireContext().registerReceiver(downloadReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        downloadReceiver.setCallback(downloadReceiverCallback);
        // reset the last-page-viewed timer
        Prefs.pageLastShown(0);
    }

    @Override public void onDestroyView() {
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == Constants.ACTIVITY_REQUEST_VOICE_SEARCH
                && resultCode == Activity.RESULT_OK && data != null
                && data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) != null) {
            String searchQuery = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            openSearchActivity(VOICE, searchQuery);
        } else if (requestCode == Constants.ACTIVITY_REQUEST_GALLERY
                && resultCode == GalleryActivity.ACTIVITY_RESULT_PAGE_SELECTED) {
            startActivity(data);
        } else if (requestCode == Constants.ACTIVITY_REQUEST_LOGIN
                && resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            refreshExploreFeed();
            ((MainActivity) requireActivity()).setUpHomeMenuIcon();
            FeedbackUtil.showMessage(this, R.string.login_success_toast);
        } else if (requestCode == Constants.ACTIVITY_REQUEST_BROWSE_TABS) {
            if (WikipediaApp.getInstance().getTabCount() == 0) {
                // They browsed the tabs and cleared all of them, without wanting to open a new tab.
                return;
            }
            if (resultCode == TabActivity.RESULT_NEW_TAB) {
                HistoryEntry entry = new HistoryEntry(MainPageClient.getMainPageTitle(), HistoryEntry.SOURCE_MAIN_PAGE);
                startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.getTitle()));
            } else if (resultCode == TabActivity.RESULT_LOAD_FROM_BACKSTACK) {
                startActivity(PageActivity.newIntent(requireContext()));
            }
        } else if (requestCode == Constants.ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY
                && resultCode == SearchFragment.RESULT_LANG_CHANGED) {
            refreshExploreFeed();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
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
            startActivity(RandomActivity.newIntent(requireActivity(), APP_SHORTCUTS));
        } else if (intent.hasExtra(Constants.INTENT_APP_SHORTCUT_SEARCH)) {
            openSearchActivity(APP_SHORTCUTS, null);
        } else if (intent.hasExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING)) {
            startActivity(PageActivity.newIntent(requireActivity()));
        } else if (intent.hasExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST)) {
            goToTab(NavTab.READING_LISTS);
        } else if (intent.hasExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB)
                && !((tabLayout.getSelectedItemId() == NavTab.EXPLORE.code())
                && intent.getIntExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.EXPLORE.code()) == NavTab.EXPLORE.code())) {
            goToTab(NavTab.of(intent.getIntExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.EXPLORE.code())));
        } else if (lastPageViewedWithin(1) && !intent.hasExtra(Constants.INTENT_RETURN_TO_MAIN) && WikipediaApp.getInstance().getTabCount() > 0) {
            startActivity(PageActivity.newIntent(requireContext()));
        }
    }

    @Override public void onFeedSearchRequested() {
        openSearchActivity(FEED_BAR, null);
    }

    @Override public void onFeedVoiceSearchRequested() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        try {
            startActivityForResult(intent, Constants.ACTIVITY_REQUEST_VOICE_SEARCH);
        } catch (ActivityNotFoundException a) {
            FeedbackUtil.showMessage(this, R.string.error_voice_search_not_available);
        }
    }

    @Override public void onFeedSelectPage(HistoryEntry entry) {
        startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.getTitle()), getTransitionAnimationBundle(entry.getTitle()));
    }

    @Override public void onFeedSelectPageFromExistingTab(HistoryEntry entry) {
        startActivity(PageActivity.newIntentForExistingTab(requireContext(), entry, entry.getTitle()), getTransitionAnimationBundle(entry.getTitle()));
    }

    @Override public void onFeedAddPageToList(HistoryEntry entry) {
        bottomSheetPresenter.show(getChildFragmentManager(),
                AddToReadingListDialog.newInstance(entry.getTitle(), FEED));
    }

    @Override
    public void onFeedRemovePageFromList(@NonNull HistoryEntry entry) {
        FeedbackUtil.showMessage(requireActivity(),
                getString(R.string.reading_list_item_deleted, entry.getTitle().getDisplayText()));
    }

    @Override public void onFeedSharePage(HistoryEntry entry) {
        ShareUtil.shareText(requireContext(), entry.getTitle());
    }

    @Override public void onFeedNewsItemSelected(@NonNull NewsItemCard card, @NonNull HorizontalScrollingListCardItemView view) {
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(requireActivity(), view.getImageView(), getString(R.string.transition_news_item));
        startActivity(NewsActivity.newIntent(requireActivity(), card.item(), card.wikiSite()), card.image() != null ? options.toBundle() : null);
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
        }.get();
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
        startActivityForResult(GalleryActivity.newIntent(requireActivity(), card.age(),
                card.filename(), card.baseImage(), card.wikiSite(),
                GalleryFunnel.SOURCE_FEED_FEATURED_IMAGE), Constants.ACTIVITY_REQUEST_GALLERY);
    }

    @Override
    public void onLoginRequested() {
        startActivityForResult(LoginActivity.newIntent(requireContext(), LoginFunnel.SOURCE_NAV),
                Constants.ACTIVITY_REQUEST_LOGIN);
    }

    @Nullable
    public Bundle getTransitionAnimationBundle(@NonNull PageTitle pageTitle) {
        // TODO: add future transition animations.
        return null;
    }

    @Override
    public void updateToolbarElevation(boolean elevate) {
        if (callback() != null) {
            callback().updateToolbarElevation(elevate);
        }
    }

    public void requestUpdateToolbarElevation() {
        Fragment fragment = getCurrentFragment();
        updateToolbarElevation(!(fragment instanceof FeedFragment) || ((FeedFragment) fragment).shouldElevateToolbar());
    }

    @Override public void onLoading() {
        // todo: [overhaul] update loading indicator.
    }

    @Override public void onLoaded() {
        // todo: [overhaul] update loading indicator.
    }

    @Override public void onLoadPage(@NonNull HistoryEntry entry, @Nullable Location location) {
        showLinkPreview(entry, location);
    }

    @Override public void onLoadPage(@NonNull HistoryEntry entry) {
        startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.getTitle()), getTransitionAnimationBundle(entry.getTitle()));
    }

    @Override public void onClearHistory() {
        // todo: [overhaul] clear history.
    }

    public void onLinkPreviewLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean inNewTab) {
        if (inNewTab) {
            startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.getTitle()), getTransitionAnimationBundle(entry.getTitle()));
        } else {
            startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.getTitle()), getTransitionAnimationBundle(entry.getTitle()));
        }
    }

    @Override
    public void onLinkPreviewCopyLink(@NonNull PageTitle title) {
        copyLink(title.getCanonicalUri());
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

    public void setBottomNavVisible(boolean visible) {
        tabLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
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

    @OnPageChange(R.id.fragment_main_view_pager) void onTabChanged(int position) {
        Callback callback = callback();
        if (callback != null) {
            NavTab tab = NavTab.of(position);
            callback.onTabChanged(tab);
        }
    }

    private void showLinkPreview(@NonNull HistoryEntry entry, @Nullable Location location) {
        bottomSheetPresenter.show(getChildFragmentManager(), LinkPreviewDialog.newInstance(entry, location));
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

    private void openSearchActivity(@NonNull Constants.InvokeSource source, @Nullable String query) {
        Intent intent = SearchActivity.newIntent(requireActivity(), source, query);
        startActivityForResult(intent, ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY);
    }

    private void goToTab(@NonNull NavTab tab) {
        tabLayout.setSelectedItemId(tab.code());
    }

    private void refreshExploreFeed() {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof FeedFragment) {
            ((FeedFragment) fragment).refresh();
        }
    }

    public Fragment getCurrentFragment() {
        return ((NavTabFragmentPagerAdapter) viewPager.getAdapter()).getCurrentFragment();
    }

    private class MediaDownloadReceiverCallback implements MediaDownloadReceiver.Callback {
        @Override
        public void onSuccess() {
            FeedbackUtil.showMessage(requireActivity(), R.string.gallery_save_success);
        }
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
