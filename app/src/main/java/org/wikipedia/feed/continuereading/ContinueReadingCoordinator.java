package org.wikipedia.feed.continuereading;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.history.HistoryEntry;

/** The state machine for continue reading cards. */
public class ContinueReadingCoordinator {
    @Nullable private ContinueReadingCard card;

    /** @param entry The article most recently viewed.
        @param lastDismissedTitle The title of the continue reading article most recently dismissed. */
    public void update(@NonNull HistoryEntry entry, @Nullable String lastDismissedTitle) {
        update(new ContinueReadingCard(entry), lastDismissedTitle);
    }

    public void update(@NonNull ContinueReadingCard card, @Nullable String lastDismissedTitle) {
        this.card = card.title().equals(lastDismissedTitle) || card.daysOld() < 1
                ? null
                : card;
    }

    @Nullable public ContinueReadingCard card() {
        return card;
    }
}