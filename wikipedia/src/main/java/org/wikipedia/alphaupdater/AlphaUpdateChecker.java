package org.wikipedia.alphaupdater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.recurring.RecurringTask;

import java.util.Date;

public class AlphaUpdateChecker extends RecurringTask {
    // The 'l' suffix is needed because stupid Java overflows constants otherwise
    private static final long RUN_INTERVAL_MILLI = 24L * 60L * 60L * 1000L; // Once a day!

    private static final String PREFERENCE_KEY_ALPHA_COMMIT = "alpha_last_checked_commit";
    public AlphaUpdateChecker(Context context) {
        super(context);
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
            HttpRequest request = HttpRequest.get("https://tools.wmflabs.org/wikipedia-android-builds/runs/latest/meta.json");
            responseJSON = request.body();
        } catch (HttpRequest.HttpRequestException e) {
            // It's ok, we can do nothing.
            return;
        }
        JSONObject meta;
        try {
            meta = new JSONObject(responseJSON);
        } catch (JSONException e) {
            // This never happens
            throw new RuntimeException(e);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (prefs.contains(PREFERENCE_KEY_ALPHA_COMMIT)) {
            if (!prefs.getString(PREFERENCE_KEY_ALPHA_COMMIT, "").equals(meta.optString("commit_hash", ""))) {
                showNotification();
            }
        }

        prefs.edit().putString(PREFERENCE_KEY_ALPHA_COMMIT, meta.optString("commit_hash")).commit();
    }

    private void showNotification() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tools.wmflabs.org/wikipedia-android-builds/runs/latest/wikipedia.apk"));
        PendingIntent pintent = PendingIntent.getActivity(getContext(), 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(getContext()).setSmallIcon(R.drawable.launcher_alpha)
                .setContentTitle(getContext().getString(R.string.alpha_update_notification_title))
                .setContentText(getContext().getString(R.string.alpha_update_notification_text))
                .setContentIntent(pintent)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, notification);
    }

    @Override
    protected String getName() {
        return "alpha-update-checker";
    }
}
