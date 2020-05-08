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
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.log.L;

import java.io.CharArrayWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

public final class UriUtil {
    public static final String LOCAL_URL_SETTINGS = "#settings";
    public static final String LOCAL_URL_LOGIN = "#login";
    public static final String LOCAL_URL_CUSTOMIZE_FEED = "#customizefeed";
    public static final String LOCAL_URL_LANGUAGES = "#languages";

    private static BitSet UNENCODED_CHARS;

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
                && uri.getPath().startsWith("/wiki"))
                && (uri.getFragment() == null || !uri.getFragment().startsWith("cite"));
    }

    public static void handleExternalLink(final Context context, final Uri uri) {
        visitInExternalBrowser(context, uri);
    }

    public static String getUrlWithProvenance(Context context, PageTitle title,
                                              @StringRes int provId) {
        return title.getUri() + "?wprov=" + context.getString(provId);
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
        return link.replaceFirst("/wiki/|/zh.*/", "");
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

    /**
     * This reproduces the functionality of the JavaScript encodeURIComponent() function, and is
     * necessary because our RESTBase endpoints require path segments to be encoded this way to
     * ensure that the correctly-cached content is returned.
     * This is based on Java's own URLEncoder.encode() functionality, except with a few more
     * unreserved characters included, and no transforming the space character to a plus.
     * @param s Unencoded string.
     * @return Encoded string.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static String encodeURIComponent(String s) {
        if (UNENCODED_CHARS == null) {
            UNENCODED_CHARS = new BitSet(256);
            for (int i = 'a'; i <= 'z'; i++) {
                UNENCODED_CHARS.set(i);
            }
            for (int i = 'A'; i <= 'Z'; i++) {
                UNENCODED_CHARS.set(i);
            }
            for (int i = '0'; i <= '9'; i++) {
                UNENCODED_CHARS.set(i);
            }
            UNENCODED_CHARS.set('-');
            UNENCODED_CHARS.set('_');
            UNENCODED_CHARS.set('.');
            UNENCODED_CHARS.set('*');
            UNENCODED_CHARS.set('~');
            UNENCODED_CHARS.set('\'');
            UNENCODED_CHARS.set('(');
            UNENCODED_CHARS.set(')');
            UNENCODED_CHARS.set('!');
        }

        int caseDiff = ('a' - 'A');
        boolean needToChange = false;
        StringBuilder out = new StringBuilder(s.length());
        Charset charset;
        CharArrayWriter charArrayWriter = new CharArrayWriter();
        charset = StandardCharsets.UTF_8;

        for (int i = 0; i < s.length();) {
            int c = s.charAt(i);
            if (UNENCODED_CHARS.get(c)) {
                out.append((char)c);
                i++;
            } else {
                // convert to external encoding before hex conversion
                do {
                    charArrayWriter.write(c);
                    if (c >= 0xD800 && c <= 0xDBFF) {
                        if ((i + 1) < s.length()) {
                            int d = s.charAt(i + 1);
                            if (d >= 0xDC00 && d <= 0xDFFF) {
                                charArrayWriter.write(d);
                                i++;
                            }
                        }
                    }
                    i++;
                    if (i >= s.length()) {
                        break;
                    }
                    c = s.charAt(i);
                } while (!UNENCODED_CHARS.get(c));

                charArrayWriter.flush();
                String str = new String(charArrayWriter.toCharArray());
                byte[] ba = str.getBytes(charset);
                for (byte b : ba) {
                    out.append('%');
                    char ch = Character.forDigit((b >> 4) & 0xF, 16);
                    // converting to use uppercase letter as part of the hex value if ch is a letter.
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                    ch = Character.forDigit(b & 0xF, 16);
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                }
                charArrayWriter.reset();
                needToChange = true;
            }
        }
        return (needToChange ? out.toString() : s);
    }

    /**
     * This reproduces the functionality of the JavaScript decodeURIComponent() function, and is
     * necessary for decoding encoded segments received from the JavaScript bridge.
     * This is based on Java's own URLDecoder.decode() functionality, except without transforming
     * the plus sign to a space character.
     * @param s Encoded string.
     * @return Decoded string.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static String decodeURIComponent(String s) {
        boolean needToChange = false;
        int numChars = s.length();
        StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
        int i = 0;
        char c;
        byte[] bytes = null;

        while (i < numChars) {
            c = s.charAt(i);
            if (c == '%') {
                try {
                    if (bytes == null) {
                        bytes = new byte[(numChars - i) / 3];
                    }
                    int pos = 0;

                    while (((i + 2) < numChars) && (c == '%')) {
                        int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
                        if (v < 0) {
                            throw new IllegalArgumentException("URLDecoder: Illegal hex characters in escape (%) pattern");
                        }
                        bytes[pos++] = (byte) v;
                        i += 3;
                        if (i < numChars) {
                            c = s.charAt(i);
                        }
                    }
                    if ((i < numChars) && (c == '%')) {
                        throw new IllegalArgumentException("URLDecoder: Incomplete trailing escape (%) pattern");
                    }
                    sb.append(new String(bytes, 0, pos, StandardCharsets.UTF_8));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("URLDecoder: Illegal hex characters in escape (%) pattern - " + e.getMessage());
                }
                needToChange = true;
            } else {
                sb.append(c);
                i++;
            }
        }
        return (needToChange ? sb.toString() : s);
    }

    private UriUtil() { }
}
