package org.wikipedia.search;

import android.support.annotation.NonNull;

import org.wikipedia.LongPressHandler;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.AddToReadingListDialog;

public abstract class SearchResultsLongPressHandler implements LongPressHandler.ContextMenuListener {
    @NonNull private final SearchResultsFragment fragment;

    public SearchResultsLongPressHandler(@NonNull SearchResultsFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onCopyLink(@NonNull PageTitle title) {
        fragment.copyLink(title);
    }

    @Override
    public void onShareLink(@NonNull PageTitle title) {
        fragment.shareLink(title);
    }

    @Override
    public void onAddToList(@NonNull PageTitle title,
                            @NonNull AddToReadingListDialog.InvokeSource source) {
        fragment.addToReadingList(title, source);
    }
}
