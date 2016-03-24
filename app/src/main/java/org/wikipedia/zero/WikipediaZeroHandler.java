package org.wikipedia.zero;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.WikipediaZeroUsageFunnel;
import org.wikipedia.events.WikipediaZeroStateChangeEvent;
import org.wikipedia.random.RandomArticleIdTask;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.OnHeaderCheckListener;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import retrofit.client.Header;
import retrofit.client.Response;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.net.URL;
import java.util.List;

import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class WikipediaZeroHandler extends BroadcastReceiver implements OnHeaderCheckListener {
    private static final int MESSAGE_ZERO_RND = 1;
    private static final int MESSAGE_ZERO_CS = 2;

    /**
     * Size of the text, in sp, of the Zero banner text.
     */
    private static final int BANNER_TEXT_SIZE = 16;
    /**
     * Height of the Zero banner, in pixels, that will pop up from the bottom of the screen.
     */
    private static final int BANNER_HEIGHT = (int) (120 * DimenUtil.getDensityScalar());

    private WikipediaApp app;

    private boolean zeroEnabled = false;
    private volatile boolean acquiringCarrierMessage = false;
    private ZeroConfig zeroConfig;
    private WikipediaZeroUsageFunnel zeroFunnel;

    private String zeroCarrierString = "";
    private String zeroCarrierMetaString = "";

    // Tracks the X-Carrier header (if any) outside of the Zero state machine.
    // This is for reference in the rare cases where a user is potentially eligible
    // for zero-rating based on IP, only certain language wikis are whitelisted and
    // the user isn't visiting one of them.
    private String xCarrier = "";

    private RandomArticleIdTask curRandomArticleIdTask;

    public WikipediaZeroHandler(WikipediaApp app) {
        this.app = app;
        IntentFilter connFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        app.registerReceiver(this, connFilter);
        this.zeroFunnel = new WikipediaZeroUsageFunnel(app, "", "");
    }

    public boolean isZeroEnabled() {
        return zeroEnabled;
    }

    public ZeroConfig getZeroConfig() {
        return zeroConfig;
    }

    public WikipediaZeroUsageFunnel getZeroFunnel() {
        return zeroFunnel;
    }

    public String getXCarrier() {
        return xCarrier;
    }

    public void showZeroBanner(@NonNull final Activity activity, @NonNull ZeroConfig zeroConfig) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(activity.getWindow().getDecorView(),
                zeroConfig.getMessage(), FeedbackUtil.LENGTH_DEFAULT);
        final String zeroBannerUrl = zeroConfig.getBannerUrl();
        if (!StringUtil.emptyIfNull(zeroBannerUrl).equals("")) {
            snackbar.setAction("Info", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UriUtil.visitInExternalBrowser(activity, Uri.parse(zeroBannerUrl));
                    zeroFunnel.logBannerClick();
                }
            });
        }
        show(snackbar, zeroConfig.getBackground(), zeroConfig.getForeground());
    }

    public void showZeroOffBanner(@NonNull final Activity activity, String message, int background, int foreground) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(activity.getWindow().getDecorView(),
                message, FeedbackUtil.LENGTH_DEFAULT);
        show(snackbar, background, foreground);
    }

    private void show(Snackbar snackbar, int background, int foreground) {
        ViewGroup rootView = (ViewGroup) snackbar.getView();
        TextView textView = (TextView) rootView.findViewById(R.id.snackbar_text);
        rootView.setBackgroundColor(background);
        textView.setTextColor(foreground);
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
                    String xCarrierFromHeader = result.getHeaders().get("X-Carrier").get(0);
                    String xCarrierMetaFromHeader = "";
                    if (result.getHeaders().containsKey("X-Carrier-Meta")) {
                        xCarrierMetaFromHeader = result.getHeaders().get("X-Carrier-Meta").get(0);
                    }
                    if (!(xCarrierFromHeader.equals(zeroCarrierString) && xCarrierMetaFromHeader.equals(zeroCarrierMetaString))) {
                        identifyZeroCarrier(xCarrierFromHeader, xCarrierMetaFromHeader);
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
                    String xCarrierFromHeader = getHeader(response, "X-Carrier");
                    String xCarrierMetaFromHeader = "";
                    if (getHeader(response, "X-Carrier-Meta") != null) {
                        xCarrierMetaFromHeader = getHeader(response, "X-Carrier-Meta");
                    }
                    if (xCarrierFromHeader != null) {
                        if (!(xCarrierFromHeader.equals(zeroCarrierString) && xCarrierMetaFromHeader.equals(zeroCarrierMetaString))) {
                            identifyZeroCarrier(xCarrierFromHeader, xCarrierMetaFromHeader);
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

    public static void showZeroExitInterstitialDialog(final Context context, final Uri uri) {
        final WikipediaZeroHandler zeroHandler = WikipediaApp.getInstance()
                .getWikipediaZeroHandler();

        final ZeroConfig zeroConfig = zeroHandler.getZeroConfig();
        final String customExitTitle = zeroConfig.getExitTitle();
        final String customExitWarning = zeroConfig.getExitWarning();
        final String customPartnerInfoText = zeroConfig.getPartnerInfoText();
        final String customPartnerInfoUrl = zeroConfig.getPartnerInfoUrl();

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(!(StringUtil.emptyIfNull(customExitTitle).equals("")) ? customExitTitle
                : context.getString(R.string.zero_interstitial_title));
        alert.setMessage(!(StringUtil.emptyIfNull(customExitWarning).equals("")) ? customExitWarning
                : context.getString(R.string.zero_interstitial_leave_app));
        alert.setPositiveButton(context.getString(R.string.zero_interstitial_continue), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                visitInExternalBrowser(context, uri);
                zeroHandler.getZeroFunnel().logExtLinkConf();
            }
        });
        if (customPartnerInfoText != null && customPartnerInfoUrl != null) {
            alert.setNeutralButton(customPartnerInfoText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    visitInExternalBrowser(context, Uri.parse(customPartnerInfoUrl));
                }
            });
        }
        alert.setNegativeButton(context.getString(R.string.zero_interstitial_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                zeroHandler.getZeroFunnel().logExtLinkBack();
            }
        });
        AlertDialog ad = alert.create();
        ad.show();
        zeroHandler.getZeroFunnel().logExtLinkWarn();
    }

    @Override
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

    private void identifyZeroCarrier(final String xCarrierFromHeader, final String xCarrierMetaFromHeader) {
        Handler wikipediaZeroHandler = new Handler(new Handler.Callback() {
            private WikipediaZeroTask curZeroTask;

            @Override
            public boolean handleMessage(Message msg) {
                WikipediaZeroTask zeroTask = new WikipediaZeroTask(app.getApiForMobileSite(app.getSite()), app.getUserAgent()) {
                    @Override
                    public void onFinish(ZeroConfig config) {
                        L.d("New Wikipedia Zero config: " + config);
                        xCarrier = xCarrierFromHeader; // ex. "123-45"

                        if (config != null) {
                            zeroCarrierString = xCarrierFromHeader;
                            zeroCarrierMetaString = xCarrierMetaFromHeader; // ex. "wap"; usually empty (the default case)
                                zeroConfig = config;
                            zeroEnabled = true;
                            zeroFunnel = new WikipediaZeroUsageFunnel(app, zeroCarrierString,
                                    StringUtil.emptyIfNull(zeroCarrierMetaString));
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

    private void zeroOff() {
        zeroCarrierString = "";
        zeroCarrierMetaString = "";
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
}