package org.wikipedia.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import org.wikipedia.R

object CustomTabsUtil {

    fun openInCustomTab(context: Context, url: String) {
        val colors = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
                .setNavigationBarColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
                .setSecondaryToolbarColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
                .setNavigationBarDividerColor(ResourceUtil.getThemedColor(context, R.attr.secondary_color))
                .build()
        try {
            CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colors)
                .build()
                .launchUrl(context, url.toUri())
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error_browser_not_found, Toast.LENGTH_LONG).show()
        }
    }
}
