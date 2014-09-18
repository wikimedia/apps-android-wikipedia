package org.wikipedia.beta.networking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.random.RandomArticleIdTask;



public class ConnectionChangeReceiver extends BroadcastReceiver {
    private WikipediaApp app;
    private RandomArticleIdTask curRandomArticleIdTask;
    private static final int MESSAGE_ZERO_RND = 1;

    public void onReceive(final Context context, Intent intent) {
        app = (WikipediaApp)context;
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();

        // if user isn't now completely offline
        if (networkInfo != null) {
            NetworkInfo.State currentState = networkInfo.getState();

            /*
            We care both if a new network connection was made or when one of 2 or more connections is closed.
            NetworkInfo.State.CONNECTED => isConnected(), but let's call isConnected as documentation suggests.
            We don't need to check against the zeroconfig API unless the (latest) W0 state is *on* (true).
             */
            if (WikipediaApp.getWikipediaZeroDisposition()
                    && (currentState == NetworkInfo.State.CONNECTED
                        || currentState == NetworkInfo.State.DISCONNECTED)
                    && networkInfo.isConnected()
               ) {

                // OK, now check if we're still eligible for zero-rating
                Handler wikipediaZeroRandomHandler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        RandomArticleIdTask randomTask = new RandomArticleIdTask(app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), context) {
                            @Override
                            public void onCatch(Throwable caught) {
                                // oh snap
                                Log.d("Wikipedia", "Random article ID retrieval failed");
                                curRandomArticleIdTask = null;
                            }
                        };
                        if (curRandomArticleIdTask != null) {
                            // if this connection was hung, clean up a bit
                            curRandomArticleIdTask.cancel();
                        }
                        curRandomArticleIdTask = randomTask;
                        curRandomArticleIdTask.execute();
                        return true;
                    }
                });

                wikipediaZeroRandomHandler.removeMessages(MESSAGE_ZERO_RND);
                Message zeroMessage = Message.obtain();
                zeroMessage.what = MESSAGE_ZERO_RND;
                zeroMessage.obj = "zero_eligible_random_check";

                wikipediaZeroRandomHandler.sendMessage(zeroMessage);
            }
        }
    }

}
