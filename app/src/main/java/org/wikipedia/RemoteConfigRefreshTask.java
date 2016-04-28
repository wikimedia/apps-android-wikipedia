package org.wikipedia;

import android.support.annotation.NonNull;
import android.util.Log;
import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONObject;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.recurring.RecurringTask;
import org.wikipedia.settings.RbSwitch;

import java.util.Date;

public class RemoteConfigRefreshTask extends RecurringTask {
    // Switch over to production when it is available
    private static final java.lang.String REMOTE_CONFIG_URL = "https://meta.wikimedia.org/static/current/extensions/MobileApp/config/android.json";

    // The 'l' suffix is needed because stupid Java overflows constants otherwise
    private static final long RUN_INTERVAL_MILLI = 24L * 60L * 60L * 1000L; // Once a day!

    @NonNull private final WikipediaApp app;

    public RemoteConfigRefreshTask(@NonNull WikipediaApp app) {
        this.app = app;
    }

    @Override
    protected boolean shouldRun(Date lastRun) {
        return System.currentTimeMillis() - lastRun.getTime() >= RUN_INTERVAL_MILLI;
    }

    @Override
    protected void run(Date lastRun) {
        new SaneAsyncTask<Boolean>() {
            @Override
            public Boolean performTask() throws Throwable {
                JSONObject config = new JSONObject(HttpRequest.get(REMOTE_CONFIG_URL).body());
                app.getRemoteConfig().updateConfig(config);
                RbSwitch.INSTANCE.update();
                Log.d("Wikipedia", config.toString());
                return true;
            }

            @Override
            public void onCatch(Throwable caught) {
                // Don't do anything, but do write out a log statement. We don't particularly care.
                Log.d("Wikipedia", "Caught " + caught.toString());
            }
        }.execute();
    }

    @Override
    protected String getName() {
        return "remote-config-refresher";
    }
}
