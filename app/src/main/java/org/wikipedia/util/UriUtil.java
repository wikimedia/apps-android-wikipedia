package org.wikipedia.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;

import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.RegExp;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.log.L;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public final class UriUtil {
    public static final String LOCAL_URL_SETTINGS = "#settings";
    public static final String LOCAL_URL_LOGIN = "#login";
    public static final String LOCAL_URL_CUSTOMIZE_FEED = "#customizefeed";
    public static final String LOCAL_URL_LANGUAGES = "#languages";
    @RegExp public static final String WIKI_REGEX = "/(wiki|[a-z]{2,3}|[a-z]{2,3}-.*)/";

    /**
     * Decodes a URL-encoded string into its UTF-8 equivalent. If the string cannot be decoded, the
     * original string is returned.
     * @param url The URL-encoded string that you wish to decode.
     * @return The decoded string, or the input string if the decoding failed.
     */
    @NonNull public static String decodeURL(@NonNull String url) {
        try {
            // Force decoding of plus sign, since the built-in decode() function will replace
            // plus sign with space.
            return URLDecoder.decode(url.replace("+", "%2B"), "UTF-8");
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
            // Before returning, explicitly convert plus signs to encoded spaces, since URLEncoder
            // does that for some reason.
            return URLEncoder.encode(url, "UTF-8").replace("+", "%20");
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
    public static void visitInExternalBrowser(@NonNull final Context context, @NonNull Uri uri) {
        Intent chooserIntent = ShareUtil.createChooserIntent(new Intent(Intent.ACTION_VIEW, uri), context);
        try {
            chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooserIntent);
        } catch (ActivityNotFoundException e) {
            // This means that there was no way to handle this link.
            // We will just show a toast now. FIXME: Make this more visible?
            ShareUtil.showUnresolvableIntentMessage(context);
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
                && uri.getPath().matches("^" + WIKI_REGEX + ".*"))
                && (uri.getFragment() == null
                || (uri.getFragment().length() > 0
                && !uri.getFragment().startsWith("cite")));
    }

    public static void handleExternalLink(final Context context, final Uri uri) {
        visitInExternalBrowser(context, uri);
    }

    public static String getUrlWithProvenance(Context context, PageTitle title,
                                              @StringRes int provId) {
        return title.getUri() + "?wprov=" + context.getString(provId);
    }

    @NonNull
    public static String getFilenameFromUploadUrl(@NonNull String url) {
        String[] splitArray = url.split("/");
        String thumbnailName = splitArray[splitArray.length - 1];
        if (splitArray.length > 2) {
            String originalFilename = splitArray[splitArray.length - 2];
            if (originalFilename.matches("^[\\w,\\s-]+\\.[A-Za-z]{3}$")) {
                return originalFilename;
            }
        }
        return thumbnailName;
    }

    /**
     * Note that while this method also replaces '_' with spaces it doesn't fully decode the string.
     */
    @NonNull
    public static String getTitleFromUrl(@NonNull String url) {
        return removeFragment(removeLinkPrefix(url)).replace("_", " ");
    }

    /** Get language variant code from a Uri, e.g. "zh.*", otherwise returns empty string. */
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
        return link.replaceFirst(WIKI_REGEX, "");
    }

    /** For links that could be internal or external links */
    @NonNull
    public static String removeLinkPrefix(@NonNull String link) {
        return link.replaceFirst("^.*?" + WIKI_REGEX, "");
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

    private UriUtil() { }
}
