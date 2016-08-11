package org.wikipedia.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.Log;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.Prefs;
import org.wikipedia.zero.WikipediaZeroHandler;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static org.wikipedia.zero.WikipediaZeroHandler.showZeroExitInterstitialDialog;

public final class UriUtil {

    /**
     * Decodes a URL-encoded string into its UTF-8 equivalent.
     * @param url The URL-encoded string that you wish to decode.
     * @return The decoded string, or the input string if the decoding failed.
     */
    public static String decodeURL(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Inexplicable decoding problem. This shouldn't happen. Return the input.
            Log.d("Wikipedia", "URL decoding failed. String was: " + url);
            return url;
        }
    }

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
            context.startActivity(chooserIntent);
        }
    }

    /**
     * Resolves a potentially protocol relative URL to a 'full' URL
     *
     * @param url Url to check for (and fix) protocol relativeness
     * @return A fully qualified, protocol specified URL
     */
    public static String resolveProtocolRelativeUrl(String url) {
        return (url.startsWith("//") ? WikipediaApp.getInstance().getSite().scheme() + ":" + url
                : url);
    }

    public static boolean isValidPageLink(@NonNull Uri uri) {
        return (!TextUtils.isEmpty(uri.getAuthority())
                && uri.getAuthority().endsWith("wikipedia.org")
                && !TextUtils.isEmpty(uri.getPath())
                && uri.getPath().startsWith("/wiki"));
    }

    public static void handleExternalLink(final Context context, final Uri uri) {
        final WikipediaZeroHandler zeroHandler = WikipediaApp.getInstance()
                .getWikipediaZeroHandler();

        if (!zeroHandler.isZeroEnabled()) {
            if (!StringUtil.emptyIfNull(zeroHandler.getXCarrier()).equals("")) {
                // User is potentially zero-rated based on IP, but not on a whitelisted site
                // (this is rare)
                zeroHandler.getZeroFunnel().logExtLink();
            }
            visitInExternalBrowser(context, uri);
            return;
        }

        if (!Prefs.isShowZeroInterstitialEnabled()) {
            visitInExternalBrowser(context, uri);
            zeroHandler.getZeroFunnel().logExtLinkAuto();
            return;
        }

        showZeroExitInterstitialDialog(context, uri);
    }

    public static void sendGeoIntent(@NonNull Activity activity,
                                     @NonNull Location location,
                                     String placeName) {
        String geoStr = "geo:";
        geoStr += Double.toString(location.getLatitude()) + ","
                + Double.toString(location.getLongitude());
        if (!TextUtils.isEmpty(placeName)) {
            geoStr += "?q=" + Uri.encode(placeName);
        }
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(geoStr)));
        } catch (ActivityNotFoundException e) {
            FeedbackUtil.showMessage(activity, R.string.error_no_maps_app);
        }
    }

    public static String getUrlWithProvenance(Context context, PageTitle title,
                                              @StringRes int provId) {
        return title.getCanonicalUri() + "?wprov=" + context.getString(provId);
    }

    public static String removeInternalLinkPrefix(String link) {
        return link.replaceFirst("/wiki/", "");
    }

    private UriUtil() {

    }
}
