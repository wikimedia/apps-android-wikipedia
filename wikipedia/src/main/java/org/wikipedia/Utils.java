package org.wikipedia;

import com.nineoldandroids.animation.*;
import com.nineoldandroids.view.*;
import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.text.*;
import android.text.format.*;
import android.util.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import com.squareup.otto.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.events.*;
import org.wikipedia.zero.*;

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
        ViewHelper.setAlpha(view, 0f);
        view.setVisibility(View.VISIBLE);
        animate(view)
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
        animate(view)
                .alpha(0f)
                .setDuration(WikipediaApp.MEDIUM_ANIMATION_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    boolean wasCanceled = false;

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        wasCanceled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!wasCanceled) {
                            // Detect if we got canceled, and if so DON'T hide...
                            // There's another animation now pushing the alpha back up
                            view.setVisibility(View.GONE);
                            ViewHelper.setAlpha(view, 1.0f);
                        }
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
        if (ViewHelper.getTranslationY(view) != translation) {
            animate(view).translationY(translation).setDuration(WikipediaApp.SHORT_ANIMATION_DURATION).start();
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

    public static void setupShowPasswordCheck(final CheckBox check, final EditText edit) {
        check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // EditText loses the cursor position when you change the InputType
                int curPos = edit.getSelectionStart();
                if (isChecked) {
                    edit.setInputType(InputType.TYPE_CLASS_TEXT);
                } else {
                    edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                edit.setSelection(curPos);
            }
        });
    }

     /* Inspect an API response, and fire an event to update the UI for Wikipedia Zero On/Off.
     *
     * @param app The application object
     * @param result An API result to inspect for Wikipedia Zero headers
     */
    public static void processHeadersForZero(final WikipediaApp app, final ApiResult result) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Map<String, List<String>> headers = result.getHeaders();
                boolean responseZeroState = headers.containsKey("X-CS");
                if (responseZeroState) {
                    String xcs = headers.get("X-CS").get(0);
                    if (!xcs.equals(WikipediaApp.getXcs())) {
                        identifyZeroCarrier(app, xcs);
                    }
                } else if (WikipediaApp.getWikipediaZeroDisposition()) {
                    WikipediaApp.setXcs("");
                    WikipediaApp.setCarrierMessage("");
                    WikipediaApp.setWikipediaZeroDisposition(responseZeroState);
                    app.getBus().post(new WikipediaZeroStateChangeEvent());
                }
            }
        });
    }

    private static final int MESSAGE_ZERO = 1;

    public static void identifyZeroCarrier(final WikipediaApp app, final String xcs) {
        Handler wikipediaZeroHandler = new Handler(new Handler.Callback(){
            private WikipediaZeroTask curZeroTask;

            @Override
            public boolean handleMessage(Message msg) {
                WikipediaZeroTask zeroTask = new WikipediaZeroTask(app.getAPIForSite(app.getPrimarySite()), app) {
                    @Override
                    public void onFinish(String message) {
                        Log.d("Wikipedia", "Wikipedia Zero message: " + message);

                        if (message != null) {
                            WikipediaApp.setXcs(xcs);
                            WikipediaApp.setCarrierMessage(message);
                            WikipediaApp.setWikipediaZeroDisposition(true);
                            Bus bus = app.getBus();
                            bus.post(new WikipediaZeroStateChangeEvent());
                            curZeroTask = null;
                        }
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        // oh snap
                        Log.d("Wikipedia", "Wikipedia Zero Eligibility Check Exception Caught");
                        curZeroTask = null;
                    }
                };
                if (curZeroTask != null) {
                    // if this connection was hung, clean up a bit
                    curZeroTask.cancel();
                }
                curZeroTask = zeroTask;
                curZeroTask.execute();
                return true;
            }
        });

        wikipediaZeroHandler.removeMessages(MESSAGE_ZERO);
        Message zeroMessage = Message.obtain();
        zeroMessage.what = MESSAGE_ZERO;
        zeroMessage.obj = "zero_eligible_check";

        wikipediaZeroHandler.sendMessage(zeroMessage);
    }

    /**
     * Takes a language code (as returned by Android) and returns a wiki code, as used by wikipedia.
     *
     * @param langCode Language code (as returned by Android)
     * @return Wiki code, as used by wikipedia.
     */
    public static String langCodeToWikiLang(String langCode) {
        // Convert deprecated language codes to modern ones.
        // See https://developer.android.com/reference/java/util/Locale.html
        if (langCode.equals("iw")) {
            return "he"; // Hebrew
        } else if (langCode.equals("in")) {
            return "id"; // Indonesian
        } else if (langCode.equals("ji")) {
            return "yi"; // Yiddish
        }

        return langCode;
    }

    /**
     * List of wiki language codes for which the content is primarily RTL.
     *
     * Ensure that this is always sorted alphabetically.
     */
    private static String[] rtlLangs = {
            "arc", "arz", "ar", "bcc", "bqi", "ckb", "dv", "fa", "glk", "ha", "he",
            "khw", "ks", "mzn", "pnb", "ps", "sd", "ug", "ur", "yi"
    };

    /**
     * Setup directionality for both UI and content elements in a webview.
     *
     * @param contentLang The Content language to use to set directionality. Wiki Language code.
     * @param uiLang The UI language to use to set directionality. Java language code.
     * @param bridge The CommunicationBridge to use to communicate with the WebView
     */
    public static void setupDirectionality(String contentLang, String uiLang, CommunicationBridge bridge) {
        JSONObject payload = new JSONObject();
        try {
            if (isLangRTL(contentLang)) {
                payload.put("contentDirection", "rtl");
            } else {
                payload.put("contentDirection", "ltr");
            }
            if (isLangRTL(langCodeToWikiLang(uiLang))) {
                payload.put("uiDirection", "rtl");
            } else {
                payload.put("uiDirection", "ltr");
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setDirectionality", payload);
    }

    /**
     * Returns true if the given wiki language is to be displayed RTL.
     *
     * @param lang Wiki code for the language to check for directionality
     * @return true if it is RTL, false if LTR
     */
    public static boolean isLangRTL(String lang) {
        return Arrays.binarySearch(rtlLangs, lang, null) >= 0;
    }

    /**
     * Sets text direction (RTL / LTR) for given view based on given lang.
     *
     * Doesn't do anything on pre Android 4.2, since their RTL support is terrible.
     *
     * @param view View to set direction of
     * @param lang Wiki code for the language based on which to set direction
     */
    public static void setTextDirection(View view, String lang) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            view.setTextDirection(Utils.isLangRTL(lang) ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
        }
    }
}
