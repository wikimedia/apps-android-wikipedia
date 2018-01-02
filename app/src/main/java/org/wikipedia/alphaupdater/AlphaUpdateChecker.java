package org.wikipedia.alphaupdater;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.recurring.RecurringTask;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Request;
import okhttp3.Response;

public class AlphaUpdateChecker extends RecurringTask {
    private static final long RUN_INTERVAL_MILLI = TimeUnit.DAYS.toMillis(1);

    private static final String PREFERENCE_KEY_ALPHA_COMMIT = "alpha_last_checked_commit";
    private static final String ALPHA_BUILD_APK_URL = "https://android-builds.wmflabs.org/runs/latest/wikipedia.apk";
    private static final String ALPHA_BUILD_DATA_URL = "https://android-builds.wmflabs.org/runs/latest/meta.json";
    private static final String CHANNEL_ID = "ALPHA_UPDATE_CHECKER_CHANNEL";
    @NonNull private final Context context;

    public AlphaUpdateChecker(@NonNull Context context) {
        this.context = context;
    }

    @Override
    protected boolean shouldRun(Date lastRun) {
        return System.currentTimeMillis() - lastRun.getTime() >= RUN_INTERVAL_MILLI;
    }

    @Override
    protected void run(Date lastRun) {
        // Check for updates!
        JSONObject config;
        Response response = null;
        try {
            Request request = new Request.Builder().url(ALPHA_BUILD_DATA_URL).build();
            response = OkHttpConnectionFactory.getClient().newCall(request).execute();
            config = new JSONObject(response.body().string());
        } catch (IOException | JSONException e) {
            // It's ok, we can do nothing.
            return;
        } finally {
            if (response != null) {
                response.close();
            }
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.contains(PREFERENCE_KEY_ALPHA_COMMIT)) {
            if (!prefs.getString(PREFERENCE_KEY_ALPHA_COMMIT, "").equals(config.optString("commit_hash", ""))) {
                showNotification();
            }
        }

        prefs.edit().putString(PREFERENCE_KEY_ALPHA_COMMIT, config.optString("commit_hash")).apply();
    }

    private void showNotification() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(ALPHA_BUILD_APK_URL));
        PendingIntent pintent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.alpha_update_notification_title))
                .setContentText(context.getString(R.string.alpha_update_notification_text))
                .setContentIntent(pintent)
                .setAutoCancel(true);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setSmallIcon(R.drawable.ic_w_transparent);
        } else {
            notificationBuilder.setSmallIcon(R.mipmap.launcher);
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, notificationBuilder.build());
    }

    @Override
    protected String getName() {
        return "alpha-update-checker";
    }
}
