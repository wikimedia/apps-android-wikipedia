package org.wikipedia.zero;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.WikipediaZeroUsageFunnel;
import org.wikipedia.events.WikipediaZeroStateChangeEvent;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.OnHeaderCheckListener;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import okhttp3.Headers;
import retrofit2.Response;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.net.URL;

import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class WikipediaZeroHandler implements OnHeaderCheckListener {
    private static final int MESSAGE_ZERO_CS = 1;

    /**
     * Size of the text, in sp, of the Zero banner text.
     */
    private static final int BANNER_TEXT_SIZE = 12;
    /**
     * Height of the Zero banner, in pixels, that will pop up from the bottom of the screen.
     */
    private static final int BANNER_HEIGHT = (int) (24 * DimenUtil.getDensityScalar());

    @NonNull private WikipediaApp app;

    private boolean zeroEnabled = false;
    private volatile boolean acquiringCarrierMessage = false;
    @Nullable private ZeroConfig zeroConfig;
    @NonNull private WikipediaZeroUsageFunnel zeroFunnel;

    @NonNull private String zeroCarrierString = "";
    @NonNull private String zeroCarrierMetaString = "";

    // Tracks the X-Carrier header (if any) outside of the Zero state machine.
    // This is for reference in the rare cases where a user is potentially eligible
    // for zero-rating based on IP, only certain language wikis are whitelisted and
    // the user isn't visiting one of them.
    @NonNull private String xCarrier = "";

    public WikipediaZeroHandler(@NonNull WikipediaApp app) {
        this.app = app;
        this.zeroFunnel = new WikipediaZeroUsageFunnel(app, "", "");
    }

    public boolean isZeroEnabled() {
        return zeroEnabled;
    }

    @Nullable
    public ZeroConfig getZeroConfig() {
        return zeroConfig;
    }

    @NonNull
    public WikipediaZeroUsageFunnel getZeroFunnel() {
        return zeroFunnel;
    }

    public String getXCarrier() {
        return xCarrier;
    }

    public void showZeroBanner(@NonNull final Activity activity, @NonNull ZeroConfig zeroConfig) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(activity,
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

    public void showZeroOffBanner(@NonNull final Activity activity, String message, int background,
                                  int foreground) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(activity,
                message, FeedbackUtil.LENGTH_DEFAULT);
        show(snackbar, background, foreground);
    }

    public static void showZeroExitInterstitialDialog(@NonNull final Context context,
                                                      @NonNull final Uri uri) {
        final WikipediaZeroHandler zeroHandler = WikipediaApp.getInstance()
                .getWikipediaZeroHandler();

        final ZeroConfig zeroConfig = zeroHandler.getZeroConfig();

        if (zeroConfig == null) {
            return;
        }

        final String customExitTitle = zeroConfig.getExitTitle();
        final String customExitWarning = zeroConfig.getExitWarning();
        final String customPartnerInfoText = zeroConfig.getPartnerInfoText();
        final String customPartnerInfoUrl = zeroConfig.getPartnerInfoUrl();

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(!(StringUtil.emptyIfNull(customExitTitle).equals("")) ? customExitTitle
                : context.getString(R.string.zero_interstitial_title));
        alert.setMessage(!(StringUtil.emptyIfNull(customExitWarning).equals("")) ? customExitWarning
                : context.getString(R.string.zero_interstitial_leave_app));
        alert.setPositiveButton(context.getString(R.string.zero_interstitial_continue),
                new DialogInterface.OnClickListener() {
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
        alert.setNegativeButton(context.getString(R.string.zero_interstitial_cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                zeroHandler.getZeroFunnel().logExtLinkBack();
            }
        });
        AlertDialog ad = alert.create();
        ad.show();
        zeroHandler.getZeroFunnel().logExtLinkWarn();
    }

    /** For MW API responses */
    // Note: keep in sync with next method. This one will go away when we've retrofitted all
    // API calls.
    @Override
    public void onHeaderCheck(@NonNull final ApiResult result, @NonNull final URL apiURL) {
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
                    if (eitherChanged(xCarrierFromHeader, xCarrierMetaFromHeader)) {
                        identifyZeroCarrier(xCarrierFromHeader, xCarrierMetaFromHeader);
                    }
                } else if (zeroEnabled) {
                    zeroOff();
                }
            }
        });
    }

    /** For Retrofit responses */
    public void onHeaderCheck(@NonNull final Response<?> response) {
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
                        if (eitherChanged(xCarrierFromHeader, xCarrierMetaFromHeader)) {
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

    private boolean eitherChanged(String xCarrier, String xCarrierMeta) {
        return !(xCarrier.equals(zeroCarrierString) && xCarrierMeta.equals(zeroCarrierMetaString));
    }

    private void show(@NonNull Snackbar snackbar, int background, int foreground) {
        ViewGroup rootView = (ViewGroup) snackbar.getView();
        TextView textView = (TextView) rootView.findViewById(R.id.snackbar_text);
        rootView.setBackgroundColor(background);
        rootView.setMinimumHeight(BANNER_HEIGHT);
        textView.setTextColor(foreground);
        textView.setTextSize(BANNER_TEXT_SIZE);
        if (ApiUtil.hasJellyBeanMr1()) {
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        snackbar.show();
    }

    private void identifyZeroCarrier(@NonNull final String xCarrierFromHeader,
                                     @NonNull final String xCarrierMetaFromHeader) {
        Handler wikipediaZeroHandler = new Handler(new Handler.Callback() {
            private WikipediaZeroTask curZeroTask;

            @Override
            public boolean handleMessage(Message msg) {
                WikipediaZeroTask zeroTask = new WikipediaZeroTask(
                        app.getApiForMobileSite(app.getSite()), app.getUserAgent()) {
                    @Override
                    public void onFinish(ZeroConfig config) {
                        L.i("New Wikipedia Zero config: " + config);
                        xCarrier = xCarrierFromHeader; // ex. "123-45"

                        if (config != null) {
                            zeroCarrierString = xCarrierFromHeader;
                            zeroCarrierMetaString = xCarrierMetaFromHeader; // ex. "wap"; default ""
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
                        L.w("Wikipedia Zero eligibility check failed", caught);
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

    @Nullable
    private String getHeader(@NonNull Response<?> response, @NonNull String key) {
        Headers headers = response.headers();
        for (String name: headers.names()) {
            if (key.equalsIgnoreCase(name)) {
                return headers.get(name);
            }
        }
        return null;
    }
}