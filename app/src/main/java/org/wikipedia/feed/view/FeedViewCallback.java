package org.wikipedia.feed.view;

import org.wikipedia.feed.image.FeaturedImageCardView;
import org.wikipedia.feed.news.NewsListCardView;
import org.wikipedia.feed.searchbar.SearchCardView;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

public interface FeedViewCallback extends ItemTouchHelperSwipeAdapter.Callback,
        PageTitleListCardItemView.Callback, CardHeaderView.Callback, FeaturedImageCardView.Callback,
        SearchCardView.Callback, NewsListCardView.Callback {
}
