package org.wikipedia

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import org.wikipedia.settings.Prefs

object LauncherController {

    fun setIcon(icon: LauncherIcon) {
        val context = WikipediaApp.instance.applicationContext
        val packageManager = context.packageManager
        LauncherIcon.entries.forEach { launcherIcon ->
            packageManager.setComponentEnabledSetting(
                launcherIcon.getComponentName(context),
                if (launcherIcon == icon) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else
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
    var isSelected: Boolean = false
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
        foreground = R.drawable.ic_launcher_donor_benefit_foreground,
        label = R.string.app_name
    );

    fun getComponentName(context: Context): ComponentName {
        return ComponentName(context.packageName, "org.wikipedia.$key")
    }

    companion object {
        fun initialValues(): List<LauncherIcon> {
            val savedAppIcon = Prefs.currentSelectedAppIcon ?: DEFAULT.key
            entries.forEach { icon ->
                icon.isSelected = icon.key == savedAppIcon
            }
            return entries
        }
    }
}
