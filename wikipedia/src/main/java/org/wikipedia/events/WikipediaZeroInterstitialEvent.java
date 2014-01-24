package org.wikipedia.events;

import android.net.Uri;

public class WikipediaZeroInterstitialEvent {
    private Uri uri;

    public WikipediaZeroInterstitialEvent(Uri uri) {
        this.uri = uri;
    }

    public Uri getUri() {
        return uri;
    }
}