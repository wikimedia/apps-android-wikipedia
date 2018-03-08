package org.wikipedia.espresso.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import java.util.Locale;

import static org.wikipedia.espresso.Constants.DEFAULT_LANGUAGE_OF_TESTING_DEVICE;
import static org.wikipedia.espresso.Constants.HEIGHT_OF_TESTING_DEVICE;
import static org.wikipedia.espresso.Constants.SDK_VERSION_OF_TESTING_DEVICE;
import static org.wikipedia.espresso.Constants.WIDTH_OF_TESTING_DEVICE;

public final class ConfigurationTools {

    private Context context;

    public ConfigurationTools(@NonNull Context context) {
        this.context = context;
    }

    public void checkDeviceConfigurations() throws RuntimeException {
        if (!checkDeviceResolution()) {
            throw new RuntimeException("Your device's resolution does not meet the requirement. The default resolution for testing is "
                    + HEIGHT_OF_TESTING_DEVICE + "x" + WIDTH_OF_TESTING_DEVICE);
        }

        if (!checkDeviceSDK()) {
            throw new RuntimeException("Your device's SDK version does not meet the requirement. The SDK version for testing is SDK"
                    + SDK_VERSION_OF_TESTING_DEVICE);
        }

        if (!checkHardwareButtons()) {
            throw new RuntimeException("Your device does not have the hardware buttons (back, home, menu), which does not meet the requirement.");
        }

        if (!checkDeviceLanguage()) {
            throw new RuntimeException("Please set your device language to " + DEFAULT_LANGUAGE_OF_TESTING_DEVICE);
        }

    }

    private boolean checkDeviceResolution() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        return height == HEIGHT_OF_TESTING_DEVICE && width == WIDTH_OF_TESTING_DEVICE;
    }

    private boolean checkDeviceSDK() {
        return android.os.Build.VERSION.SDK_INT == SDK_VERSION_OF_TESTING_DEVICE;
    }

    // The hardware buttons are: Home, Menu and Back
    private boolean checkHardwareButtons() {
        return ViewConfiguration.get(context).hasPermanentMenuKey();
    }

    private boolean checkDeviceLanguage() {
        return Locale.getDefault().getDisplayLanguage().equals(DEFAULT_LANGUAGE_OF_TESTING_DEVICE);
    }
}
