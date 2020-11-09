package org.wikipedia.feed.announcement;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

public class AnnouncementCard extends Card {
    @NonNull private final Announcement announcement;

    public AnnouncementCard(@NonNull Announcement announcement) {
        this.announcement = announcement;
    }

    @Override @NonNull public String title() {
        return announcement.type();
    }

    @Nullable @Override public String extract() {
        return announcement.text();
    }

    public boolean hasAction() {
        return announcement.hasAction();
    }

    @NonNull public String actionTitle() {
        return announcement.actionTitle();
    }

    @NonNull public Uri actionUri() {
        return Uri.parse(announcement.actionUrl());
    }

    @Nullable public String negativeText() {
        return announcement.negativeText();
    }

    public boolean hasFooterCaption() {
        return announcement.hasFooterCaption();
    }

    @NonNull public String footerCaption() {
        return announcement.footerCaption();
    }

    public boolean hasImage() {
        return announcement.hasImageUrl();
    }

    boolean hasBorder() {
        return announcement.hasBorder();
    }

    @NonNull public boolean isArticlePlacement() {
        return Announcement.PLACEMENT_ARTICLE.equals(announcement.placement());
    }

    @NonNull @Override public Uri image() {
        return Uri.parse(announcement.imageUrl());
    }

    public int imageHeight() {
        try {
            return Integer.parseInt(announcement.imageHeight());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @NonNull @Override public CardType type() {
        return CardType.ANNOUNCEMENT;
    }

    @Override protected int dismissHashCode() {
        return announcement.id().hashCode();
    }
}
