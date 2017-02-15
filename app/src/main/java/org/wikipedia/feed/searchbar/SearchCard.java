package org.wikipedia.feed.searchbar;

import android.support.annotation.NonNull;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

public class SearchCard extends Card {
    @NonNull @Override public CardType type() {
        return CardType.SEARCH_BAR;
    }
}
