package org.wikipedia.overhaul;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.feed.FeedFragment;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.news.NewsItemCard;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.nearby.NearbyFragment;
import org.wikipedia.overhaul.navtab.NavTab;
import org.wikipedia.overhaul.navtab.NavTabViewPagerAdapter;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.ReadingListsFragment;
import org.wikipedia.search.OverhaulSearchFragment;
import org.wikipedia.search.SearchResultsFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class OverhaulFragment extends Fragment implements FeedFragment.Callback,
        NearbyFragment.Callback, HistoryFragment.Callback, ReadingListsFragment.Callback,
        OverhaulSearchFragment.Callback, SearchResultsFragment.Callback {
    @BindView(R.id.fragment_overhaul_view_pager) ViewPager viewPager;
    @BindView(R.id.view_nav_view_pager_tab_layout) TabLayout tabLayout;
    private Unbinder unbinder;
    private NavTabChangeListener pagerChangeListener = new NavTabChangeListener();
    private OverhaulSearchFragment searchFragment;

    public interface Callback {
        void onTabChanged(@NonNull NavTab tab, @NonNull Fragment fragment);
    }

    public static OverhaulFragment newInstance() {
        OverhaulFragment fragment = new OverhaulFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Nullable @Override public View onCreateView(LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_overhaul, container, false);
        unbinder = ButterKnife.bind(this, view);

        viewPager.addOnPageChangeListener(pagerChangeListener);
        viewPager.setAdapter(new NavTabViewPagerAdapter(getChildFragmentManager()));
        tabLayout.setupWithViewPager(viewPager);

        searchFragment = (OverhaulSearchFragment) getChildFragmentManager().findFragmentById(R.id.search_fragment);
        return view;
    }

    @Override public void onDestroyView() {
        viewPager.removeOnPageChangeListener(pagerChangeListener);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override public void onFeedSearchRequested() {
        searchFragment.setInvokeSource(OverhaulSearchFragment.InvokeSource.FEED_BAR);
        searchFragment.openSearch();
    }

    @Override public void onFeedVoiceSearchRequested() {
        // todo: [overhaul] voice search.
    }

    @Override public void onFeedSelectPage(HistoryEntry entry) {
        // todo: [overhaul] load page.
    }

    @Override public void onFeedAddPageToList(HistoryEntry entry) {
        // todo: [overhaul] add page to list.
    }

    @Override public void onFeedSharePage(HistoryEntry entry) {
        // todo: [overhaul] share page.
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
        // todo: [overhaul] show link preview.
    }

    @Override public void onLoadPage(PageTitle title, HistoryEntry entry) {
        // todo: [overhaul] show link preview.
    }

    @Override public void onClearHistory() {
        // todo: [overhaul] clear history.
    }

    @Override public boolean isMenuAllowed() {
        return true;
    }

    private void onTabChanged(@NonNull NavTab tab, @Nullable Fragment fragment) {
        Callback callback = callback();
        if (callback != null && fragment != null) {
            callback.onTabChanged(tab, fragment);
        }
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private class NavTabChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
            onTabChanged(NavTab.of(position),
                    ((NavTabViewPagerAdapter) viewPager.getAdapter()).getCurrentFragment());
        }
    }

    @Override
    public void onSearchResultCopyLink(@NonNull PageTitle title) {
        // TODO: implement
    }

    @Override
    public void onSearchResultAddToList(@NonNull PageTitle title, @NonNull AddToReadingListDialog.InvokeSource source) {
        // TODO: implement
    }

    @Override
    public void onSearchResultShareLink(@NonNull PageTitle title) {
        // TODO: implement
    }

    @Override
    public void onSearchProgressBar(boolean enabled) {
        // TODO: implement
    }

    @Override
    public void onSearchSelectPage(@NonNull HistoryEntry entry, boolean inNewTab) {
        // TODO: implement
    }

    @Override
    public void onSearchOpen() {
        // TODO: implement
    }

    @Override
    public void onSearchClose() {
        // TODO: implement
    }
}