package org.wikipedia.analytics;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;

public class RandomizerFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppRandomizer";
    private static final int REV_ID = 18118733;

    private final InvokeSource source;
    private int numSwipesForward;
    private int numSwipesBack;
    private int numClicksForward;
    private int numClicksBack;

    public RandomizerFunnel(WikipediaApp app, WikiSite wiki, InvokeSource source) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL, wiki);
        this.source = source;
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void swipedForward() {
        numSwipesForward++;
    }

    public void swipedBack() {
        numSwipesBack++;
    }

    public void clickedForward() {
        numClicksForward++;
    }

    public void clickedBack() {
        numClicksBack++;
    }

    public void done() {
        log(
                "source", source.ordinal(),
                "fingerSwipesForward", numSwipesForward,
                "fingerSwipesBack", numSwipesBack,
                "diceClicks", numClicksForward,
                "backClicks", numClicksBack
        );
    }
}
