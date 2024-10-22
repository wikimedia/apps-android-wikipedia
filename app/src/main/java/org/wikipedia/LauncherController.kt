package org.wikipedia

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object LauncherController {

    fun setIcon(icon: LauncherIcon) {
        val context = WikipediaApp.instance.applicationContext
        val packageManager = context.packageManager
        LauncherIcon.entries.forEach { i ->
            packageManager.setComponentEnabledSetting(
                i.getComponentName(context),
                if (i == icon) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

}

enum class LauncherIcon(
    val key: String,
    val background: Int,
    val foreground: Int,
    val label: Int,
) {
    DEFAULT(
        key = "DefaultIcon",
        background = R.drawable.launcher_background,
        foreground = R.drawable.launcher_foreground,
        label = R.string.app_name
    ),
    DONOR(
        key = "DonorIcon",
        background = R.drawable.launcher_background,
        foreground = R.drawable.ic_donor_test_foreground,
        label = R.string.app_name
    );


    fun getComponentName(context: Context): ComponentName {
        return ComponentName(context.packageName, "org.wikipedia.$key")
    }

}