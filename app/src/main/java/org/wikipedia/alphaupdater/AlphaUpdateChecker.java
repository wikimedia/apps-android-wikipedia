package org.wikipedia.alphaupdater;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.wikipedia.R;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.recurring.RecurringTask;
import org.wikipedia.settings.PrefsIoUtil;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Request;
import okhttp3.Response;

public class AlphaUpdateChecker extends RecurringTask {
    private static final long RUN_INTERVAL_MILLI = TimeUnit.DAYS.toMillis(1);

    private static final String PREFERENCE_KEY_ALPHA_COMMIT = "alpha_last_checked_commit";
    private static final String ALPHA_BUILD_APK_URL = "https://github.com/wikimedia/apps-android-wikipedia/releases/download/latest/app-alpha-universal-release.apk";
    private static final String ALPHA_BUILD_DATA_URL = "https://github.com/wikimedia/apps-android-wikipedia/releases/download/latest/rev-hash.txt";
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
        String hashString;
        Response response = null;
        try {
            Request request = new Request.Builder().url(ALPHA_BUILD_DATA_URL).build();
            response = OkHttpConnectionFactory.getClient().newCall(request).execute();
            hashString = response.body().string();
        } catch (IOException e) {
            // It's ok, we can do nothing.
            return;
        } finally {
            if (response != null) {
                response.close();
            }
        }
        if (!PrefsIoUtil.getString(PREFERENCE_KEY_ALPHA_COMMIT, "").equals(hashString)) {
            showNotification();
        }
        PrefsIoUtil.setString(PREFERENCE_KEY_ALPHA_COMMIT, hashString);
    }

    private void showNotification() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(ALPHA_BUILD_APK_URL));
        PendingIntent pintent = PendingIntent.getActivity(context, 0, intent, 0);
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        // Notification channel ( >= API 26 )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "Alpha updates", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.alpha_update_notification_title))
                .setContentText(context.getString(R.string.alpha_update_notification_text))
                .setContentIntent(pintent)
                .setAutoCancel(true);

        notificationBuilder.setSmallIcon(R.drawable.ic_w_transparent);

        notificationManager.notify(1, notificationBuilder.build());
    }

    @Override
    protected String getName() {
        return "alpha-update-checker";
    }
}
