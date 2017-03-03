package org.wikipedia.util;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public final class StringUtil {
    private static final String CSV_DELIMITER = ",";

    @NonNull
    public static String listToCsv(@NonNull List<String> list) {
        return TextUtils.join(CSV_DELIMITER, list);
    }

    /** @return Nonnull immutable list. */
    @NonNull
    public static List<String> csvToList(@NonNull String csv) {
        return delimiterStringToList(csv, CSV_DELIMITER);
    }

    /** @return Nonnull immutable list. */
    @NonNull
    public static List<String> delimiterStringToList(@NonNull String delimitedString,
                                                     @NonNull String delimiter) {
        return Arrays.asList(TextUtils.split(delimitedString, delimiter));
    }

    /**
     * Creates an MD5 hash of the provided string and returns its ASCII representation
     * @param s String to hash
     * @return ASCII MD5 representation of the string passed in
     */
    @NonNull public static String md5string(@NonNull String s) {
        StringBuilder hexStr = new StringBuilder();
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes("utf-8"));
            byte[] messageDigest = digest.digest();

            final int maxByteVal = 0xFF;
            for (byte b : messageDigest) {
                hexStr.append(Integer.toHexString(maxByteVal & b));
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return hexStr.toString();
    }

    /**
     * Remove leading and trailing whitespace from a CharSequence. This is useful after using
     * the fromHtml() function to convert HTML to a CharSequence.
     * @param str CharSequence to be trimmed.
     * @return The trimmed CharSequence.
     */
    @NonNull public static CharSequence strip(@Nullable CharSequence str) {
        if (str == null || str.length() == 0) {
            return "";
        }
        int len = str.length();
        int start = 0;
        int end = len - 1;
        while (Character.isWhitespace(str.charAt(start)) && start < len) {
            start++;
        }
        while (Character.isWhitespace(str.charAt(end)) && end > 0) {
            end--;
        }
        if (end > start) {
            return str.subSequence(start, end + 1);
        }
        return "";
    }

    @NonNull
    public static String intToHexStr(int i) {
        return String.format("x%08x", i);
    }

    public static String addUnderscores(@NonNull String text) {
        return text.replace(" ", "_");
    }

    public static String removeUnderscores(@NonNull String text) {
        return text.replace("_", " ");
    }

    public static boolean hasSectionAnchor(@NonNull String text) {
        return text.contains("#");
    }

    public static String removeSectionAnchor(String text) {
        return text.substring(0, text.indexOf("#"));
    }

    public static String sanitizeText(@NonNull String selectedText) {
        return selectedText.replaceAll("\\[\\d+\\]", "") // [1]
                // https://en.wikipedia.org/wiki/Phonetic_symbols_in_Unicode
                .replaceAll("\\s*/[^/]+/;?\\s*", "")
                .replaceAll("\\(\\s*;\\s*", "\\(") // (; -> (    hacky way for IPA remnants
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    /** Fix Html.fromHtml is deprecated problem
     * @param source provided Html string
     * @return returned Spanned of appropriate method according to version check
     * */
    @NonNull public static Spanned fromHtml(@NonNull String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            //noinspection deprecation
            return Html.fromHtml(source);
        }
    }

    private StringUtil() { }
}
