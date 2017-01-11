package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.ReleaseUtil;

// https://meta.wikimedia.org/wiki/Schema:WikipediaZeroUsage
public class WikipediaZeroUsageFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "WikipediaZeroUsage";
    private static final int REV_ID = 14574251;

    private final String xcs;
    private final String net;

    public WikipediaZeroUsageFunnel(WikipediaApp app, String xcs, String net) {
        super(app, SCHEMA_NAME, REV_ID, ReleaseUtil.isProdRelease() ? Funnel.SAMPLE_LOG_100 : Funnel.SAMPLE_LOG_ALL);
        this.xcs = xcs;
        this.net = net;
    }

    public void logBannerClick() {
        log(
                "action", "bannerclick",
                "xcs", xcs,
                "net", net
        );
    }

    public void logExtLink() {
        log(
                "action", "extlink",
                "xcs", xcs,
                "net", net
        );
    }

    public void logExtLinkAuto() {
        log(
                "action", "extlink-auto",
                "xcs", xcs,
                "net", net
        );
    }

    public void logExtLinkWarn() {
        log(
                "action", "extlink-warn",
                "xcs", xcs,
                "net", net
        );
    }

    public void logExtLinkConf() {
        log(
                "action", "extlink-conf",
                "xcs", xcs,
                "net", net
        );
    }

    public void logExtLinkBack() {
        log(
                "action", "extlink-back",
                "xcs", xcs,
                "net", net
        );
    }

    // There's no way to log this in Android at present (see schema), but we can use it if
    // at some point in the future Android sends a signal when the user closes the app or
    // navigates to a different app.
    public void logExtLinkClose() {
        log(
                "action", "extlink-close",
                "xcs", xcs,
                "net", net
        );
    }

    public void logExtLinkAlways() {
        log(
                "action", "extlink-always",
                "xcs", xcs,
                "net", net
        );
    }

    public void logExtLinkMore() {
        log(
                "action", "extlink-more",
                "xcs", xcs,
                "net", net
        );
    }

    @Override protected String getDurationFieldName() {
        return "time";
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    @Override protected void preprocessAppInstallID(@NonNull JSONObject eventData) { }
}
