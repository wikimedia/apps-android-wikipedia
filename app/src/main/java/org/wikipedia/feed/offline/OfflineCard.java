package org.wikipedia.feed.offline;

import android.support.annotation.NonNull;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

public class OfflineCard extends Card {
    @NonNull @Override public CardType type() {
        return CardType.OFFLINE;
    }
}
