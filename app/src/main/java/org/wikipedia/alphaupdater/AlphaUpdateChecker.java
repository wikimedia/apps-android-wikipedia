package org.wikipedia.alphaupdater;

import org.wikipedia.R;
import org.wikipedia.recurring.RecurringTask;
import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import java.util.Date;

public class AlphaUpdateChecker extends RecurringTask {
    private static final long RUN_INTERVAL_MILLI = 24L * 60L * 60L * 1000L; // Once a day!

    private static final String PREFERENCE_KEY_ALPHA_COMMIT = "alpha_last_checked_commit";
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
        String responseJSON;
        try {
            HttpRequest request = HttpRequest.get("https://android-builds.wmflabs.org/runs/latest/meta.json");
            responseJSON = request.body();
        } catch (HttpRequest.HttpRequestException | SecurityException e) {
            // It's ok, we can do nothing.
            return;
        }
        JSONObject meta;
        try {
            meta = new JSONObject(responseJSON);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.contains(PREFERENCE_KEY_ALPHA_COMMIT)) {
            if (!prefs.getString(PREFERENCE_KEY_ALPHA_COMMIT, "").equals(meta.optString("commit_hash", ""))) {
                showNotification();
            }
        }

        prefs.edit().putString(PREFERENCE_KEY_ALPHA_COMMIT, meta.optString("commit_hash")).apply();
    }

    private void showNotification() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://android-builds.wmflabs.org/runs/latest/wikipedia.apk"));
        PendingIntent pintent = PendingIntent.getActivity(context, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.alpha_update_notification_title))
                .setContentText(context.getString(R.string.alpha_update_notification_text))
                .setContentIntent(pintent)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, notification);
    }

    @Override
    protected String getName() {
        return "alpha-update-checker";
    }
}
