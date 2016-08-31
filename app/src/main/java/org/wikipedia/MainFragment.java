package org.wikipedia;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.feed.FeedFragment;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.news.NewsItemCard;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.nearby.NearbyFragment;
import org.wikipedia.navtab.NavTab;
import org.wikipedia.navtab.NavTabViewPagerAdapter;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.ReadingListsFragment;
import org.wikipedia.search.SearchFragment;
import org.wikipedia.search.SearchResultsFragment;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnPageChange;
import butterknife.Unbinder;

public class MainFragment extends Fragment implements FeedFragment.Callback,
        NearbyFragment.Callback, HistoryFragment.Callback, ReadingListsFragment.Callback,
        SearchFragment.Callback, SearchResultsFragment.Callback,
        LinkPreviewDialog.Callback {
    @BindView(R.id.fragment_main_view_pager) ViewPager viewPager;
    @BindView(R.id.view_nav_view_pager_tab_layout) TabLayout tabLayout;
    private Unbinder unbinder;
    private SearchFragment searchFragment;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter;

    public interface Callback {
        void onTabChanged(@NonNull NavTab tab, @NonNull Fragment fragment);
    }

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Nullable @Override public View onCreateView(LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        unbinder = ButterKnife.bind(this, view);

        viewPager.setAdapter(new NavTabViewPagerAdapter(getChildFragmentManager()));
        tabLayout.setupWithViewPager(viewPager);

        bottomSheetPresenter = new ExclusiveBottomSheetPresenter(getChildFragmentManager());
        searchFragment = (SearchFragment) getChildFragmentManager().findFragmentById(R.id.search_fragment);
        return view;
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
            openSearchFromIntent(searchQuery, SearchFragment.InvokeSource.VOICE);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onFeedTabListRequested() {
        // todo: [overhaul] tab list.
    }

    @Override public void onFeedSearchRequested() {
        searchFragment.setInvokeSource(SearchFragment.InvokeSource.FEED_BAR);
        searchFragment.openSearch();
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
        startActivity(PageActivity.newIntent(getContext(), entry, entry.getTitle(), false));
    }

    @Override public void onFeedAddPageToList(HistoryEntry entry) {
        FeedbackUtil.showAddToListDialog(entry.getTitle(), AddToReadingListDialog.InvokeSource.FEED,
                bottomSheetPresenter, null);
    }

    @Override public void onFeedSharePage(HistoryEntry entry) {
        ShareUtil.shareText(getContext(), entry.getTitle());
    }

    @Override public void onFeedNewsItemSelected(NewsItemCard card) {
        // todo: [overhaul] load page.
    }

    @Override public void onFeedShareImage(FeaturedImageCard card) {
        // todo: [overhaul] share image.
    }

    @Override public void onFeedDownloadImage(FeaturedImage image) {
        // todo: [overhaul] download image.
    }

    @Override public void onFeaturedImageSelected(FeaturedImageCard card) {
        // todo: [overhaul] update loading indicator.
    }

    @Override public void onLoading() {
        // todo: [overhaul] update loading indicator.
    }

    @Override public void onLoaded() {
        // todo: [overhaul] update loading indicator.
    }

    @Override public void onLoadPage(PageTitle title, int entrySource, @Nullable Location location) {
        showLinkPreview(title, entrySource, location);
    }

    @Override public void onLoadPage(PageTitle title, HistoryEntry entry) {
        startActivity(PageActivity.newIntent(getContext(), entry, entry.getTitle(), false));
    }

    @Override public void onClearHistory() {
        // todo: [overhaul] clear history.
    }

    @Override public boolean isMenuAllowed() {
        return true;
    }

    @Override
    public void onSearchResultCopyLink(@NonNull PageTitle title) {
        copyLink(title.getCanonicalUri());
    }


    @Override
    public void onSearchResultAddToList(@NonNull PageTitle title, @NonNull AddToReadingListDialog.InvokeSource source) {
        FeedbackUtil.showAddToListDialog(title, source, bottomSheetPresenter, null);
    }

    @Override
    public void onSearchResultShareLink(@NonNull PageTitle title) {
        ShareUtil.shareText(getContext(), title);
    }

    @Override
    public void onSearchProgressBar(boolean enabled) {
        // TODO: implement
    }

    @Override
    public void onSearchSelectPage(@NonNull HistoryEntry entry, boolean inNewTab) {
        startActivity(PageActivity.newIntent(getContext(), entry, entry.getTitle(), inNewTab));
    }

    @Override
    public void onSearchOpen() {
        // TODO: implement
    }

    @Override
    public void onSearchClose() {
        // TODO: implement
    }

    @Override
    public void onLinkPreviewLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean inNewTab) {
        startActivity(PageActivity.newIntent(getContext(), entry, entry.getTitle(), inNewTab));
    }

    @Override
    public void onLinkPreviewCopyLink(@NonNull PageTitle title) {
        copyLink(title.getCanonicalUri());
    }

    @Override
    public void onLinkPreviewAddToList(@NonNull PageTitle title) {
        FeedbackUtil.showAddToListDialog(title, AddToReadingListDialog.InvokeSource.LINK_PREVIEW_MENU, bottomSheetPresenter, null);
    }

    @Override
    public void onLinkPreviewShareLink(@NonNull PageTitle title) {
        ShareUtil.shareText(getContext(), title);
    }

    @OnPageChange(R.id.fragment_main_view_pager) void onTabChanged(int position) {
        Callback callback = callback();
        Fragment fragment = ((NavTabViewPagerAdapter) viewPager.getAdapter()).getCurrentFragment();
        if (callback != null && fragment != null) {
            NavTab tab = NavTab.of(position);
            callback.onTabChanged(tab, fragment);
        }
    }

    private void showLinkPreview(PageTitle title, int entrySource, @Nullable Location location) {
        bottomSheetPresenter.show(LinkPreviewDialog.newInstance(title, entrySource, location));
    }

    private void copyLink(@NonNull String url) {
        ClipboardUtil.setPlainText(getContext(), null, url);
        FeedbackUtil.showMessage(this, R.string.address_copied);
    }

    private void openSearchFromIntent(@Nullable final CharSequence query,
                                      final SearchFragment.InvokeSource source) {
        tabLayout.post(new Runnable() {
            @Override
            public void run() {
                searchFragment.setInvokeSource(source);
                searchFragment.openSearch();
                if (query != null) {
                    searchFragment.setSearchText(query);
                }
            }
        });
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}