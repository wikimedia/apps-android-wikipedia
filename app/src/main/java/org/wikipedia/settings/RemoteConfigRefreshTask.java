package org.wikipedia.settings;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
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
        Response response = null;
        try {
            Request request = new Request.Builder().url(REMOTE_CONFIG_URL).build();
            response = OkHttpConnectionFactory.getClient().newCall(request).execute();
            JSONObject config = new JSONObject(response.body().string());
            WikipediaApp.getInstance().getRemoteConfig().updateConfig(config);
            RbSwitch.INSTANCE.update();
            L.d(config.toString());
        } catch (Exception e) {
            L.e(e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    protected String getName() {
        return "remote-config-refresher";
    }
}
