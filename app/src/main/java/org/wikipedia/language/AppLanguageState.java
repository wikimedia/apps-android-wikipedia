package org.wikipedia.language;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    //Todo:Deprecate the usage by the end of Multilingual changes, and start using appLanguageCodes.get(0)
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
        mruLanguageCodes = unmarshalLanguageCodes(Prefs.getMruLanguageCodeCsv());
        appLanguageCodes = unmarshalLanguageCodes(Prefs.getAppLanguageCodeCsv());
        oneTimeTransferOfAppLanguageCode();
        checkForProperInitializationOfAppLanguageCode();
    }

    private void checkForProperInitializationOfAppLanguageCode() {
        if (appLanguageCodes.get(0) == null) {
            addAppLanguageCode(getSystemLanguageCode());
        }
        appLanguageCodes.remove(null);
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
        appLanguageCodes.remove(code);
        Prefs.setAppLanguageCodeCsv(StringUtil.listToCsv(appLanguageCodes));
        return appLanguageCodes;
    }

    /**
     * Make a transfer of user app language preference to the list, for old users.
     * Todo: Deprecate after Apr 2019 and remove related preferences
     */
    private void oneTimeTransferOfAppLanguageCode() {
        //conditional check to see if we have done this exercise once
        if (appLanguageCode == null || !appLanguageCodes.isEmpty()) {
            return;
        }
        addAppLanguageCode(appLanguageCode);
       //Todo: activate code at the end of MultiLingual changes
        // Prefs.setAppLanguageCode(null);
    }

    @Nullable
    public String getAppLanguageCode() {
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

    @NonNull
    private List<String> unmarshalLanguageCodes(String mruLanguageCodeCsv) {
        // Null value is used to indicate that system language should be used.
        String systemLanguageCodeString = String.valueOf(SYSTEM_LANGUAGE_CODE);

        String csv = defaultString(mruLanguageCodeCsv, systemLanguageCodeString);

        List<String> list = new ArrayList<>(StringUtil.csvToList(csv));

        Collections.replaceAll(list, systemLanguageCodeString, SYSTEM_LANGUAGE_CODE);

        return list;
    }
}
