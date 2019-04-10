package org.wikipedia.feed.announcement;

import org.wikipedia.feed.model.CardType;

import androidx.annotation.NonNull;

public class FundraisingCard extends AnnouncementCard {

    public FundraisingCard(@NonNull Announcement announcement) {
        super(announcement);
    }

    @NonNull @Override public CardType type() {
        return CardType.FUNDRAISING;
    }
}
