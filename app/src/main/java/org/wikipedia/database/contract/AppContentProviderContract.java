package org.wikipedia.database.contract;

import android.content.ContentResolver;
import android.net.Uri;

import org.wikipedia.BuildConfig;

public final class AppContentProviderContract {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID;
    public static final Uri AUTHORITY_BASE = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .build();

    private AppContentProviderContract() { }
}