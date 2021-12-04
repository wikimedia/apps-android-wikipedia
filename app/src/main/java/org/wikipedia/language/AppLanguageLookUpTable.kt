package org.wikipedia.language;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Immutable look up table for all app supported languages. All article languages may not be
  * present in this table as it is statically bundled with the app. */
public class AppLanguageLookUpTable {
    public static final String SIMPLIFIED_CHINESE_LANGUAGE_CODE = "zh-hans";
    public static final String TRADITIONAL_CHINESE_LANGUAGE_CODE = "zh-hant";
    public static final String CHINESE_CN_LANGUAGE_CODE = "zh-cn";
    public static final String CHINESE_HK_LANGUAGE_CODE = "zh-hk";
    public static final String CHINESE_MO_LANGUAGE_CODE = "zh-mo";
    public static final String CHINESE_MY_LANGUAGE_CODE = "zh-my";
    public static final String CHINESE_SG_LANGUAGE_CODE = "zh-sg";
    public static final String CHINESE_TW_LANGUAGE_CODE = "zh-tw";
    public static final String CHINESE_YUE_LANGUAGE_CODE = "zh-yue";
    public static final String CHINESE_LANGUAGE_CODE = "zh";
    public static final String NORWEGIAN_LEGACY_LANGUAGE_CODE = "no";
    public static final String NORWEGIAN_BOKMAL_LANGUAGE_CODE = "nb";
    public static final String BELARUSIAN_LEGACY_LANGUAGE_CODE = "be-x-old";
    public static final String BELARUSIAN_TARASK_LANGUAGE_CODE = "be-tarask";
    public static final String TEST_LANGUAGE_CODE = "test";
    public static final String FALLBACK_LANGUAGE_CODE = "en"; // Must exist in preference_language_keys.

    @NonNull private final Resources resources;

    // Language codes for all app supported languages in fixed order. The special code representing
    // the dynamic system language is null.
    @NonNull private SoftReference<List<String>> codesRef = new SoftReference<>(null);

    // English names for all app supported languages in fixed order.
    @NonNull private SoftReference<List<String>> canonicalNamesRef = new SoftReference<>(null);

    // Native names for all app supported languages in fixed order.
    @NonNull private SoftReference<List<String>> localizedNamesRef = new SoftReference<>(null);

    // Fallback language codes for language variants
    @NonNull private SoftReference<Map<String, List<String>>> languagesVariantsRef = new SoftReference<>(null);

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
        String name = defaultIndex(getCanonicalNames(), indexOfCode(code), null);
        if (TextUtils.isEmpty(name) && !TextUtils.isEmpty(code)) {
            if (code.equals(Locale.CHINESE.getLanguage())) {
                name = Locale.CHINESE.getDisplayName(Locale.ENGLISH);
            } else if (code.equals(NORWEGIAN_LEGACY_LANGUAGE_CODE)) {
                name = defaultIndex(getCanonicalNames(), indexOfCode(NORWEGIAN_BOKMAL_LANGUAGE_CODE), null);
            }
        }
        return name;
    }

    @Nullable
    public String getLocalizedName(@Nullable String code) {
        String name = defaultIndex(getLocalizedNames(), indexOfCode(code), null);
        if (TextUtils.isEmpty(name) && !TextUtils.isEmpty(code)) {
            if (code.equals(Locale.CHINESE.getLanguage())) {
                name = Locale.CHINESE.getDisplayName(Locale.CHINESE);
            } else if (code.equals(NORWEGIAN_LEGACY_LANGUAGE_CODE)) {
                name = defaultIndex(getLocalizedNames(), indexOfCode(NORWEGIAN_BOKMAL_LANGUAGE_CODE), null);
            }
        }
        return name;
    }

    @Nullable
    public List<String> getLanguageVariants(@Nullable String code) {
        return getLanguagesVariants().get(code);
    }

    @Nullable
    public String getDefaultLanguageCodeFromVariant(@Nullable String code) {
        for (Map.Entry<String, List<String>> entry : getLanguagesVariants().entrySet()) {
            if (entry.getValue().contains(code)) {
                return entry.getKey();
            }
        }
        return null;
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

    private Map<String, List<String>> getLanguagesVariants() {
        Map<String, List<String>> map = languagesVariantsRef.get();
        if (map == null) {
            map = new HashMap<>();
            for (String fallbacks : getStringList(R.array.preference_language_variants)) {
                String[] array = fallbacks.split(",");
                if (array.length > 1) {
                    map.put(array[0], new ArrayList<>(Arrays.asList(array).subList(1, array.length)));
                }
            }
        }
        return map;
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
    public int indexOfCode(@Nullable String code) {
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
