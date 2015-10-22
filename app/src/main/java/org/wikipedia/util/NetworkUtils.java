package org.wikipedia.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

public final class NetworkUtils {

    private static final int MCC_LENGTH = 3;

    /** Private constructor, so nobody can construct NetworkUtils. */
    private NetworkUtils() { }

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
                    && (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE || networkInfo.getType() == ConnectivityManager.TYPE_WIMAX)) {
                TelephonyManager t = (TelephonyManager)ctx.getSystemService(WikipediaApp.TELEPHONY_SERVICE);
                if (t != null && t.getPhoneType() >= 0) {
                    mccMncNetwork = t.getNetworkOperator();
                    if (mccMncNetwork != null) {
                        mccMncNetwork = mccMncNetwork.substring(0, MCC_LENGTH) + "-" + mccMncNetwork.substring(MCC_LENGTH);
                    } else {
                        mccMncNetwork = "000-00";
                    }

                    // TelephonyManager documentation refers to MCC-MNC unreliability on CDMA,
                    // and we actually see that network and SIM MCC-MNC don't always agree,
                    // so let's check the SIM, too. Let's not worry if it's CDMA, as the def of CDMA is complex.
                    mccMncSim = t.getSimOperator();
                    if (mccMncSim != null) {
                        mccMncSim = mccMncSim.substring(0, MCC_LENGTH) + "-" + mccMncSim.substring(MCC_LENGTH);
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
     * Resolves a potentially protocol relative URL to a 'full' URL
     *
     * @param url Url to check for (and fix) protocol relativeness
     * @return A fully qualified, protocol specified URL
     */
    public static String resolveProtocolRelativeUrl(String url) {
        return (url.startsWith("//") ? WikipediaApp.getInstance().getNetworkProtocol() + ":" + url : url);
    }

    /**
     * Ask user to try connecting again upon (hopefully) recoverable network failure.
     */
    public static void toastNetworkFail() {
        Toast.makeText(WikipediaApp.getInstance(), R.string.error_network_error_try_again, Toast.LENGTH_LONG).show();
    }

    /**
     * Checks for active network connection.
     * @param ctx Application context.
     * @return True if an active network connection is present, false if not.
     */
    public static boolean isNetworkConnectionPresent(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}
