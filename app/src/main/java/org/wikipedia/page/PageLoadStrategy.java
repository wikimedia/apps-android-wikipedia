package org.wikipedia.page;

import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.editing.EditHandler;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.search.SearchBarHideHandler;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;

import android.content.Intent;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * Defines interaction between PageFragment and an implementation that loads a page
 * for viewing.
 */
public interface PageLoadStrategy {

    /**
     * Indicates what type of cache strategy should the current request take.
     */
    enum Cache {
        /**
         * Page should be retrieved from cache if possible, only use network connection if necessary
         */
        PREFERRED,

        /**
         * Page should try to be loaded from network connection, only try cache as a fallback
         */
        FALLBACK,

        /**
         * Page should only load from network, not use cache at all
         */
        NONE
    }

    @SuppressWarnings("checkstyle:parameternumber")
    void setUp(@NonNull PageViewModel model,
               @NonNull PageFragment fragment,
               @NonNull SwipeRefreshLayoutWithScroll refreshView,
               @NonNull ObservableWebView webView,
               @NonNull CommunicationBridge bridge,
               @NonNull SearchBarHideHandler searchBarHideHandler,
               @NonNull LeadImagesHandler leadImagesHandler,
               @NonNull List<PageBackStackItem> backStack);

    void load(boolean pushBackStack, @NonNull Cache cachePreference, int stagedScrollY);

    boolean isLoading();

    void loadFromBackStack();

    /**
     * Update the current topmost backstack item, based on the currently displayed page.
     * (Things like the last y-offset position should be updated here)
     * Should be done right before loading a new page.
     */
    void updateCurrentBackStackItem();

    void setBackStack(@NonNull List<PageBackStackItem> backStack);

    boolean popBackStack();

    boolean backStackEmpty();

    /** Convenience method for hiding all the content of a page. */
    void onHidePageContent();

    void setEditHandler(EditHandler editHandler);

    void backFromEditing(Intent data);

    // TODO: remove. This has nothing to do with loading the page.
    void layoutLeadImage();
}
