package org.wikipedia.crash;

import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseCrashReporter implements CrashReporter {
    @NonNull private final Map<String, String> props = new HashMap<>();

    /**
     * HockeyApp doesn't seem to offer custom properties, so these are bundled as JSON in the report
     * description. Since these properties are not associated with a crash instance and not
     * preserved across application death, and crashes may enqueue, it's possible they may be
     * inaccurate. However, these properties are used in one place presently and the current
     * implementation should be adequate.
     */
    @Override
    public BaseCrashReporter putReportProperty(String key, String value) {
        getProps().put(key, value);
        return this;
    }

    protected String getPropsJson() {
        return new JSONObject(getProps()).toString();
    }

    @NonNull protected Map<String, String> getProps() {
        return props;
    }
}
