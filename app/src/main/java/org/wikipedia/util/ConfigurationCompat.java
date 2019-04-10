package org.wikipedia.util;

import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

import androidx.annotation.NonNull;

public final class ConfigurationCompat {

    @NonNull public static Locale getLocale(@NonNull Configuration cfg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return cfg.getLocales().get(0);
        }
        //noinspection deprecation
        return cfg.locale;
    }

    private ConfigurationCompat() { }
}
