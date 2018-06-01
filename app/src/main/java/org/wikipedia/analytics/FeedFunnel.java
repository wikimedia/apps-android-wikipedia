package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.CardType;

import java.util.Arrays;
import java.util.List;

public class FeedFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppFeed";
    private static final int REVISION = 18115458;

    private boolean entered;
    private static List<CardType> EXCLUDED_CARDS = Arrays.asList(CardType.SEARCH_BAR, CardType.PROGRESS);

    public FeedFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION, Funnel.SAMPLE_LOG_100);
    }

    public void enter() {
        if (!entered) {
            entered = true;
            resetDuration();
            log(
                    "action", "enter"
            );
        }
    }

    public void exit() {
        if (entered) {
            entered = false;
            log(
                    "action", "exit"
            );
        }
    }

    public void cardShown(@NonNull CardType cardType, String languageCode) {
        if (EXCLUDED_CARDS.contains(cardType)) {
            return;
        }
        log(
                "action", "cardShown",
                "cardType", cardType.code(),
                "language", languageCode
        );
    }

    public void cardClicked(@NonNull CardType cardType, String languageCode) {
        if (EXCLUDED_CARDS.contains(cardType)) {
            return;
        }
        log(
                "action", "cardClicked",
                "cardType", cardType.code(),
                "language", languageCode
        );
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
