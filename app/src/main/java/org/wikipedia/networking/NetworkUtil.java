package org.wikipedia.networking;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import org.wikipedia.WikipediaApp;

public final class NetworkUtil {

    private static final int MCC_LENGTH = 3;

    /**
     * Read the MCC-MNC (mobile operator code) if available and the cellular data connection is the active one.
     * http://lists.wikimedia.org/pipermail/wikimedia-l/2014-April/071131.html
     * @param ctx Application context.
     * @return The MCC-MNC, typically as ###-##, or null if unable to ascertain (e.g., no actively used cellular)
     */
    static String getMccMnc(Context ctx) {
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

    private NetworkUtil() { }
}
