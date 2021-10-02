package org.wikipedia.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import org.wikipedia.R

object CustomTabsUtil {

    fun openInCustomTab(context: Context, url: String) {
        val colors = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
                .setNavigationBarColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
                .setSecondaryToolbarColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
                .setNavigationBarDividerColor(ResourceUtil.getThemedColor(context, R.attr.material_theme_secondary_color))
                .build()
        CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colors)
                .build()
                .launchUrl(context, Uri.parse(url))
    }
}
