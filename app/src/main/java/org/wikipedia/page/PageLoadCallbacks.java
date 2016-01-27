package org.wikipedia.page;

import android.support.annotation.NonNull;

/**
 * Callback methods for page load state feedback
 */
public interface PageLoadCallbacks {
    /** Called when page has finished loading */
    void onLoadComplete();
    void onLoadError(@NonNull Throwable e);
}