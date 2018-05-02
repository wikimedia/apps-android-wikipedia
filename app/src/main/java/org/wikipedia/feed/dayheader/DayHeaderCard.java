package org.wikipedia.feed.dayheader;

import android.support.annotation.NonNull;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.util.DateUtil;

public class DayHeaderCard extends Card {
    private int age;

    public DayHeaderCard(int age) {
        this.age = age;
    }

    @Override @NonNull public String title() {
        return DateUtil.getFeedCardDayHeaderDate(age);
    }

    @NonNull @Override public CardType type() {
        return CardType.DAY_HEADER;
    }
}
