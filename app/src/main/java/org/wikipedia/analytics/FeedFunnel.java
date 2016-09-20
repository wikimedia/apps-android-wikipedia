package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.CardType;

public class FeedFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppFeed";
    private static final int REVISION = 15734713;

    public FeedFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION, Funnel.SAMPLE_LOG_100);
    }

    public void requestMore(int age) {
        log(
                "action", "more",
                "age", age
        );
    }

    public void refresh(int age) {
        log(
                "action", "refresh",
                "age", age
        );
    }

    public void dismissCard(@NonNull CardType cardType, int position) {
        log(
                "action", "dismiss",
                "cardType", cardType.code(),
                "position", position
        );
    }
}
