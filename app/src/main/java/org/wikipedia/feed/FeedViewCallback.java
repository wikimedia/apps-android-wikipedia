package org.wikipedia.feed;

import android.support.annotation.NonNull;

import org.wikipedia.page.PageTitle;

public interface FeedViewCallback {
    void onSelectPage(@NonNull PageTitle title);
    void onAddPageToList(@NonNull PageTitle title);
}
