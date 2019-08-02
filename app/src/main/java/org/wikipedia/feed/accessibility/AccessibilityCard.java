package org.wikipedia.feed.accessibility;

import androidx.annotation.NonNull;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

public class AccessibilityCard extends Card {
    @NonNull @Override public CardType type() {
        return CardType.ACCESSIBILITY;
    }
}
