package org.wikipedia.zero;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.WikipediaZeroUsageFunnel;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.events.WikipediaZeroEnterEvent;
import org.wikipedia.main.MainActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

import okhttp3.Headers;
import retrofit2.Call;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class WikipediaZeroHandler {
    private static final String CHANNEL_ID = "WIKIPEDIA_ZERO_CHANNEL";
    private static final int NOTIFICATION_ID = 100;
    private static final int MESSAGE_ZERO_CS = 1;

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
        final Uri customPartnerInfoUrl = zeroConfig.getPartnerInfoUrl();

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(!StringUtils.isEmpty(customExitTitle) ? customExitTitle
                : context.getString(R.string.zero_interstitial_title));
        alert.setMessage(!StringUtils.isEmpty(customExitWarning) ? customExitWarning
                : context.getString(R.string.zero_interstitial_leave_app));
        alert.setPositiveButton(context.getString(R.string.zero_interstitial_continue), (DialogInterface dialog, int id) -> {
            visitInExternalBrowser(context, uri);
            zeroHandler.getZeroFunnel().logExtLinkConf();
        });
        if (customPartnerInfoText != null && customPartnerInfoUrl != null) {
            alert.setNeutralButton(customPartnerInfoText, (DialogInterface dialog, int which) -> {
                    visitInExternalBrowser(context, customPartnerInfoUrl);
            });
        }
        alert.setNegativeButton(context.getString(R.string.zero_interstitial_cancel), (DialogInterface dialog, int id) -> {
            zeroHandler.getZeroFunnel().logExtLinkBack();
        });
        AlertDialog ad = alert.create();
        ad.show();
        zeroHandler.getZeroFunnel().logExtLinkWarn();
    }

    public void onHeaderCheck(@NonNull Headers headers) {
        if (acquiringCarrierMessage) {
            return;
        }

        final String xCarrierFromHeader = getHeader(headers, "X-Carrier");
        final String xCarrierMetaFromHeader = StringUtils.defaultString(getHeader(headers,
                "X-Carrier-Meta"));
        // newHeader may be true but must still be compared against SharedPreferences
        final boolean newHeader = xCarrierFromHeader != null
                && eitherChanged(xCarrierFromHeader, xCarrierMetaFromHeader);
        boolean transitioningOff = zeroEnabled && xCarrierFromHeader == null;

        if (!newHeader && !transitioningOff) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (newHeader) {
                    identifyZeroCarrier(xCarrierFromHeader, xCarrierMetaFromHeader);
                } else {
                    zeroOff();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void showZeroTutorialDialog(@NonNull Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.zero_wikipedia_zero_heading)
                .setMessage(R.string.zero_learn_more)
                .setPositiveButton(R.string.zero_learn_more_dismiss, null)
                .setNegativeButton(R.string.zero_learn_more_learn_more, (DialogInterface dialog, int id) -> {
                            visitInExternalBrowser(app,
                                    (Uri.parse(app.getString(R.string.zero_webpage_url))));
                            zeroFunnel.logExtLinkMore();
                        }).create().show();
    }

    private boolean eitherChanged(String xCarrier, String xCarrierMeta) {
        return !(xCarrier.equals(zeroCarrierString) && xCarrierMeta.equals(zeroCarrierMetaString));
    }

    private void identifyZeroCarrier(@NonNull final String xCarrierFromHeader,
                                     @NonNull final String xCarrierMetaFromHeader) {
        Handler wikipediaZeroHandler = new Handler((Message msg) -> {
            new ZeroConfigClient().request(new WikiSite(app.getWikiSite().mobileAuthority()),
                    app.getUserAgent(), new ZeroConfigClient.Callback() {
                @Override
                public void success(@NonNull Call<ZeroConfig> call, @NonNull ZeroConfig config) {
                    L.i("New Wikipedia Zero config: " + config);

                    if (!config.isEligible()) {
                        acquiringCarrierMessage = false;
                        return;
                    }

                    xCarrier = xCarrierFromHeader; // ex. "123-45"
                    zeroCarrierString = xCarrierFromHeader;
                    zeroCarrierMetaString = xCarrierMetaFromHeader; // ex. "wap"; default ""
                    zeroConfig = config;
                    zeroEnabled = true;
                    zeroFunnel = new WikipediaZeroUsageFunnel(app, zeroCarrierString,
                            defaultString(zeroCarrierMetaString));
                    app.getBus().post(new WikipediaZeroEnterEvent());
                    if (zeroConfig.hashCode() != Prefs.zeroConfigHashCode()) {
                        notifyEnterZeroNetwork(app, zeroConfig);
                    }
                    Prefs.zeroConfigHashCode(zeroConfig.hashCode());
                    acquiringCarrierMessage = false;
                }

                @Override
                public void failure(@NonNull Call<ZeroConfig> call, @NonNull Throwable caught) {
                    L.w("Wikipedia Zero eligibility check failed", caught);
                    acquiringCarrierMessage = false;
                }
            });
            acquiringCarrierMessage = true;
            return true;
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
        Prefs.zeroConfigHashCode(0);
        notifyExitZeroNetwork(app);
    }

    @Nullable
    private String getHeader(@NonNull Headers headers, @NonNull String key) {
        for (String name: headers.names()) {
            if (key.equalsIgnoreCase(name)) {
                return headers.get(name);
            }
        }
        return null;
    }

    private void notifyEnterZeroNetwork(@NonNull Context context, @NonNull ZeroConfig config) {
        NotificationCompat.Builder builder = createNotification(context);
        builder.setColor(config.getBackground())
                .setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        ? R.drawable.ic_wikipedia_zero_on : R.mipmap.launcher)
                .setLights(config.getBackground(),
                        context.getResources().getInteger(R.integer.zero_notification_light_on_ms),
                        context.getResources().getInteger(R.integer.zero_notification_light_off_ms))
                .setContentText(context.getString(R.string.zero_learn_more))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.zero_learn_more)))
                .addAction(0, context.getString(R.string.zero_learn_more_learn_more),
                        pendingIntentForUrl(context, context.getString(R.string.zero_webpage_url)));
        showNotification(context, builder.build());
    }

    private void notifyExitZeroNetwork(@NonNull Context context) {
        NotificationCompat.Builder builder = createNotification(context);
        builder.setColor(ContextCompat.getColor(context, R.color.red50))
                .setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        ? R.drawable.ic_wikipedia_zero_off : R.mipmap.launcher)
                .setContentText(context.getString(R.string.zero_charged_verbiage))
                .setAutoCancel(true)
                .addAction(0, context.getString(R.string.zero_learn_more_learn_more),
                        pendingIntentForUrl(context, context.getString(R.string.zero_webpage_url)));
        showNotification(context, builder.build());
    }

    private NotificationCompat.Builder createNotification(@NonNull Context context) {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentTitle(context.getString(R.string.zero_wikipedia_zero_heading))
                .setContentIntent(PendingIntent
                        .getActivity(context, 0, new Intent(context, MainActivity.class), 0));
    }

    private void showNotification(@NonNull Context context, @NonNull Notification notification) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);
    }

    private PendingIntent pendingIntentForUrl(@NonNull Context context, @NonNull String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
