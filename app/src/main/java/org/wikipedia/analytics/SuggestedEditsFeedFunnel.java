package org.wikipedia.analytics;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.WikipediaApp;
import org.wikipedia.descriptions.DescriptionEditActivity;

public class SuggestedEditsFeedFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppSuggestedEditsFeed";
    private static final int REVISION = 20437611;
    private InvokeSource source;
    private DescriptionEditActivity.Action type;

    public SuggestedEditsFeedFunnel(DescriptionEditActivity.Action type, InvokeSource source) {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, REVISION, Funnel.SAMPLE_LOG_ALL);
        this.type = type;
        this.source = source;
    }

    public void start() {
        log(
                "action", "start"
        );
    }

    public void stop() {
        log(
                "action", "stop"
        );
    }

    public void editSuccess() {
        log(
                "action", "editSuccess"
        );
    }

    @Override
    protected void preprocessSessionToken(@NonNull JSONObject eventData) {
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "source", source.getName());
        preprocessData(eventData, "type", type == DescriptionEditActivity.Action.ADD_IMAGE_TAGS ? "tags"
                : type == DescriptionEditActivity.Action.ADD_CAPTION || type == DescriptionEditActivity.Action.TRANSLATE_CAPTION ? "captions" : "descriptions");
        return super.preprocessData(eventData);
    }
}
