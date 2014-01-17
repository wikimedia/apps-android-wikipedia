package org.wikipedia.networking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import org.wikipedia.WikipediaApp;
import org.wikipedia.zero.WikipediaZeroTask;
import org.wikipedia.R;


public class ConnectionChangeReceiver extends BroadcastReceiver {
    private static boolean previousZeroState = false;
    private WikipediaApp app;
    private WikipediaZeroTask curZeroTask;
    private static final int MESSAGE_ZERO = 1;

    public void onReceive(final Context context, Intent intent) {
        app = (WikipediaApp)context.getApplicationContext();
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();

        // if user isn't now completely offline
        if (networkInfo != null) {
            NetworkInfo.State currentState = networkInfo.getState();

            // we care both if a new network connection was made or when one of 2 or more connections is closed
            if (currentState == NetworkInfo.State.CONNECTED || currentState == NetworkInfo.State.DISCONNECTED) {

                // OK, now check if we're eligible for zero-rating
                Handler wikipediaZeroHandler = new Handler(new Handler.Callback(){
                    @Override
                    public boolean handleMessage(Message msg) {
                        WikipediaZeroTask zeroTask = new WikipediaZeroTask(app.getAPIForSite(app.getPrimarySite())) {
                            @Override
                            public void onFinish(Boolean result) {
                                Log.d("Wikipedia", "Wikipedia Zero Eligibility Status: " + result);

                                String toastVerbiage;
                                if (!previousZeroState && result) {
                                    toastVerbiage = context.getString(R.string.zero_free_verbiage);
                                } else if (previousZeroState && !result) {
                                    toastVerbiage = context.getString(R.string.zero_charged_verbiage);
                                } else {
                                    return;
                                }
                                previousZeroState = result;
                                Toast.makeText(context, toastVerbiage, Toast.LENGTH_LONG).show();

                                curZeroTask = null;
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
        }
    }

}
