package org.wikipedia.feed.continuereading;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.test.TestRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(TestRunner.class)
public class ContinueReadingCoordinatorTest {
    @NonNull private final ContinueReadingCoordinator subject = new ContinueReadingCoordinator();

    @Test public void testUpdateCtor() {
        assertThat(subject.card(), nullValue());
    }

    @Test public void testUpdateLastReadToday() {
        ContinueReadingCard card = mock(ContinueReadingCard.class);
        when(card.title()).thenReturn("last read");
        when(card.daysOld()).thenReturn(0L);
        String lastDismissedTitle = "last dismissed";

        subject.update(card, lastDismissedTitle);

        assertThat(subject.card(), nullValue());
    }

    @Test public void testUpdateLastReadYesterday() {
        ContinueReadingCard card = mock(ContinueReadingCard.class);
        when(card.title()).thenReturn("last read");
        when(card.daysOld()).thenReturn(1L);
        String lastDismissedTitle = "last dismissed";

        subject.update(card, lastDismissedTitle);

        assertThat(subject.card(), is(card));
    }

    @Test public void testUpdateNoLastDismissedTitle() {
        ContinueReadingCard card = mock(ContinueReadingCard.class);
        when(card.title()).thenReturn("last read");
        when(card.daysOld()).thenReturn(1L);
        final String lastDismissedTitle = null;

        subject.update(card, lastDismissedTitle);

        assertThat(subject.card(), is(card));
    }

    @Test public void testUpdateDismissedTitle() {
        ContinueReadingCard card = mock(ContinueReadingCard.class);
        when(card.title()).thenReturn("last read");
        when(card.daysOld()).thenReturn(1L);
        String lastDismissedTitle = "last read";

        subject.update(card, lastDismissedTitle);

        assertThat(subject.card(), nullValue());
    }
}