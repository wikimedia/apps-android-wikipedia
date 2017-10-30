package org.wikipedia.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;
import org.wikipedia.zero.WikipediaZeroHandler;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import static org.wikipedia.zero.WikipediaZeroHandler.showZeroExitInterstitialDialog;

public final class UriUtil {
    public static final String LOCAL_URL_OFFLINE_LIBRARY = "#offlinelibrary";
    public static final String LOCAL_URL_SETTINGS = "#settings";
    public static final String LOCAL_URL_LOGIN = "#login";

    /**
     * Decodes a URL-encoded string into its UTF-8 equivalent. If the string cannot be decoded, the
     * original string is returned.
     * @param url The URL-encoded string that you wish to decode.
     * @return The decoded string, or the input string if the decoding failed.
     */
    @NonNull public static String decodeURL(@NonNull String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (IllegalArgumentException e) {
            // Swallow IllegalArgumentException (can happen with malformed encoding), and just
            // return the original string.
            L.d("URL decoding failed. String was: " + url);
            return url;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull public static String encodeURL(@NonNull String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
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
            chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooserIntent);
        }
    }

    @NonNull public static String resolveProtocolRelativeUrl(@NonNull WikiSite wiki,
                                                             @NonNull String url) {
        String ret = resolveProtocolRelativeUrl(url);

        // also handle images like /w/extensions/ImageMap/desc-20.png?15600 on Estados Unidos
        // or like /api/rest_v1/page/graph/png/API/0/019dd76b5f4887040716e65de53802c5033cb40c.png
        return (ret.startsWith("./") || ret.startsWith("/w/") || ret.startsWith("/wiki/"))
                || ret.startsWith("/api/")
                ? wiki.uri().buildUpon().appendEncodedPath(ret.replaceFirst("/", "")).build().toString()
                : ret;
    }

    /**
     * Resolves a potentially protocol relative URL to a 'full' URL
     *
     * @param url Url to check for (and fix) protocol relativeness
     * @return A fully qualified, protocol specified URL
     */
    @NonNull public static String resolveProtocolRelativeUrl(@NonNull String url) {
        return (url.startsWith("//") ? WikipediaApp.getInstance().getWikiSite().scheme() + ":" + url
                : url);
    }

    public static boolean isValidPageLink(@NonNull Uri uri) {
        return (!TextUtils.isEmpty(uri.getAuthority())
                && uri.getAuthority().endsWith("wikipedia.org")
                && !TextUtils.isEmpty(uri.getPath())
                && uri.getPath().startsWith("/wiki"));
    }

    /*
    Links in a ZIM file are of the form "[Title].html", instead of "/wiki/[Title]", which is what
    isValidPageLink() expects.  This necessitates a slightly different way to check for validity.
    */
    public static boolean isValidOfflinePageLink(@NonNull Uri uri) {
        return (!TextUtils.isEmpty(uri.getAuthority())
                && uri.getAuthority().endsWith("wikipedia.org")
                && !TextUtils.isEmpty(uri.getPath())
                && uri.getPath().endsWith(".html"));
    }

    public static void handleExternalLink(final Context context, final Uri uri) {
        final WikipediaZeroHandler zeroHandler = WikipediaApp.getInstance()
                .getWikipediaZeroHandler();

        if (!zeroHandler.isZeroEnabled()) {
            if (!StringUtils.isEmpty(zeroHandler.getXCarrier())) {
                // User is potentially zero-rated based on IP, but not on a whitelisted wiki (this
                // is rare)
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

    public static String getUrlWithProvenance(Context context, PageTitle title,
                                              @StringRes int provId) {
        return title.getCanonicalUri() + "?wprov=" + context.getString(provId);
    }

    /**
     * Note that while this method also replaces '_' with spaces it doesn't fully decode the string.
     */
    @NonNull
    public static String getTitleFromUrl(@NonNull String url) {
        return removeFragment(removeLinkPrefix(url)).replace("_", " ");
    }

    /** Get language variant code from a Uri, e.g. "zh-*", otherwise returns empty string. */
    @NonNull
    public static String getLanguageVariantFromUri(@NonNull Uri uri) {
        if (TextUtils.isEmpty(uri.getPath())) {
            return "";
        }
        String[] parts = StringUtils.split(StringUtils.defaultString(uri.getPath()), '/');
        return parts.length > 1 && !parts[0].equals("wiki") ? parts[0] : "";
    }

    /** For internal links only */
    @NonNull
    public static String removeInternalLinkPrefix(@NonNull String link) {
        return link.replaceFirst("/wiki/|/zh-.*/", "");
    }

    /** For links that could be internal or external links */
    @NonNull
    private static String removeLinkPrefix(@NonNull String link) {
        return link.replaceFirst("^.*?/wiki/", "");
    }

    /** Removes an optional fragment portion of a URL */
    @VisibleForTesting
    @NonNull
    static String removeFragment(@NonNull String link) {
        return link.replaceFirst("#.*$", "");
    }

    public static String getFragment(String link) {
        return Uri.parse(link).getFragment();
    }

    private UriUtil() {

    }
}
