package org.wikipedia.util

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import org.wikipedia.R
import org.wikipedia.util.log.L


object CustomTabsUtil {

    fun openInCustomTab(context: Context, url: String) {
        val colors = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
                .setNavigationBarColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
                .setSecondaryToolbarColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
                .setNavigationBarDividerColor(ResourceUtil.getThemedColor(context, R.attr.secondary_color))
                .build()
        CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colors)
                .build()
                .launchUrl(context, Uri.parse(url))
    }

    fun openInCustomTab(context: Context, packageName: String, url: String) {
        var customTabsClient: CustomTabsClient?
        CustomTabsClient.bindCustomTabsService(context, packageName, object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                L.d("CustomTabClient onCustomTabsServiceConnected")
                customTabsClient = client
                customTabsClient?.warmup(0)
                val session = customTabsClient?.newSession(CustomTabsCallback())
                session?.mayLaunchUrl(Uri.parse(url), null, null)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                L.d("CustomTabClient onServiceDisconnected")
                customTabsClient = null
            }
        })


        val colors = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
            .setNavigationBarColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
            .setSecondaryToolbarColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
            .setNavigationBarDividerColor(ResourceUtil.getThemedColor(context, R.attr.secondary_color))
            .build()
        CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colors)
            .build()
            .launchUrl(context, Uri.parse(url))
    }
}
