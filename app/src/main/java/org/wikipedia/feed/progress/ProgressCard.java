package org.wikipedia.feed.progress;

import android.support.annotation.NonNull;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

public class ProgressCard extends Card {
    @NonNull @Override public CardType type() {
        return CardType.PROGRESS;
    }
}
