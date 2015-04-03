package org.wikipedia.util;

import android.content.Context;

import org.wikipedia.R;

import java.util.Locale;

/**
 * A collection of localization related methods.
 *
 * Note the distinction between Article language and device language.
 * Article language is the language of the current page content.
 * Device language is the current language setting in the device system settings.
 * Those can be different.
 */
public final class L10nUtils {
    private L10nUtils() {
    }

    /**
     * Returns true if the translated string for the stylized WP wordmark is equivalent to the
     * English one, so that the PNG image could be used instead. We'd like to avoid bloating up our
     * APK size with extra fonts just to show the logo in the correct font, which we only use
     * rarely (Initial onboarding and ShareAFact).
     * As a compromise we use the PNG image with the correct font for the mainly used
     * languages (and also for languages that haven't translated this value). For all other
     * languages we use a font already available in Android.
     *
     * @param context any valid Context will do (even ApplicationContext)
     * @return true if the translated stylized WP logo text is the same as in English.
     */
    public static boolean canLangUseImageForWikipediaWordmark(Context context) {
        return "<big>W</big>IKIPEDI<big>A</big>".equals(context.getString(R.string.wp_stylized));
    }

    /**
     * Returns true if the device languages is set to an RTL language. Note that this includes
     * RTL_Arabic (AL).
     *
     * @return true if RTL, false if not RTL
     */
    public static boolean isDeviceRTL() {
        final int dir = Character.getDirectionality(Locale.getDefault().getDisplayName().charAt(0));
        return dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }
}
