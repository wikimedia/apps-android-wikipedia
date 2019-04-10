package org.wikipedia.feed.announcement;

import org.wikipedia.feed.model.CardType;

import androidx.annotation.NonNull;

public class SurveyCard extends AnnouncementCard {

    public SurveyCard(@NonNull Announcement announcement) {
        super(announcement);
    }

    @NonNull @Override public CardType type() {
        return CardType.SURVEY;
    }
}
