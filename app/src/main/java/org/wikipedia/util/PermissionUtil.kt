package org.wikipedia.util

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.wikipedia.settings.Prefs

/**
 * Common methods for dealing with runtime permissions.
 */
object PermissionUtil {
    @JvmStatic
    fun isPermitted(grantResults: IntArray): Boolean {
        // If request is cancelled, the result arrays are empty.
        return grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun shouldShowWritePermissionRationale(activity: AppCompatActivity): Boolean {
        return (!Prefs.askedForPermissionOnce(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    @JvmStatic
    fun hasWriteExternalStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun requestWriteStorageRuntimePermissions(fragment: Fragment, requestCode: Int) {
        fragment.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), requestCode)
        // once permission is granted/denied it will continue with onRequestPermissionsResult
    }

    @JvmStatic
    fun requestWriteStorageRuntimePermissions(activity: AppCompatActivity, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), requestCode)
        // once permission is granted/denied it will continue with onRequestPermissionsResult
    }
}
