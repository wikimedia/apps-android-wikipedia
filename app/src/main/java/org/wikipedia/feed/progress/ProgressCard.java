package org.wikipedia.feed.progress;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

import androidx.annotation.NonNull;

public class ProgressCard extends Card {
    @NonNull @Override public CardType type() {
        return CardType.PROGRESS;
    }
}
