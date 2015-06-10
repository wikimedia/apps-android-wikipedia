package org.wikipedia.interlanguage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Immutable look up table for all app supported languages. All article languages may not be
  * present in this table as it is statically bundled with the app. */
public class AppLanguageLookUpTable {
    public static final String SIMPLIFIED_CHINESE_LANGUAGE_CODE = "zh-hans";
    public static final String TRADITIONAL_CHINESE_LANGUAGE_CODE = "zh-hant";
    public static final String FALLBACK_LANGUAGE_CODE = "en"; // Must exist in preference_language_keys.

    // Immutable language codes for all app supported languages in fixed order.
    @NonNull
    private final List<String> codes;

    // Immutable English names for all app supported languages in fixed order.
    @NonNull
    private final List<String> canonicalNames;

    // Immutable native names for all app supported languages in fixed order.
    @NonNull
    private final List<String> localizedNames;

    public AppLanguageLookUpTable(@NonNull Context context) {
        codes = getStringList(context, R.array.preference_language_keys); // The codes are the keys.
        canonicalNames = getStringList(context, R.array.preference_language_canonical_names);
        localizedNames = getStringList(context, R.array.preference_language_local_names);
    }

    /** @return Nonnull immutable list. */
    @NonNull
    public List<String> getCodes() {
        return codes;
    }

    @Nullable
    public String getCanonicalName(String code) {
        return defaultIndex(canonicalNames, indexOfCode(code), null);
    }

    @Nullable
    public String getLocalizedName(String code) {
        return defaultIndex(localizedNames, indexOfCode(code), null);
    }

    public boolean isSupportedCode(String code) {
        return getCodes().contains(code);
    }

    private <T> T defaultIndex(List<T> list, int index, T defaultValue) {
        return inBounds(list, index) ? list.get(index) : defaultValue;
    }

    /**
     * Searches #codes for the specified language code and returns the index for use in
     * #canonicalNames and #localizedNames.
     *
     * @param code The language code to search for.
     * @return The index of the language code or -1 if the code is not supported.
     */
    private int indexOfCode(String code) {
        return getCodes().indexOf(code);
    }

    /** @return Nonnull immutable list. */
    @NonNull
    private List<String> getStringList(Context context, int id) {
        String[] array = context.getResources().getStringArray(id);
        return array == null ? Collections.<String>emptyList() : Arrays.asList(array);
    }

    private boolean inBounds(List<?> list, int index) {
        return index >= 0 && index < list.size();
    }
}
