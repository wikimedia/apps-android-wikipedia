package org.wikipedia.zero;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.WikipediaZeroStateChangeEvent;
import org.wikipedia.random.RandomArticleIdTask;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.OnHeaderCheckListener;
import org.wikipedia.util.FeedbackUtil;

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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class WikipediaZeroHandler extends BroadcastReceiver implements OnHeaderCheckListener {
    private static final boolean WIKIPEDIA_ZERO_DEV_MODE_ON = true;
    /**
     * Size of the text, in sp, of the Zero banner text.
     */
    private static final int BANNER_TEXT_SIZE = 20;
    /**
     * Height of the Zero banner, in pixels, that will pop up from the bottom of the screen.
     */
    private static final int BANNER_HEIGHT = (int) (192 * WikipediaApp.getInstance().getScreenDensity());

    public WikipediaZeroHandler(WikipediaApp app) {
        this.app = app;

        if (WIKIPEDIA_ZERO_DEV_MODE_ON) {
            IntentFilter connFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            app.registerReceiver(this, connFilter);
        }
    }

    private WikipediaApp app;
    private boolean zeroEnabled = false;
    public boolean isZeroEnabled() {
        return zeroEnabled;
    }

    private volatile boolean acquiringCarrierMessage = false;
    private ZeroMessage carrierMessage;
    public ZeroMessage getCarrierMessage() {
        return carrierMessage;
    }

    private String carrierString = "";

    private RandomArticleIdTask curRandomArticleIdTask;
    private static final int MESSAGE_ZERO_RND = 1;
    private static final int MESSAGE_ZERO_CS = 2;

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
        if (!WIKIPEDIA_ZERO_DEV_MODE_ON || acquiringCarrierMessage) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (hostSupportsZeroHeaders(apiURL.getHost())) {
                    boolean hasZeroHeader = result.getHeaders().containsKey("X-CS");
                    if (hasZeroHeader) {
                        String xcs = result.getHeaders().get("X-CS").get(0);
                        if (!xcs.equals(carrierString)) {
                            identifyZeroCarrier(xcs);
                        }
                    } else if (zeroEnabled) {
                        zeroOff();
                    }
                }
            }
        });
    }

    /** For Retrofit responses */
    public void onHeaderCheck(final Response response) {
        if (!WIKIPEDIA_ZERO_DEV_MODE_ON || acquiringCarrierMessage) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (hostSupportsZeroHeaders(new URL(response.getUrl()).getHost())) {
                        String xcs = getHeader(response, "X-CS");
                        if (xcs != null) {
                            if (!xcs.equals(carrierString)) {
                                identifyZeroCarrier(xcs);
                            }
                        } else if (zeroEnabled) {
                            zeroOff();
                        }
                    }
                } catch (MalformedURLException e) {
                    throw new RuntimeException("interesting response", e);
                }
            }
        });
    }

    private void zeroOff() {
        carrierString = "";
        carrierMessage = null;
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

    private void identifyZeroCarrier(final String xcs) {
        Handler wikipediaZeroHandler = new Handler(new Handler.Callback() {
            private WikipediaZeroTask curZeroTask;

            @Override
            public boolean handleMessage(Message msg) {
                WikipediaZeroTask zeroTask = new WikipediaZeroTask(app.getAPIForSite(app.getSite()), app.getUserAgent()) {
                    @Override
                    public void onFinish(ZeroMessage message) {
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

    /**
     * Only subdomains of m.wikipedia.org have W0 headers, but there are other hosts,
     * like wikidata.org, that are also W0 rated.
     */
    private boolean hostSupportsZeroHeaders(String host) {
        return host.endsWith(".m.wikipedia.org");
    }
}