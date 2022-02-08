package org.wikipedia.language

import android.content.Context
import org.wikipedia.R
import java.lang.ref.SoftReference
import java.util.*

class AppLanguageLookUpTable(context: Context) {
    private val resources = context.resources
    private var codesRef = SoftReference<List<String>>(null)
    private var canonicalNamesRef = SoftReference<List<String>>(null)
    private var localizedNamesRef = SoftReference<List<String>>(null)
    private var languagesVariantsRef = SoftReference<Map<String, List<String>>>(null)

    val codes: List<String>
        get() {
            var codes = codesRef.get()
            if (codes == null) {
                codes = getStringList(R.array.preference_language_keys)
                codesRef = SoftReference(codes)
            }
            return codes
        }

    private val canonicalNames: List<String>
        get() {
            var names = canonicalNamesRef.get()
            if (names == null) {
                names = getStringList(R.array.preference_language_canonical_names)
                canonicalNamesRef = SoftReference(names)
            }
            return names
        }

    private val localizedNames: List<String>
        get() {
            var names = localizedNamesRef.get()
            if (names == null) {
                names = getStringList(R.array.preference_language_local_names)
                localizedNamesRef = SoftReference(names)
            }
            return names
        }

    private val languagesVariants: Map<String, List<String>>
        get() {
            var map = languagesVariantsRef.get()
            if (map == null) {
                map = getStringList(R.array.preference_language_variants)
                    .map { it.split(",") }
                    .filter { it.size > 1 }
                    .associate { it[0] to ArrayList(it.subList(1, it.size)) }
                languagesVariantsRef = SoftReference(map)
            }
            return map
        }

    fun getCanonicalName(code: String?): String? {
        var name = canonicalNames.getOrNull(indexOfCode(code))
        if (name.isNullOrEmpty() && !code.isNullOrEmpty()) {
            if (code == Locale.CHINESE.language) {
                name = Locale.CHINESE.getDisplayName(Locale.ENGLISH)
            } else if (code == NORWEGIAN_LEGACY_LANGUAGE_CODE) {
                name = canonicalNames.getOrNull(indexOfCode(NORWEGIAN_BOKMAL_LANGUAGE_CODE))
            }
        }
        return name
    }

    fun getLocalizedName(code: String?): String? {
        var name = localizedNames.getOrNull(indexOfCode(code))
        if (name.isNullOrEmpty() && !code.isNullOrEmpty()) {
            if (code == Locale.CHINESE.language) {
                name = Locale.CHINESE.getDisplayName(Locale.CHINESE)
            } else if (code == NORWEGIAN_LEGACY_LANGUAGE_CODE) {
                name = localizedNames.getOrNull(indexOfCode(NORWEGIAN_BOKMAL_LANGUAGE_CODE))
            }
        }
        return name
    }

    fun getLanguageVariants(code: String?): List<String>? {
        return languagesVariants[code]
    }

    fun getDefaultLanguageCodeFromVariant(code: String?): String? {
        return languagesVariants.entries.firstOrNull { (_, value) -> code in value }?.key
    }

    fun isSupportedCode(code: String?): Boolean {
        return code in codes
    }

    fun indexOfCode(code: String?): Int {
        return codes.indexOf(code)
    }

    private fun getStringList(id: Int): List<String> {
        return resources.getStringArray(id).asList()
    }

    companion object {
        const val SIMPLIFIED_CHINESE_LANGUAGE_CODE = "zh-hans"
        const val TRADITIONAL_CHINESE_LANGUAGE_CODE = "zh-hant"
        const val CHINESE_CN_LANGUAGE_CODE = "zh-cn"
        const val CHINESE_HK_LANGUAGE_CODE = "zh-hk"
        const val CHINESE_MO_LANGUAGE_CODE = "zh-mo"
        const val CHINESE_MY_LANGUAGE_CODE = "zh-my"
        const val CHINESE_SG_LANGUAGE_CODE = "zh-sg"
        const val CHINESE_TW_LANGUAGE_CODE = "zh-tw"
        const val CHINESE_YUE_LANGUAGE_CODE = "zh-yue"
        const val CHINESE_LANGUAGE_CODE = "zh"
        const val NORWEGIAN_LEGACY_LANGUAGE_CODE = "no"
        const val NORWEGIAN_BOKMAL_LANGUAGE_CODE = "nb"
        const val BELARUSIAN_LEGACY_LANGUAGE_CODE = "be-x-old"
        const val BELARUSIAN_TARASK_LANGUAGE_CODE = "be-tarask"
        const val TEST_LANGUAGE_CODE = "test"
        const val FALLBACK_LANGUAGE_CODE = "en" // Must exist in preference_language_keys.
    }
}
