package org.wikipedia.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class UriUtils {
    public static final String LOCAL_URL_SETTINGS = "#settings";
    public static final String LOCAL_URL_LOGIN = "#login";
    public static final String LOCAL_URL_CUSTOMIZE_FEED = "#customizefeed";
    public static final String LOCAL_URL_LANGUAGES = "#languages";

    /**
     * Open the specified URI in an external browser (even if our app's intent filter
     * matches the given URI)
     *
     * @param context Context of the calling app
     * @param uri URI to open in an external browser
     */
    public static void visitInExternalBrowser(final Context context, Uri uri) {
        Intent chooserIntent = ShareUtil.createChooserIntent(new Intent(Intent.ACTION_VIEW, uri),
                null, context);
        if (chooserIntent == null) {
            // This means that there was no way to handle this link.
            // We will just show a toast now. FIXME: Make this more visible?
            ShareUtil.showUnresolvableIntentMessage(context);
        } else {
            chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooserIntent);
        }
    }

    public static void handleExternalLink(final Context context, final Uri uri) {
        visitInExternalBrowser(context, uri);
    }

    private UriUtils() {
    }
}
