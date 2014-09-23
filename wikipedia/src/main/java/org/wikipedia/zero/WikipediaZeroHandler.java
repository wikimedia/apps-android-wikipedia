package org.wikipedia.zero;

import org.wikipedia.WikipediaApp;
import org.wikipedia.events.WikipediaZeroStateChangeEvent;
import org.wikipedia.random.RandomArticleIdTask;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.OnHeaderCheckListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class WikipediaZeroHandler extends BroadcastReceiver implements OnHeaderCheckListener {

    private WikipediaApp app;
    private static final boolean WIKIPEDIA_ZERO_DEV_MODE_ON = true;

    public WikipediaZeroHandler(WikipediaApp app) {
        this.app = app;

        if (WIKIPEDIA_ZERO_DEV_MODE_ON) {
            IntentFilter connFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            app.registerReceiver(this, connFilter);
        }
    }

    private boolean zeroEnabled = false;
    public boolean isZeroEnabled() {
        return zeroEnabled;
    }

    private volatile boolean acquiringCarrierMessage = false;
    private String carrierMessage = "";
    public String getCarrierMessage() {
        return carrierMessage;
    }

    private String carrierString = "";

    private RandomArticleIdTask curRandomArticleIdTask;
    private static final int MESSAGE_ZERO_RND = 1;
    private static final int MESSAGE_ZERO_CS = 2;

    @Override
    public void onHeaderCheck(final ApiResult result) {
        if (!WIKIPEDIA_ZERO_DEV_MODE_ON || acquiringCarrierMessage) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                boolean responseZeroState = result.getHeaders().containsKey("X-CS");
                if (responseZeroState) {
                    String xcs = result.getHeaders().get("X-CS").get(0);
                    if (!xcs.equals(carrierString)) {
                        identifyZeroCarrier(xcs);
                    }
                } else if (zeroEnabled) {
                    carrierString = "";
                    carrierMessage = "";
                    zeroEnabled = false;
                    app.getBus().post(new WikipediaZeroStateChangeEvent());
                }
            }
        });
    }

    public void onReceive(final Context context, Intent intent) {
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
            if (zeroEnabled
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

    private void identifyZeroCarrier(final String xcs) {
        Handler wikipediaZeroHandler = new Handler(new Handler.Callback() {
            private WikipediaZeroTask curZeroTask;

            @Override
            public boolean handleMessage(Message msg) {
                WikipediaZeroTask zeroTask = new WikipediaZeroTask(app.getAPIForSite(app.getPrimarySite()), app) {
                    @Override
                    public void onFinish(String message) {
                        Log.d("Wikipedia", "Wikipedia Zero message: " + message);

                        if (message != null) {
                            carrierString = xcs;
                            carrierMessage = message;
                            zeroEnabled = true;
                            app.getBus().post(new WikipediaZeroStateChangeEvent());
                            curZeroTask = null;
                        }
                        acquiringCarrierMessage = false;
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        // oh snap
                        Log.d("Wikipedia", "Wikipedia Zero Eligibility Check Exception Caught");
                        curZeroTask = null;
                        acquiringCarrierMessage = false;
                    }
                };
                if (curZeroTask != null) {
                    // if this connection was hung, clean up a bit
                    curZeroTask.cancel();
                }
                curZeroTask = zeroTask;
                curZeroTask.execute();
                acquiringCarrierMessage = true;
                return true;
            }
        });

        wikipediaZeroHandler.removeMessages(MESSAGE_ZERO_CS);
        Message zeroMessage = Message.obtain();
        zeroMessage.what = MESSAGE_ZERO_CS;
        zeroMessage.obj = "zero_eligible_check";

        wikipediaZeroHandler.sendMessage(zeroMessage);
    }
}