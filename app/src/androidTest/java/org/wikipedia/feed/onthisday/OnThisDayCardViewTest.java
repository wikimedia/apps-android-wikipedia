package org.wikipedia.feed.onthisday;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.experimental.theories.Theory;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.PrimaryTestStr;
import org.wikipedia.test.view.SecondaryTestStr;
import org.wikipedia.test.view.TestStr;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class OnThisDayCardViewTest extends ViewTest {
    private static final int NEXT_YEAR = 2001;
    private static final int YEAR = 2000;
    private static final String TEXT = "Here's a description of the event for this date.";
    private OnThisDayCardView subject;


    @Theory
    public void testSetNextYearText() {
        assertThat(subject.nextYearTextView.getText().toString(), is("" + NEXT_YEAR));
    }

    @Theory
    public void testYearText() {
        assertThat(subject.yearTextView.getText().toString(), is("" + YEAR));
    }

    @Theory
    public void testTitleText() {
        assertThat(subject.getCard().title(), is(str(PrimaryTestStr.SHORT)));
    }

    @Theory
    public void testSubTitleText() {
        assertThat(subject.getCard().subtitle(), is(str(SecondaryTestStr.SHORT)));
    }

    @Theory
    public void testText() {
        assertThat(subject.descTextView.getText().toString(), is(TEXT));
    }

    @Before
    public void setUp() {
        super.setUp(WIDTH_DP_M, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT);
        subject = new OnThisDayCardView(ctx());
        subject.setCard(mockAnnouncementCard(NEXT_YEAR, YEAR, PrimaryTestStr.SHORT, SecondaryTestStr.SHORT, TEXT));
    }

    @NonNull
    private OnThisDayCard mockAnnouncementCard(int nextYear, int year, PrimaryTestStr title, TestStr subtitle, String text) {
        OnThisDayCard card = mock(OnThisDayCard.class);
        when(card.title()).thenReturn(str(title));
        when(card.subtitle()).thenReturn(str(subtitle));
        when(card.text()).thenReturn(text);
        when(card.nextYear()).thenReturn(nextYear);
        when(card.year()).thenReturn(year);
        return card;
    }
}
