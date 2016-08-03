package org.wikipedia;

import org.json.JSONObject;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.recurring.RecurringTask;
import org.wikipedia.settings.RbSwitch;
import org.wikipedia.util.log.L;

import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RemoteConfigRefreshTask extends RecurringTask {
    // Switch over to production when it is available
    private static final java.lang.String REMOTE_CONFIG_URL = "https://meta.wikimedia.org/static/current/extensions/MobileApp/config/android.json";

    // The 'l' suffix is needed because stupid Java overflows constants otherwise
    private static final long RUN_INTERVAL_MILLI = 24L * 60L * 60L * 1000L; // Once a day!

    @Override
    protected boolean shouldRun(Date lastRun) {
        return System.currentTimeMillis() - lastRun.getTime() >= RUN_INTERVAL_MILLI;
    }

    @Override
    protected void run(Date lastRun) {
        new SaneAsyncTask<Boolean>() {
            @Override
            public Boolean performTask() throws Throwable {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(REMOTE_CONFIG_URL).build();
                Response response = client.newCall(request).execute();
                try {
                    JSONObject config = new JSONObject(response.body().string());
                    WikipediaApp.getInstance().getRemoteConfig().updateConfig(config);
                    RbSwitch.INSTANCE.update();
                    L.d(config.toString());
                    return true;
                } finally {
                    response.close();
                }
            }

            @Override
            public void onCatch(Throwable caught) {
                // Don't do anything, but do write out a log statement. We don't particularly care.
                L.d("Caught " + caught.toString());
            }
        }.execute();
    }

    @Override
    protected String getName() {
        return "remote-config-refresher";
    }
}
