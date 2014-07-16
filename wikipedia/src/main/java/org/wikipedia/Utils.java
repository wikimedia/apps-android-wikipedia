package org.wikipedia;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import com.squareup.otto.Bus;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.ApiResult;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.events.WikipediaZeroInterstitialEvent;
import org.wikipedia.events.WikipediaZeroStateChangeEvent;
import org.wikipedia.zero.WikipediaZeroTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Contains utility methods that Java doesn't have because we can't make code look too good, can we?
 */
public final class Utils {
    /**
     * Private constructor, so nobody can construct Utils.
     *
     * THEIR EVIL PLANS HAVE BEEN THWARTED!!!1
     */
    private Utils() { }

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
     * Creates an MD5 hash of the provided string & returns its base64 representation
     * @param s String to hash
     * @return Base64'd MD5 representation of the string passed in
     */
    public static String md5base64(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes("utf-8"));
            byte[] messageDigest = digest.digest();

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
     * Creates an MD5 hash of the provided string and returns its ASCII representation
     * @param s String to hash
     * @return ASCII MD5 representation of the string passed in
     */
    public static String md5string(String s) {
        StringBuilder hexStr = new StringBuilder();
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes("utf-8"));
            byte[] messageDigest = digest.digest();

            for (byte b : messageDigest) {
                hexStr.append(Integer.toHexString(0xFF & b));
            }
        } catch (NoSuchAlgorithmException e) {
            // This will never happen, yes.
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            // This will never happen, yes.
            throw new RuntimeException(e);
        }
        return hexStr.toString();
    }

    /**
     * Deletes a file or directory, with optional recursion.
     * @param path File or directory to delete.
     * @param recursive Whether to delete all subdirectories and files.
     */
    public static void delete(File path, boolean recursive) {
        if (recursive && path.isDirectory()) {
            String[] children = path.list();
            for (String child : children) {
                delete(new File(path, child), recursive);
            }
        }
        path.delete();
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
     * Ensures that the calling method is on the main thread.
     */
    public static void ensureMainThread() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Method must be called from the Main Thread");
        }
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
        InputMethodManager keyboard = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        // Not using getCurrentFocus as that sometimes is null, but the keyboard is still up.
        keyboard.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
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
     * Read the MCC-MNC (mobile operator code) if available and the cellular data connection is the active one.
     * http://lists.wikimedia.org/pipermail/wikimedia-l/2014-April/071131.html
     * @param ctx Application context.
     * @return The MCC-MNC, typically as ###-##, or null if unable to ascertain (e.g., no actively used cellular)
     */
    public static String getMccMnc(Context ctx) {
        String mccMncNetwork;
        String mccMncSim;
        try {
            ConnectivityManager conn = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = conn.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED
                    && (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE || networkInfo.getType() == ConnectivityManager.TYPE_WIMAX))
            {
                TelephonyManager t = (TelephonyManager)ctx.getSystemService(WikipediaApp.TELEPHONY_SERVICE);
                if (t != null && t.getPhoneType() >= 0) {
                    mccMncNetwork = t.getNetworkOperator();
                    if (mccMncNetwork != null) {
                        mccMncNetwork = mccMncNetwork.substring(0, 3) + "-" + mccMncNetwork.substring(3);
                    } else {
                        mccMncNetwork = "000-00";
                    }

                    // TelephonyManager documentation refers to MCC-MNC unreliability on CDMA,
                    // and we actually see that network and SIM MCC-MNC don't always agree,
                    // so let's check the SIM, too. Let's not worry if it's CDMA, as the def of CDMA is complex.
                    mccMncSim = t.getSimOperator();
                    if (mccMncSim != null) {
                        mccMncSim = mccMncSim.substring(0, 3) + "-" + mccMncSim.substring(3);
                    } else {
                        mccMncSim = "000-00";
                    }

                    return mccMncNetwork + "," + mccMncSim;
                }
            }
            return null;
        } catch (Throwable t) {
            // Because, despite best efforts, things can go wrong and we don't want to crash the app:
            return null;
        }
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
    private static final String[] RTL_LANGS = {
            "ar", "arc", "arz", "bcc", "bqi", "ckb", "dv", "fa", "glk", "ha", "he",
            "khw", "ks", "mzn", "pnb", "ps", "sd", "ug", "ur", "yi"
    };

    /**
     * Returns true if the given wiki language is to be displayed RTL.
     *
     * @param lang Wiki code for the language to check for directionality
     * @return true if it is RTL, false if LTR
     */
    public static boolean isLangRTL(String lang) {
        return Arrays.binarySearch(RTL_LANGS, lang, null) >= 0;
    }

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

    /**
     * Returns db name for given site
     *
     * WARNING: HARDCODED TO WORK FOR WIKIPEDIA ONLY
     *
     * @param site Site object to get dbname for
     * @return dbname for given site object
     */
    public static String getDBNameForSite(Site site) {
        return site.getLanguage() + "wiki";
    }

    public static void handleExternalLink(final Context context, final Uri uri) {
        if (WikipediaApp.isWikipediaZeroDevmodeOn() && WikipediaApp.getWikipediaZeroDisposition()) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPref.getBoolean(WikipediaApp.PREFERENCE_ZERO_INTERSTITIAL, true)) {
                WikipediaApp.getInstance().getBus().post(new WikipediaZeroInterstitialEvent(uri));
            } else {
                Utils.visitInExternalBrowser(context, uri);
            }
        } else {
            Utils.visitInExternalBrowser(context, uri);
        }
    }

    /**
     * Open the specified URI in an external browser (even if our app's intent filter
     * matches the given URI)
     *
     * @param context Context of the calling app
     * @param uri URI to open in an external browser
     */
    public static void visitInExternalBrowser(final Context context, Uri uri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(intent, 0);
        if (!resInfo.isEmpty()) {
            List<Intent> browserIntents = new ArrayList<Intent>();
            for (ResolveInfo resolveInfo : resInfo) {
                String packageName = resolveInfo.activityInfo.packageName;
                // remove our apps from the selection!
                // This ensures that all the variants of the Wiki app (Alpha, Beta, Stable) are never shown
                if (packageName.startsWith("org.wikipedia")) {
                    continue;
                }
                Intent newIntent = new Intent(Intent.ACTION_VIEW);
                newIntent.setData(uri);
                newIntent.setPackage(packageName);
                browserIntents.add(newIntent);
            }
            if (browserIntents.size() > 0) {
                // initialize the chooser intent with one of the browserIntents, and remove that
                // intent from the list, since the chooser already has it, and we don't need to
                // add it again in putExtra. (initialize with the last item in the list, to preserve order)
                Intent chooserIntent = Intent.createChooser(browserIntents.remove(browserIntents.size() - 1), null);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, browserIntents.toArray(new Parcelable[]{}));
                context.startActivity(chooserIntent);
                return;
            }
        }
        // This means that there was no way to handle this link.
        // We will just show a toast now. FIXME: Make this more visible?
        Toast.makeText(context, R.string.error_can_not_process_link, Toast.LENGTH_LONG).show();
    }

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
     * Utility method to copy a stream into another stream.
     *
     * Uses a 16KB buffer.
     *
     * @param in Stream to copy from.
     * @param out Stream to copy to.
     * @throws IOException
     */
    public static void copyStreams(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[16 * 1024]; // 16kb buffer
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    /**
     * Write a JSON object to a file
     * @param file file to be written
     * @param jsonObject content of file
     * @throws IOException when writing failed
     */
    public static void writeToFile(File file, JSONObject jsonObject) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file));
        try {
            writer.write(jsonObject.toString());
        } finally {
            writer.close();
        }
    }

    /**
     * Reads the contents of this page from storage.
     * @return Page object with the contents of the page.
     * @throws IOException
     * @throws JSONException
     */
    public static JSONObject readJSONFile(File f) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String readStr;
            while ((readStr = reader.readLine()) != null) {
                stringBuilder.append(readStr);
            }
            return new JSONObject(stringBuilder.toString());
        } finally {
            reader.close();
        }
    }

    /**
     * Format for formatting/parsing dates to/from the ISO 8601 standard
     */
    private static final String ISO8601_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * Parse a date formatted in ISO8601 format.
     *
     * @param dateString Date String to parse
     * @return Parsed Date object.
     * @throws ParseException
     */
    public static Date parseISO8601(String dateString) throws ParseException {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(ISO8601_FORMAT_STRING, Locale.ROOT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        date.setTime(sdf.parse(dateString).getTime());
        return date;
    }

    /**
     * Format a date to an ISO8601 formatted string.
     *
     * @param date Date to format.
     * @return The given date formatted in ISO8601 format.
     */
    public static String formatISO8601(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO8601_FORMAT_STRING, Locale.ROOT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    /**
     * Resolves a potentially protocol relative URL to a 'full' URL
     *
     * @param url Url to check for (and fix) protocol relativeness
     * @return A fully qualified, protocol specified URL
     */
    public static String resolveProtocolRelativeUrl(String url) {
        String fullUrl;
        if (url.startsWith("//")) {
            // That's a protocol specific link! Make it https!
            fullUrl = WikipediaApp.PROTOCOL + ":" + url;
        } else {
            fullUrl = url;
        }
        return fullUrl;
    }

    /**
     * Ask user to try connecting again upon (hopefully) recoverable network failure.
     */
    public static void toastFail() {
        Toast.makeText(WikipediaApp.getInstance(), R.string.error_network_error_try_again, Toast.LENGTH_LONG).show();
    }

    /**
     *
     * @param actual The exception object
     * @param expected The class you're trying to find, usually tossed by ExceptionImpl.class, for example.
     * @return boolean true if the Throwable type was found in the nested exception change, else false.
     */
    public static boolean throwableContainsSpecificType(Throwable actual, Class expected) {
        if (actual == null) {
            return false;
        } else if (actual.getClass() == expected) {
            return true;
        } else {
            return throwableContainsSpecificType(actual.getCause(), expected);
        }
    }

    /**
     * Calculates the actual font size for the current device, based on an "sp" measurement.
     * @param window The window on which the font will be rendered.
     * @param fontSp Measurement in "sp" units of the font.
     * @return Actual font size for the given sp amount.
     */
    public static float getFontSizeFromSp(Window window, float fontSp) {
        final DisplayMetrics metrics = new DisplayMetrics();
        window.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return fontSp / metrics.scaledDensity;
    }

    /**
     * Resolves the resource ID of a theme-dependent attribute (for example, a color value
     * that changes based on the selected theme)
     * @param activity The activity whose theme contains the attribute.
     * @param id Theme-dependent attribute ID to be resolved.
     * @return The actual resource ID of the requested theme-dependent attribute.
     */
    public static int getThemedAttributeId(Activity activity, int id) {
        TypedValue tv = new TypedValue();
        activity.getTheme().resolveAttribute(id, tv, true);
        return tv.resourceId;
    }
}
