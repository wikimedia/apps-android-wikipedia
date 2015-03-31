package org.wikipedia.page;

import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.search.SearchBarHideHandler;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;

import android.content.Intent;
import android.os.Bundle;

/**
 * Defines interaction between PageViewFragmentInternal and an implementation that loads a page
 * for viewing.
 */
public interface PageLoadStrategy {
    void setup(PageViewModel model, PageViewFragmentInternal fragment,
               SwipeRefreshLayoutWithScroll refreshView, ObservableWebView webView,
               CommunicationBridge bridge, SearchBarHideHandler searchBarHideHandler,
               LeadImagesHandler leadImagesHandler);

    void onActivityCreated(Bundle savedInstanceState);

    void onSaveInstanceState(Bundle outState);

    void backFromEditing(Intent data);

    void onDisplayNewPage(boolean pushBackStack, boolean tryFromCache, int stagedScrollY);

    boolean isLoading();

    void setSubState(int subState);

    int getSubState();

    void onHidePageContent();

    boolean onBackPressed();
}
