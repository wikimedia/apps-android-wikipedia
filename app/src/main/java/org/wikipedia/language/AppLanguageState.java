package org.wikipedia.language;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.defaultString;

/** Language lookup and state management for the application language and most recently used article
 * and application languages. */
public class AppLanguageState {
    private static final String SYSTEM_LANGUAGE_CODE = null;

    @NonNull
    private final AppLanguageLookUpTable appLanguageLookUpTable;

    // The language code used by the app when the article language is unspecified. It's possible for
    // this code to be unsupported if the languages supported changes. Null is a special value that
    // indicates the system language should used.
    @Nullable
    private String appLanguageCode;

    // Language codes that have been explicitly chosen by the user in most recently used order. This
    // list includes both app and article languages.
    @NonNull
    private final List<String> mruLanguageCodes;

    public AppLanguageState(@NonNull Context context) {
        appLanguageLookUpTable = new AppLanguageLookUpTable(context);
        appLanguageCode = Prefs.getAppLanguageCode();
        mruLanguageCodes = unmarshalMruLanguageCodes();
    }

    @Nullable
    public String getAppLanguageCode() {
        return appLanguageCode;
    }

    @NonNull
    public String getAppOrSystemLanguageCode() {
        return isSystemLanguageEnabled() ? getSystemLanguageCode() : appLanguageCode;
    }

    public void setAppLanguageCode(@Nullable String code) {
        appLanguageCode = code;
        Prefs.setAppLanguageCode(code);
    }

    private boolean isSystemLanguageEnabled() {
        return isSystemLanguageCode(appLanguageCode);
    }

    private boolean isSystemLanguageCode(@Nullable String code) {
        return StringUtils.equals(code, SYSTEM_LANGUAGE_CODE);
    }

    @NonNull
    public String getSystemLanguageCode() {
        String code = LanguageUtil.languageCodeToWikiLanguageCode(Locale.getDefault().getLanguage());
        return appLanguageLookUpTable.isSupportedCode(code)
                ? code
                : AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE;
    }

    /** Note: returned codes may include languages offered by articles but not the app. */
    @NonNull
    public List<String> getMruLanguageCodes() {
        return mruLanguageCodes;
    }

    public void setMruLanguageCode(@Nullable String code) {
        List<String> codes = getMruLanguageCodes();
        codes.remove(code);
        codes.add(0, code);
        Prefs.setMruLanguageCodeCsv(StringUtil.listToCsv(codes));
    }

    /** @return All app supported languages in MRU order. */
    public List<String> getAppMruLanguageCodes() {
        List<String> codes = new ArrayList<>(appLanguageLookUpTable.getCodes());
        int insertIndex = 0;
        for (String code : getMruLanguageCodes()) {
            if (codes.contains(code)) {
                codes.remove(code);
                codes.add(insertIndex, code);
                ++insertIndex;
            }
        }
        return codes;
    }

    /** @return English name if app language is supported. */
    @Nullable
    public String getAppLanguageCanonicalName(@Nullable String code) {
        return appLanguageLookUpTable.getCanonicalName(code);
    }

    @Nullable
    public String getAppOrSystemLanguageLocalizedName() {
        return getAppLanguageLocalizedName(getAppOrSystemLanguageCode());
    }

    /** @return Native name if app language is supported. */
    @Nullable
    public String getAppLanguageLocalizedName(@Nullable String code) {
        return appLanguageLookUpTable.getLocalizedName(code);
    }

    @NonNull
    private List<String> unmarshalMruLanguageCodes() {
        // Null value is used to indicate that system language should be used.
        String systemLanguageCodeString = String.valueOf(SYSTEM_LANGUAGE_CODE);

        String csv = defaultString(Prefs.getMruLanguageCodeCsv(), systemLanguageCodeString);

        List<String> list = new ArrayList<>(StringUtil.csvToList(csv));

        Collections.replaceAll(list, systemLanguageCodeString, SYSTEM_LANGUAGE_CODE);

        return list;
    }
}
