package org.wikipedia.util;

import android.content.ContentProviderClient;
import android.os.Build;
import android.support.annotation.NonNull;

public final class ContentProviderClientCompat {
    public static void close(@NonNull ContentProviderClient client) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            client.close();
        } else {
            //noinspection deprecation
            client.release();
        }
    }

    private ContentProviderClientCompat() { }
}
