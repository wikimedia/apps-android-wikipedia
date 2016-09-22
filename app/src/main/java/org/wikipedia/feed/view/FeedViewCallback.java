package org.wikipedia.feed.view;

import android.support.annotation.NonNull;

import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.news.NewsItemCard;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

public interface FeedViewCallback extends ItemTouchHelperSwipeAdapter.Callback,
        PageTitleListCardItemView.Callback, CardHeaderView.Callback {
    void onSearchRequested();
    void onVoiceSearchRequested();
    void onNewsItemSelected(@NonNull NewsItemCard card);
    void onShareImage(@NonNull FeaturedImageCard card);
    void onDownloadImage(@NonNull FeaturedImage image);
    void onFeaturedImageSelected(@NonNull FeaturedImageCard card);
}
