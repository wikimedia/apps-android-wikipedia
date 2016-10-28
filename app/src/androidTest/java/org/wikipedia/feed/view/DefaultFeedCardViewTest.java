package org.wikipedia.feed.view;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.FeedAdapter.Callback;
import org.wikipedia.test.ViewTest;
import org.wikipedia.theme.Theme;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.wikipedia.test.ViewTest.LayoutDirection.LOCALE;

public class DefaultFeedCardViewTest extends ViewTest {
    private DefaultFeedCardView<Card> subject;

    @Before public void setUp() {
        setUp(WIDTH_DP_S, LOCALE, FONT_SCALES[0], Theme.LIGHT);
        subject = new Subject(ctx());
    }

    @Test public void testSetGetCard() {
        Card card = mock(Card.class);
        subject.setCard(card);
        assertThat(subject.getCard(), is(card));
    }

    @Theory public void testSetGetCallback(@TestedOn(ints = {0, 1}) int nonnull) {
        Callback callback = nonnull == 0 ? null : mock(Callback.class);
        subject.setCallback(callback);
        assertThat(subject.getCallback(), is(callback));
    }

    private static class Subject extends DefaultFeedCardView<Card> {
        Subject(Context context) {
            super(context);
        }
    }
}