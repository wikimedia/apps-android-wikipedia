package org.wikipedia.feed.offline;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

import androidx.annotation.NonNull;

public class OfflineCard extends Card {
    @NonNull @Override public CardType type() {
        return CardType.OFFLINE;
    }
}
