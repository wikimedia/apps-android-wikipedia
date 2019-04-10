package org.wikipedia.feed.searchbar;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

import androidx.annotation.NonNull;

public class SearchCard extends Card {
    @NonNull @Override public CardType type() {
        return CardType.SEARCH_BAR;
    }
}
