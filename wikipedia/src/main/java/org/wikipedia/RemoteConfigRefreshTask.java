package org.wikipedia;

import android.content.*;
import android.util.*;
import com.github.kevinsawicki.http.*;
import org.json.*;
import org.wikipedia.concurrency.*;
import org.wikipedia.recurring.*;

import java.util.*;

public class RemoteConfigRefreshTask extends RecurringTask {
    // Switch over to production when it is available
    private static final java.lang.String REMOTE_CONFIG_URL = "https://bits.wikimedia.org/static-1.24wmf1/extensions/MobileApp/config/android.json";

    // The 'l' suffix is needed because stupid Java overflows constants otherwise
    private static final long RUN_INTERVAL_MILLI = 24L * 60L * 60L * 1000L; // Once a day!

    public RemoteConfigRefreshTask(Context context) {
        super(context);
    }

    @Override
    protected boolean shouldRun(Date lastRun) {
        return System.currentTimeMillis() - lastRun.getTime() >= RUN_INTERVAL_MILLI;
    }

    @Override
    protected void run(Date lastRun) {
        new SaneAsyncTask<Boolean>(1) {
            @Override
            public Boolean performTask() throws Throwable {
                WikipediaApp app = (WikipediaApp) context.getApplicationContext();
                JSONObject config = new JSONObject(HttpRequest.get(REMOTE_CONFIG_URL).body());
                app.getRemoteConfig().updateConfig(config);
                Log.d("Wikipedia", config.toString());
                return true;
            }

            @Override
            public void onCatch(Throwable caught) {
                // Don't do anything, but do write out a log statement. We don't particularly care.
                Log.d("Wikipedia", caught.toString());
            }
        }.execute();
    }

    @Override
    protected String getName() {
        return "remote-config-refresher";
    }
}
