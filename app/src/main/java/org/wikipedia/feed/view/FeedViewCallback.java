package org.wikipedia.feed.view;

import android.support.annotation.NonNull;

import org.wikipedia.feed.image.FeaturedImageCardView;
import org.wikipedia.feed.news.NewsItemCard;
import org.wikipedia.feed.searchbar.SearchCardView;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

public interface FeedViewCallback extends ItemTouchHelperSwipeAdapter.Callback,
        PageTitleListCardItemView.Callback, CardHeaderView.Callback, FeaturedImageCardView.Callback,
        SearchCardView.Callback {
    void onNewsItemSelected(@NonNull NewsItemCard card);
}
