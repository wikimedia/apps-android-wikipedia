package org.wikipedia.interlanguage;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.List;

/** Immutable look up table for all app supported languages. All article languages may not be
  * present in this table as it is statically bundled with the app. */
public class AppLanguageLookUpTable {
    public static final String SIMPLIFIED_CHINESE_LANGUAGE_CODE = "zh-hans";
    public static final String TRADITIONAL_CHINESE_LANGUAGE_CODE = "zh-hant";
    public static final String FALLBACK_LANGUAGE_CODE = "en"; // Must exist in preference_language_keys.

    @NonNull private final Resources resources;

    // Language codes for all app supported languages in fixed order. The special code representing
    // the dynamic system language is null.
    @NonNull private SoftReference<List<String>> codesRef = new SoftReference<>(null);

    // English names for all app supported languages in fixed order.
    @NonNull private SoftReference<List<String>> canonicalNamesRef = new SoftReference<>(null);

    // Native names for all app supported languages in fixed order.
    @NonNull private SoftReference<List<String>> localizedNamesRef = new SoftReference<>(null);

    public AppLanguageLookUpTable(@NonNull Context context) {
        resources = context.getResources();
    }

    /**
     * @return Nonnull immutable list. The special code representing the dynamic system language is
     *         null.
     */
    @NonNull
    public List<String> getCodes() {
        List<String> codes = codesRef.get();
        if (codes == null) {
            codes = getStringList(R.array.preference_language_keys);
            codesRef = new SoftReference<>(codes);
        }
        return codes;
    }

    @Nullable
    public String getCanonicalName(@Nullable String code) {
        return defaultIndex(getCanonicalNames(), indexOfCode(code), null);
    }

    @Nullable
    public String getLocalizedName(@Nullable String code) {
        return defaultIndex(getLocalizedNames(), indexOfCode(code), null);
    }

    private List<String> getCanonicalNames() {
        List<String> names = canonicalNamesRef.get();
        if (names == null) {
            names = getStringList(R.array.preference_language_canonical_names);
            canonicalNamesRef = new SoftReference<>(names);
        }
        return names;
    }

    private List<String> getLocalizedNames() {
        List<String> names = localizedNamesRef.get();
        if (names == null) {
            names = getStringList(R.array.preference_language_local_names);
            localizedNamesRef = new SoftReference<>(names);
        }
        return names;
    }

    public boolean isSupportedCode(@Nullable String code) {
        return getCodes().contains(code);
    }

    private <T> T defaultIndex(List<T> list, int index, T defaultValue) {
        return inBounds(list, index) ? list.get(index) : defaultValue;
    }

    /**
     * Searches #codes for the specified language code and returns the index for use in
     * #canonicalNames and #localizedNames.
     *
     * @param code The language code to search for. The special code representing the dynamic system
     *             language is null.
     * @return The index of the language code or -1 if the code is not supported.
     */
    private int indexOfCode(@Nullable String code) {
        return getCodes().indexOf(code);
    }

    /** @return Nonnull immutable list. */
    @NonNull
    private List<String> getStringList(int id) {
        return Arrays.asList(getStringArray(id));
    }

    private boolean inBounds(List<?> list, int index) {
        return index >= 0 && index < list.size();
    }

    public String[] getStringArray(@ArrayRes int id) {
        return resources.getStringArray(id);
    }
}
