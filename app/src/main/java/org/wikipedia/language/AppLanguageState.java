package org.wikipedia.language;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.settings.Prefs;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.defaultString;

/** Language lookup and state management for the application language and most recently used article
 * and application languages. */
public class AppLanguageState {
    @NonNull
    private final AppLanguageLookUpTable appLanguageLookUpTable;

    // The language code used by the app when the article language is unspecified. It's possible for
    // this code to be unsupported if the languages supported changes.
    // TODO: Remove in April 2019
    @Nullable
    private String appLanguageCode;

    // Language codes that have been explicitly chosen by the user in most recently used order. This
    // list includes both app and article languages.
    @NonNull
    private final List<String> mruLanguageCodes;

    @NonNull
    private final List<String> appLanguageCodes;

    public AppLanguageState(@NonNull Context context) {
        appLanguageLookUpTable = new AppLanguageLookUpTable(context);
        appLanguageCode = Prefs.getAppLanguageCode();
        mruLanguageCodes = new ArrayList<>(StringUtil.csvToList(defaultString(Prefs.getMruLanguageCodeCsv())));
        appLanguageCodes = new ArrayList<>(StringUtil.csvToList(defaultString(Prefs.getAppLanguageCodeCsv())));
        initAppLanguageCodes();
    }

    @NonNull
    public List<String> getAppLanguageCodes() {
        return appLanguageCodes;
    }

    @NonNull
    public List<String> addAppLanguageCode(@Nullable String code) {
        return reOrderAppLanguageCode(code, 0);
    }

    @NonNull
    public List<String> reOrderAppLanguageCode(@Nullable String code, int position) {
        appLanguageCodes.remove(code);
        appLanguageCodes.add(position, code);
        Prefs.setAppLanguageCodeCsv(StringUtil.listToCsv(appLanguageCodes));
        return appLanguageCodes;
    }

    @NonNull
    public List<String> removeAppLanguageCode(@Nullable String code) {
        if (appLanguageCodes.size() > 1) {
            appLanguageCodes.remove(code);
            Prefs.setAppLanguageCodeCsv(StringUtil.listToCsv(appLanguageCodes));
        }
        return appLanguageCodes;
    }

    private void initAppLanguageCodes() {
        if (appLanguageCodes.isEmpty()) {
            addAppLanguageCode(!TextUtils.isEmpty(appLanguageCode) ? appLanguageCode : getSystemLanguageCode());
        }
    }

    @Nullable
    public String getAppLanguageCode() {
        if (appLanguageCodes.isEmpty()) {
            // very bad, should not happen.
            initAppLanguageCodes();
        }
        return appLanguageCodes.get(0);
    }

    @NonNull
    public String getSystemLanguageCode() {
        String code = LanguageUtil.localeToWikiLanguageCode(Locale.getDefault());
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
    public String getAppLanguageLocalizedName() {
        return getAppLanguageLocalizedName(getAppLanguageCode());
    }

    /** @return Native name if app language is supported. */
    @Nullable
    public String getAppLanguageLocalizedName(@Nullable String code) {
        return appLanguageLookUpTable.getLocalizedName(code);
    }
}
