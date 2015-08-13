package org.wikipedia.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// TODO: Replace with Apache Commons Lang StringUtils.
public final class StringUtil {
    private static final String CSV_DELIMITER = ",";

    @NonNull
    public static String emptyIfNull(@Nullable String value) {
        return defaultIfNull(value, "");
    }

    @NonNull
    public static CharSequence emptyIfNull(@Nullable CharSequence value) {
        return defaultIfNull(value, "");
    }

    @Nullable
    public static String defaultIfNull(@Nullable String value, @Nullable String defaultValue) {
        return value == null ? defaultValue : value;
    }

    @Nullable
    public static CharSequence defaultIfNull(@Nullable CharSequence value,
            @Nullable CharSequence defaultValue) {
        return value == null ? defaultValue : value;
    }

    @NonNull
    public static String listToCsv(@NonNull List<String> list) {
        return listToDelimitedString(list, CSV_DELIMITER);
    }

    @NonNull
    public static String listToDelimitedString(@NonNull Iterable<String> list,
                                               @NonNull String delimiter) {
        return TextUtils.join(delimiter, list);
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

    @NonNull
    public static String[] removeNulls(@NonNull String[] args) {
        List<String> list = new ArrayList<>(Arrays.asList(args));
        list.removeAll(Collections.singleton(null));
        return list.toArray(new String[list.size()]);
    }

    private StringUtil() { }
}