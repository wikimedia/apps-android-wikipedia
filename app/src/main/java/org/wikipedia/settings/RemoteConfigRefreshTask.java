package org.wikipedia.settings;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.recurring.RecurringTask;
import org.wikipedia.util.log.L;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Request;
import okhttp3.Response;

public class RemoteConfigRefreshTask extends RecurringTask {
    // Switch over to production when it is available
    private static final java.lang.String REMOTE_CONFIG_URL = "https://meta.wikimedia.org/static/current/extensions/MobileApp/config/android.json";

    private static final long RUN_INTERVAL_MILLI = TimeUnit.DAYS.toMillis(1);

    @Override
    protected boolean shouldRun(Date lastRun) {
        return System.currentTimeMillis() - lastRun.getTime() >= RUN_INTERVAL_MILLI;
    }

    @Override
    protected void run(Date lastRun) {
        new SaneAsyncTask<Boolean>() {
            @Override
            public Boolean performTask() throws Throwable {
                Request request = new Request.Builder().url(REMOTE_CONFIG_URL).build();
                Response response = OkHttpConnectionFactory.getClient().newCall(request).execute();
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
