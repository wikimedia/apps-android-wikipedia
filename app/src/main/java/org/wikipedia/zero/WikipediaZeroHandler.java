package org.wikipedia.zero;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.WikipediaZeroStateChangeEvent;
import org.wikipedia.random.RandomArticleIdTask;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.OnHeaderCheckListener;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;

import retrofit.client.Header;
import retrofit.client.Response;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import java.net.URL;
import java.util.List;

public class WikipediaZeroHandler extends BroadcastReceiver implements OnHeaderCheckListener {
    private static final int MESSAGE_ZERO_RND = 1;
    private static final int MESSAGE_ZERO_CS = 2;

    /**
     * Size of the text, in sp, of the Zero banner text.
     */
    private static final int BANNER_TEXT_SIZE = 20;
    /**
     * Height of the Zero banner, in pixels, that will pop up from the bottom of the screen.
     */
    private static final int BANNER_HEIGHT = (int) (192 * WikipediaApp.getInstance().getScreenDensity());

    private WikipediaApp app;

    private boolean zeroEnabled = false;
    private volatile boolean acquiringCarrierMessage = false;
    private ZeroConfig zeroConfig;

    private String carrierString = "";
    private String carrierMetaString = "";

    private RandomArticleIdTask curRandomArticleIdTask;

    public WikipediaZeroHandler(WikipediaApp app) {
        this.app = app;
        IntentFilter connFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        app.registerReceiver(this, connFilter);
    }

    public boolean isZeroEnabled() {
        return zeroEnabled;
    }

    public ZeroConfig getZeroConfig() {
        return zeroConfig;
    }

    public static void showZeroBanner(@NonNull Activity activity, @NonNull String text,
                                      @ColorInt int foreColor, @ColorInt int backColor) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(activity.getWindow().getDecorView(), text, FeedbackUtil.LENGTH_DEFAULT);
        ViewGroup rootView = (ViewGroup) snackbar.getView();
        TextView textView = (TextView) rootView.findViewById(R.id.snackbar_text);
        rootView.setBackgroundColor(backColor);
        textView.setTextColor(foreColor);
        textView.setTextSize(BANNER_TEXT_SIZE);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        rootView.setMinimumHeight(BANNER_HEIGHT);
        snackbar.show();
    }

    /** For MW API responses */
    // Note: keep in sync with next method. This one will go away when we've retrofitted all
    // API calls.
    @Override
    public void onHeaderCheck(final ApiResult result, final URL apiURL) {
        if (acquiringCarrierMessage) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                    boolean hasZeroHeader = result.getHeaders().containsKey("X-Carrier");
                if (hasZeroHeader) {
                    String xCarrier = result.getHeaders().get("X-Carrier").get(0);
                    String xCarrierMeta = "";
                    if (result.getHeaders().containsKey("X-Carrier-Meta")) {
                        xCarrierMeta = result.getHeaders().get("X-Carrier-Meta").get(0);
                    }
                    if (!(xCarrier.equals(carrierString) && xCarrierMeta.equals(carrierMetaString))) {
                        identifyZeroCarrier(xCarrier, xCarrierMeta);
                    }
                } else if (zeroEnabled) {
                    zeroOff();
                }
            }
        });
    }

    /** For Retrofit responses */
    public void onHeaderCheck(final Response response) {
        if (acquiringCarrierMessage) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    String xCarrier = getHeader(response, "X-Carrier");
                    String xCarrierMeta = "";
                    if (getHeader(response, "X-Carrier-Meta") != null) {
                        xCarrierMeta = getHeader(response, "X-Carrier-Meta");
                    }
                    if (xCarrier != null) {
                        if (!(xCarrier.equals(carrierString) && xCarrierMeta.equals(carrierMetaString))) {
                            identifyZeroCarrier(xCarrier, xCarrierMeta);
                        }
                    } else if (zeroEnabled) {
                        zeroOff();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("interesting response", e);
                }
            }
        });
    }

    private void zeroOff() {
        carrierString = "";
        zeroConfig = null;
        zeroEnabled = false;
        app.getBus().post(new WikipediaZeroStateChangeEvent());
    }

    private String getHeader(Response response, String key) {
        List<Header> headers = response.getHeaders();
        for (Header header: headers) {
            if (key.equalsIgnoreCase(header.getName())) {
                return header.getValue();
            }
        }
        return null;
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
                        RandomArticleIdTask randomTask = new RandomArticleIdTask(app.getAPIForSite(app.getSite()), app.getSite()) {
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

    private void identifyZeroCarrier(final String xCarrier, final String xCarrierMeta) {
        Handler wikipediaZeroHandler = new Handler(new Handler.Callback() {
            private WikipediaZeroTask curZeroTask;

            @Override
            public boolean handleMessage(Message msg) {
                WikipediaZeroTask zeroTask = new WikipediaZeroTask(app.getApiForMobileSite(app.getSite()), app.getUserAgent()) {
                    @Override
                    public void onFinish(ZeroConfig config) {
                        L.d("New Wikipedia Zero config: " + config);

                        if (config != null) {
                            carrierString = xCarrier;
                            carrierMetaString = xCarrierMeta;
                            zeroConfig = config;
                            zeroEnabled = true;
                            app.getBus().post(new WikipediaZeroStateChangeEvent());
                            curZeroTask = null;
                        }
                        acquiringCarrierMessage = false;
                    }

                    @Override
                    public void onCatch(Throwable caught) {
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