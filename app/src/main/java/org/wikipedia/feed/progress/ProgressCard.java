package org.wikipedia.feed.progress;

import android.support.annotation.NonNull;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

public class ProgressCard extends Card {
    @Override @NonNull
    public String title() {
        return "";
    }

    @NonNull @Override public CardType type() {
        return CardType.PROGRESS;
    }
}