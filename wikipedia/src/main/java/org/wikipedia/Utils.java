package org.wikipedia;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.text.format.*;
import android.util.*;
import android.view.*;
import android.view.inputmethod.*;
import org.json.*;

import java.io.*;
import java.security.*;
import java.text.*;
import java.util.*;

/**
 * Contains utility methods that Java doesn't have because we can't make code look too good, can we?
 */
public class Utils {
    /**
     * Compares two strings properly, even when one of them is null - without throwing up
     *
     * @param str1 The first string
     * @param str2 Guess?
     * @return true if they are both equal (even if both are null)
     */
    public static boolean compareStrings(String str1, String str2) {
        return (str1 == null ? str2 == null : str1.equals(str2));
    }

    /**
     * Crossfades two views, one of which is assumed to be currently visible
     * @param curView The view that is currently visible
     * @param newView The new view that should be faded in
     */
    public static void crossFade(final View curView, final View newView) {
        fadeIn(newView);
        fadeOut(curView);
    }

    /**
     * Fades in a view.
     * @param view The currently invisible view to be faded in
     */
    public static void fadeIn(final View view) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1.0f)
                .setDuration(WikipediaApp.MEDIUM_ANIMATION_DURATION)
                .setListener(null)
                .start();
    }

    /**
     * Fades out a view.
     * @param view The currently visible view to be faded out
     */
    public static void fadeOut(final View view) {
        view.animate()
                .alpha(0f)
                .setDuration(WikipediaApp.MEDIUM_ANIMATION_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                        view.setAlpha(1.0f);
                    }
                });
    }

    /**
     * Creates an MD5 hash of the provided string & returns its base64 representation
     * @param s String to hash
     * @return Base64'd MD5 representation of the string passed in
     */
    public static final String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes("utf-8"));
            byte messageDigest[] = digest.digest();

            return Base64.encodeToString(messageDigest, Base64.URL_SAFE | Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            // This will never happen, yes.
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            // This will never happen, yes.
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the local file name for a remote image.
     *
     * Warning: Should be kept stable between releases.
     * @param url URL of the thumbnail image. Expects them to be not protocol relative & have an extension.
     * @return
     */
    public static final String imageUrlToFileName(String url) {
        String[] protocolParts = url.split("://");
        return "saved-image-"
                + md5(protocolParts[protocolParts.length - 1]);
    }

    /**
     * Add some utility methods to a communuication bridge, that can be called synchronously from JS
     */
    public static final void addUtilityMethodsToBridge(final Context context, CommunicationBridge bridge) {
        bridge.addListener( "imageUrlToFilePath", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                String imageUrl = messagePayload.optString("imageUrl");
                JSONObject ret = new JSONObject();
                try {
                    File imageFile = new File(context.getFilesDir(), imageUrlToFileName(imageUrl));
                    ret.put("filePath", imageFile.getAbsolutePath());
                    // FIXME: THIS IS BROKEN NOW!!!1
                    throw new RuntimeException("FIX THIS YUVI!");
                } catch (JSONException e) {
                    // stupid, stupid, stupid
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Parses dates from the format MediaWiki uses.
     *
     * @param mwDate String representing Date returned from a MW API call
     * @return A {@link java.util.Date} object representing that particular date
     */
    public static Date parseMWDate(String mwDate) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Assuming MW always gives me UTC
        try {
            return isoFormat.parse(mwDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Formats provided date relative to the current system time
     * @param date Date to format
     * @return String representing the relative time difference of the paramter from current time
     */
    public static String formatDateRelative(Date date) {
        return DateUtils.getRelativeTimeSpanString(date.getTime(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS, 0).toString();
    }

    /**
     * Ensures that the translationY of a particular view is the given value.
     *
     * If it isn't the current value, then it performs a short animation to make it so.
     *
     * @param view The view to translate
     * @param translation The value to ensure it is translated by
     */
    public static void ensureTranslationY(View view, int translation) {
        if (view.getTranslationY() != translation) {
            view.animate().translationY(translation).setDuration(WikipediaApp.SHORT_ANIMATION_DURATION).start();
        }
    }

    /**
     * Ensures that the calling method is on the main thread.
     */
    public static void ensureMainThread() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Method must be called from the Main Thread");
        }
    }

    /**
     * Attempt to hide the Android Keyboard
     *
     * @param activity The current activity
     */
    public static void hideSoftKeyboard(Activity activity) {
        if (activity.getCurrentFocus() != null) {
            InputMethodManager keyboard = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }
}