package org.wikipedia.language

import android.content.Context
import org.apache.commons.lang3.StringUtils
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.StringUtil
import java.util.*

class AppLanguageState(context: Context) {

    private val appLanguageLookUpTable = AppLanguageLookUpTable(context)

    // Language codes that have been explicitly chosen by the user in most recently used order. This
    // list includes both app and article languages.
    private val _mruLanguageCodes = StringUtil.csvToList(Prefs.mruLanguageCodeCsv.orEmpty()).toMutableList()
    private val _appLanguageCodes = StringUtil.csvToList(Prefs.appLanguageCodeCsv.orEmpty()).toMutableList()

    init {
        initAppLanguageCodes()
    }

    val appLanguageCodes: List<String>
        get() {
            if (_appLanguageCodes.isEmpty()) {
                // very bad, should not happen.
                initAppLanguageCodes()
            }
            return _appLanguageCodes
        }

    val mruLanguageCodes: List<String>
        get() = _mruLanguageCodes

    val appLanguageCode: String
        get() = appLanguageCodes.first()

    val remainingAvailableLanguageCodes: List<String>
        get() = LanguageUtil.availableLanguages.filter { !_appLanguageCodes.contains(it) && appLanguageLookUpTable.isSupportedCode(it) }

    val systemLanguageCode: String
        get() {
            val code = LanguageUtil.localeToWikiLanguageCode(Locale.getDefault())
            return if (appLanguageLookUpTable.isSupportedCode(code)) code else AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE
        }

    val appMruLanguageCodes: List<String>
        get() {
            val codes = appLanguageLookUpTable.codes.toMutableList()
            var insertIndex = 0
            for (code in _mruLanguageCodes) {
                if (codes.contains(code)) {
                    codes.remove(code)
                    codes.add(insertIndex, code)
                    ++insertIndex
                }
            }
            if (!Prefs.isShowDeveloperSettingsEnabled && !ReleaseUtil.isPreBetaRelease) {
                codes.remove(AppLanguageLookUpTable.TEST_LANGUAGE_CODE)
            }
            return codes
        }

    val appLanguageLocalizedNames: String
        get() {
            return appLanguageCodes.joinToString(", ") {
                StringUtils.capitalize(getAppLanguageLocalizedName(it))
            }
        }

    fun addMruLanguageCode(code: String) {
        _mruLanguageCodes.remove(code)
        _mruLanguageCodes.add(0, code)
        Prefs.mruLanguageCodeCsv = StringUtil.listToCsv(_mruLanguageCodes)
    }

    /** @return English name if app language is supported.
     */
    fun getAppLanguageCanonicalName(code: String?): String? {
        return if (!code.isNullOrEmpty()) {
            appLanguageLookUpTable.getCanonicalName(code).orEmpty().ifEmpty { code }
        } else {
            null
        }
    }

    /** @return Native name if app language is supported.
     */
    fun getAppLanguageLocalizedName(code: String?): String? {
        return if (!code.isNullOrEmpty()) {
            appLanguageLookUpTable.getLocalizedName(code).orEmpty().ifEmpty { code }
        } else {
            null
        }
    }

    fun getLanguageVariants(code: String?): List<String>? {
        return appLanguageLookUpTable.getLanguageVariants(code)
    }

    fun getDefaultLanguageCode(code: String?): String? {
        return appLanguageLookUpTable.getDefaultLanguageCodeFromVariant(code)
    }

    fun getLanguageCodeIndex(code: String?): Int {
        return appLanguageLookUpTable.indexOfCode(code)
    }

    fun addAppLanguageCode(code: String) {
        _appLanguageCodes.remove(code)
        _appLanguageCodes.add(code)
        Prefs.appLanguageCodeCsv = StringUtil.listToCsv(_appLanguageCodes)
        WikipediaApp.getInstance().resetWikiSite()
    }

    fun setAppLanguageCodes(codes: List<String>) {
        _appLanguageCodes.clear()
        _appLanguageCodes.addAll(codes.filter { it.isNotEmpty() })
        Prefs.appLanguageCodeCsv = StringUtil.listToCsv(_appLanguageCodes)
        WikipediaApp.getInstance().resetWikiSite()
    }

    fun removeAppLanguageCodes(codes: List<String>) {
        if (_appLanguageCodes.size > 1) {
            _appLanguageCodes.removeAll(codes)
            Prefs.appLanguageCodeCsv = StringUtil.listToCsv(_appLanguageCodes)
        }
    }

    private fun initAppLanguageCodes() {
        if (_appLanguageCodes.isEmpty()) {
            if (Prefs.isInitialOnboardingEnabled) {
                setAppLanguageCodes(remainingAvailableLanguageCodes)
            } else {
                // If user has never changed app language before
                addAppLanguageCode(systemLanguageCode)
            }
        }
    }
}
