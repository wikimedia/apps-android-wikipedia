package org.wikipedia.extensions

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.SparseArray
import androidx.annotation.StringRes
import androidx.core.os.ConfigurationCompat
import org.wikipedia.page.PageTitle
import org.wikipedia.util.L10nUtil.getStringForLocale
import org.wikipedia.util.L10nUtil.setDesiredLocale
import java.util.Locale

fun Context.getString(languageCode: String, @StringRes resId: Int): String {
    return getStringForLocale(this, Locale(languageCode), resId)
}

fun Context.getString(title: PageTitle, @StringRes resId: Int): String {
    return getStringForLocale(this, Locale(title.wikiSite.languageCode), resId)
}

fun Context.getStrings(title: PageTitle, strings: IntArray): SparseArray<String> {
    val targetLocale = Locale(title.wikiSite.languageCode)
    val config = Configuration(resources.configuration)
    val systemLocale = ConfigurationCompat.getLocales(config)[0]
    val localizedStrings = SparseArray<String>()
    if (systemLocale?.language == targetLocale.language) {
        strings.forEach {
            localizedStrings.put(it, getString(it))
        }
        return localizedStrings
    }
    setDesiredLocale(config, targetLocale)
    val targetResources = createConfigurationContext(config).resources
    strings.forEach {
        localizedStrings.put(it, targetResources.getString(it))
    }
    config.setLocale(systemLocale)
    // reset to current configuration
    createConfigurationContext(config)
    return localizedStrings
}

// To be used only for plural strings and strings requiring arguments
fun Context.getResources(languageCode: String): Resources {
    val config = Configuration(resources.configuration)
    val targetLocale = Locale(languageCode)
    val systemLocale = ConfigurationCompat.getLocales(config)[0]
    if (systemLocale?.language == targetLocale.language) {
        return resources
    }
    setDesiredLocale(config, targetLocale)
    val targetResources = createConfigurationContext(config).resources
    config.setLocale(systemLocale)
    // reset to current configuration
    createConfigurationContext(config)
    return targetResources
}
