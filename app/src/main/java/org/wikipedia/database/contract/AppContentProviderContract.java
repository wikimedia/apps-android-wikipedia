package org.wikipedia.database.contract;

import android.content.ContentResolver;
import android.net.Uri;

import org.wikipedia.BuildConfig;

@SuppressWarnings("checkstyle:interfaceistype")
public interface AppContentProviderContract {
    String AUTHORITY = BuildConfig.APPLICATION_ID;
    Uri AUTHORITY_BASE = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .build();
    String NOTIFY = "notify";
}
