package org.wikipedia.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import org.wikipedia.WikipediaApp;
import org.wikipedia.util.log.L;

import java.util.List;

public final class DeviceUtil {

    /**
     * Utility method to detect whether an Email app is installed,
     * for conditionally enabling/disabling email links.
     * @param context Context of the calling app.
     * @return True if an Email app exists, false otherwise.
     */
    public static boolean mailAppExists(Context context) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:test@wikimedia.org"));
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(intent, 0);
        return resInfo.size() > 0;
    }

    /**
     * Attempt to display the Android keyboard.
     *
     * FIXME: This should not need to exist.
     * Android should always show the keyboard at the appropriate time. This method allows you to display the keyboard
     * when Android fails to do so.
     *
     * @param view The currently focused view that will receive the keyboard input
     */
    public static void showSoftKeyboard(View view) {
        InputMethodManager keyboard = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.toggleSoftInput(0, 0);
    }

    /**
     * Attempt to hide the Android Keyboard.
     *
     * FIXME: This should not need to exist.
     * I do not know why Android does not handle this automatically.
     *
     * @param activity The current activity
     */
    public static void hideSoftKeyboard(Activity activity) {
        hideSoftKeyboard(activity.getWindow().getDecorView());
    }

    public static void hideSoftKeyboard(View view) {
        InputMethodManager keyboard = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        // Not using getCurrentFocus as that sometimes is null, but the keyboard is still up.
        keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void setWindowSoftInputModeResizable(Activity activity) {
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    public static boolean isLocationServiceEnabled(@NonNull Context context) {
        int locationMode = Settings.Secure.LOCATION_MODE_OFF;
        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            L.d("Location service setting not found.", e);
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    private static ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) WikipediaApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static boolean isOnline() {
        NetworkInfo info = getConnectivityManager().getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public static boolean isOnWiFi() {
        NetworkInfo info = getConnectivityManager().getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return info != null && info.isConnected();
    }

    private DeviceUtil() {
    }
}
