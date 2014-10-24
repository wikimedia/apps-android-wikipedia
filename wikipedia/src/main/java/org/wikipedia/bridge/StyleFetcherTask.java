package org.wikipedia.bridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.github.kevinsawicki.http.HttpRequest;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.recurring.RecurringTask;
import org.wikipedia.settings.PrefKeys;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

public class StyleFetcherTask extends RecurringTask {

    private static final String[][] STYLE_SPECS = {
            {StyleLoader.BUNDLE_PAGEVIEW, "mobile.app.pagestyles.android"},
            {StyleLoader.BUNDLE_PREVIEW, "mobile.app.preview"},
            {StyleLoader.BUNDLE_ABUSEFILTER, "mobile.app.pagestyles.android"},
            {StyleLoader.BUNDLE_NIGHT_MODE, "mobile.app.pagestyles.android.night"}
    };

    // The 'l' suffix is needed because stupid Java overflows constants otherwise
    private static final long RUN_INTERVAL_MILLI =  24L * 60L * 60L * 1000L;

    public StyleFetcherTask(Context context) {
        super(context);
    }

    @Override
    protected boolean shouldRun(Date lastRun) {
        return System.currentTimeMillis() - lastRun.getTime() >= RUN_INTERVAL_MILLI;
    }

    private String getRemoteURLFor(String modules) {
        Site site = ((WikipediaApp) getContext().getApplicationContext()).getPrimarySite();
        try {
            return String.format(
                    "https://bits.wikimedia.org/%s.wikipedia.org/load.php?debug=false&lang=en&modules=%s&only=styles&skin=vector",
                    site.getLanguage(),
                    URLEncoder.encode(modules, "utf-8")
            );
        } catch (UnsupportedEncodingException e) {
            // This does not happen.
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void run(Date lastRun) {
        WikipediaApp app = (WikipediaApp) getContext().getApplicationContext();
        try {
            for (String[] styleSpec : STYLE_SPECS) {
                String url = getRemoteURLFor(styleSpec[1]);

                HttpRequest request = HttpRequest.get(url).userAgent(app.getUserAgent());
                // Only overwrite files if we get a 200
                // This prevents empty style files from being used when betalabs goes down
                if (request.ok()) {
                    OutputStream fo = getContext().openFileOutput(styleSpec[0], Context.MODE_PRIVATE);
                    Utils.copyStreams(request.stream(), fo);
                    fo.close();
                    Log.d("Wikipedia", String.format("Downloaded %s into %s", url, getContext().getFileStreamPath(styleSpec[0]).getAbsolutePath()));
                } else {
                    Log.d("Wikipedia", String.format("Failed to download %s into %s", url, getContext().getFileStreamPath(styleSpec[0]).getAbsolutePath()));
                    return;
                }
            }

            //if any of the above code throws an exception, the following last-updated date will not
            //be updated, so the task will be retried on the next go.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            prefs.edit().putString(PrefKeys.getStylesLastUpdated(), Utils.formatISO8601(new Date())).commit();

        } catch (Exception e) {
            // Could be one of several exceptions, but it doesn't matter too much, since
            // the task will be retried later.
            Log.d("StyleFetcherTask", "Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected String getName() {
        return "style-fetcher";
    }
}
