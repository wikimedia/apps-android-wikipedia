package org.wikipedia.feed.mainpage;

import android.support.annotation.NonNull;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

public class MainPageCard extends Card {
    @NonNull @Override public CardType type() {
        return CardType.MAIN_PAGE;
    }
}
