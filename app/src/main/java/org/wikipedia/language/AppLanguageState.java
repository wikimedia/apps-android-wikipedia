package org.wikipedia.language;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.wikipedia.language.AppLanguageLookUpTable.TEST_LANGUAGE_CODE;

/** Language lookup and state management for the application language and most recently used article
 * and application languages. */
public class AppLanguageState {
    @NonNull
    private final AppLanguageLookUpTable appLanguageLookUpTable;

    // Language codes that have been explicitly chosen by the user in most recently used order. This
    // list includes both app and article languages.
    @NonNull
    private final List<String> mruLanguageCodes;

    @NonNull
    private final List<String> appLanguageCodes;

    public AppLanguageState(@NonNull Context context) {
        appLanguageLookUpTable = new AppLanguageLookUpTable(context);
        mruLanguageCodes = new ArrayList<>(StringUtil.csvToList(defaultString(Prefs.INSTANCE.getMruLanguageCodeCsv())));
        appLanguageCodes = new ArrayList<>(StringUtil.csvToList(defaultString(Prefs.INSTANCE.getAppLanguageCodeCsv())));
        initAppLanguageCodes();
    }

    @NonNull
    public List<String> getAppLanguageCodes() {
        if (appLanguageCodes.isEmpty()) {
            // very bad, should not happen.
            initAppLanguageCodes();
        }
        return appLanguageCodes;
    }

    public void addAppLanguageCode(@Nullable String code) {
        appLanguageCodes.remove(code);
        appLanguageCodes.add(code);
        Prefs.INSTANCE.setAppLanguageCodeCsv(StringUtil.listToCsv(appLanguageCodes));
        WikipediaApp.getInstance().resetWikiSite();
    }

    public void setAppLanguageCodes(@NonNull List<String> codes) {
        appLanguageCodes.clear();
        for (String code : codes) {
            if (!TextUtils.isEmpty(code)) {
                appLanguageCodes.add(code);
            }
        }
        Prefs.INSTANCE.setAppLanguageCodeCsv(StringUtil.listToCsv(appLanguageCodes));
        WikipediaApp.getInstance().resetWikiSite();
    }

    public void removeAppLanguageCodes(@NonNull List<String> codes) {
        if (appLanguageCodes.size() > 1) {
            appLanguageCodes.removeAll(codes);
            Prefs.INSTANCE.setAppLanguageCodeCsv(StringUtil.listToCsv(appLanguageCodes));
        }
    }

    private void initAppLanguageCodes() {
        if (appLanguageCodes.isEmpty()) {
            if (Prefs.INSTANCE.isInitialOnboardingEnabled()) {
                setAppLanguageCodes(getRemainingAvailableLanguageCodes());
            } else {
                // If user has never changed app language before
                addAppLanguageCode(getSystemLanguageCode());
            }
        }
    }

    @NonNull
    public String getAppLanguageCode() {
        return getAppLanguageCodes().get(0);
    }

    @NonNull public List<String> getRemainingAvailableLanguageCodes() {
        List<String> list = new ArrayList<>();
        for (String code : LanguageUtil.getAvailableLanguages()) {
            if (!appLanguageCodes.contains(code)
                    && appLanguageLookUpTable.isSupportedCode(code)) {
                list.add(code);
            }
        }
        return list;
    }

    @NonNull public String getSystemLanguageCode() {
        String code = LanguageUtil.localeToWikiLanguageCode(Locale.getDefault());
        return appLanguageLookUpTable.isSupportedCode(code)
                ? code : AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE;
    }

    @NonNull public List<String> getMruLanguageCodes() {
        return mruLanguageCodes;
    }

    public void addMruLanguageCode(@Nullable String code) {
        mruLanguageCodes.remove(code);
        mruLanguageCodes.add(0, code);
        Prefs.INSTANCE.setMruLanguageCodeCsv(StringUtil.listToCsv(mruLanguageCodes));
    }

    /** @return All app supported languages in MRU order. */
    public List<String> getAppMruLanguageCodes() {
        List<String> codes = new ArrayList<>(appLanguageLookUpTable.getCodes());
        int insertIndex = 0;
        for (String code : mruLanguageCodes) {
            if (codes.contains(code)) {
                codes.remove(code);
                codes.add(insertIndex, code);
                ++insertIndex;
            }
        }
        if (!Prefs.INSTANCE.isShowDeveloperSettingsEnabled() && !ReleaseUtil.isPreBetaRelease()) {
            codes.remove(TEST_LANGUAGE_CODE);
        }
        return codes;
    }

    /** @return English name if app language is supported. */
    @Nullable
    public String getAppLanguageCanonicalName(@Nullable String code) {
        return appLanguageLookUpTable.getCanonicalName(code);
    }

    @Nullable
    public String getAppLanguageLocalizedNames() {
        List<String> list = new ArrayList<>();
        for (String code : getAppLanguageCodes()) {
            list.add(StringUtils.capitalize(getAppLanguageLocalizedName(code)));
        }
        return TextUtils.join(", ", list);
    }

    /** @return Native name if app language is supported. */
    @Nullable
    public String getAppLanguageLocalizedName(@Nullable String code) {
        return appLanguageLookUpTable.getLocalizedName(code);
    }

    @Nullable
    public List<String> getLanguageVariants(@Nullable String code) {
        return appLanguageLookUpTable.getLanguageVariants(code);
    }

    @Nullable
    public String getDefaultLanguageCode(@Nullable String code) {
        return appLanguageLookUpTable.getDefaultLanguageCodeFromVariant(code);
    }

    public int getLanguageCodeIndex(@Nullable String code) {
        return appLanguageLookUpTable.indexOfCode(code);
    }
}
