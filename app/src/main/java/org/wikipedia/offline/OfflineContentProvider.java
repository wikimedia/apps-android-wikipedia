package org.wikipedia.offline;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.dmitrybrant.zimdroid.ZimContentProvider;

import org.wikipedia.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class OfflineContentProvider extends ZimContentProvider {

    @NonNull public static String getBaseUrl() {
        return "content://" + BuildConfig.APPLICATION_ID + ".offline/";
    }

    @Override protected Uri getContentUri() {
        return Uri.parse(getBaseUrl());
    }

    @Override protected ByteArrayOutputStream getDataForUrl(String url) throws IOException {
        return OfflineManager.instance().getDataForUrl(url);
    }
}
