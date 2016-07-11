package org.wikipedia.feed;

import android.support.annotation.NonNull;

import org.wikipedia.PageTitleListCardItemCallback;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.feed.news.NewsItemCard;
import org.wikipedia.feed.model.Card;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

public interface FeedViewCallback extends ItemTouchHelperSwipeAdapter.Callback,
        PageTitleListCardItemCallback {
    void onRequestMore();
    void onSearchRequested();
    void onVoiceSearchRequested();
    boolean onRequestDismissCard(@NonNull Card card);
    void onNewsItemSelected(@NonNull NewsItemCard card);
    void onShareImage(@NonNull FeaturedImageCard card);
    void onDownloadImage(@NonNull FeaturedImage image);
}
