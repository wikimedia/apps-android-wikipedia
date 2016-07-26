package org.wikipedia.search;

import android.support.annotation.NonNull;

// TODO: Necessary interface implementation
public abstract class SearchResultsFragmentLongPressHandler { //implements LongPressHandler.ContextMenuListener {
    @NonNull private final SearchResultsFragment fragment;

    public SearchResultsFragmentLongPressHandler(@NonNull SearchResultsFragment fragment) {
        this.fragment = fragment;
    }

}
