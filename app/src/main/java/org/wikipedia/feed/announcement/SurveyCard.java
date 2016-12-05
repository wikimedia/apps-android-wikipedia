package org.wikipedia.feed.announcement;

import android.support.annotation.NonNull;

import org.wikipedia.feed.model.CardType;

public class SurveyCard extends AnnouncementCard {

    public SurveyCard(@NonNull Announcement announcement) {
        super(announcement);
    }

    @NonNull @Override public CardType type() {
        return CardType.SURVEY;
    }
}
