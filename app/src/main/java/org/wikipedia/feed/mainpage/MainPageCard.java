package org.wikipedia.feed.mainpage;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

import androidx.annotation.NonNull;

public class MainPageCard extends Card {
    @NonNull @Override public CardType type() {
        return CardType.MAIN_PAGE;
    }
}
