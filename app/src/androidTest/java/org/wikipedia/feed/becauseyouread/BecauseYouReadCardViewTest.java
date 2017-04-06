package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.R;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.PrimaryTestImg;
import org.wikipedia.test.view.PrimaryTestStr;
import org.wikipedia.test.view.SecondaryTestImg;
import org.wikipedia.test.view.SecondaryTestStr;
import org.wikipedia.test.view.TestStr;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BecauseYouReadCardViewTest extends ViewTest {
    private static final int TODAY = 0;
    private static final int TOMORROW = 1;

    private static final int MAX_SUGGESTIONS = 5;
    @StringRes private static final int[] SUGGESTION_TITLES = {
            PrimaryTestStr.SHORT.id(), PrimaryTestStr.LONG.id(),
            R.string.clear_recent_searches_confirm, R.string.preference_summary_show_images,
            R.string.tool_tip_toc_button
    };
    @StringRes private static final int[] SUGGESTION_SUBTITLES = {
            SecondaryTestStr.NULL.id(), SecondaryTestStr.SHORT.id(), SecondaryTestStr.LONG.id(),
            R.string.error_no_maps_app, R.string.crash_report_relaunch_or_quit
    };

    private Subject subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_M}) int widthDp,
                                  @NonNull FontScale fontScale, @NonNull PrimaryTestStr title,
                                  @NonNull SecondaryTestStr subtitle,
                                  @TestedOn(ints = {0, 1, MAX_SUGGESTIONS}) int suggestions) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, title, subtitle, TODAY,
                suggestions);
        snap(subject, title + "_title", subtitle + "_subtitle", suggestions + "_suggestions");
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        setUp(WIDTH_DP_L, direction, FontScale.DEFAULT, Theme.LIGHT, PrimaryTestStr.SHORT,
                SecondaryTestStr.SHORT, TODAY, MAX_SUGGESTIONS);
        snap(subject);
    }

    @Theory public void testTheme(@NonNull Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, PrimaryTestStr.SHORT,
                SecondaryTestStr.SHORT, TODAY, MAX_SUGGESTIONS);
        snap(subject);
    }

    @Theory public void testFocus(@NonNull Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, PrimaryTestStr.SHORT,
                SecondaryTestStr.SHORT, TODAY, MAX_SUGGESTIONS);
        requestFocus(subject);
        snap(subject);
    }

    @Test public void testSetCard() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT,
                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT, TODAY, MAX_SUGGESTIONS);

        BecauseYouReadCard card = mock(BecauseYouReadCard.class);
        subject.setCard(card);
        assertThat(subject.getCard(), is(card));
    }

    @Theory public void testSetSubtitle(@TestedOn(ints = {TODAY, TOMORROW, 2}) int age) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT,
                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT, TODAY, MAX_SUGGESTIONS);

        String expected;
        switch (age) {
            case TODAY: expected = "Today"; break;
            case TOMORROW: expected = "Yesterday"; break;
            default: expected = String.valueOf(age);
        }

        String subtitle = subject.getSubtitle(age);
        assertThat(subtitle, containsString(expected));
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                       @NonNull FontScale fontScale, @NonNull Theme theme, @NonNull TestStr title,
                       @NonNull TestStr subtitle, int age, int suggestions) {
        setUp(widthDp, layoutDirection, fontScale, theme);

        BecauseYouReadCard card = mockBecauseYouReadCard(title.id(), subtitle.id(), age,
                suggestions);
        subject = new Subject(ctx());
        subject.setCard(card);
    }

    @NonNull private BecauseYouReadCard mockBecauseYouReadCard(@StringRes int title,
                                                               @StringRes int subtitle,
                                                               int daysOld, int suggestion) {
        BecauseYouReadCard card = mock(BecauseYouReadCard.class);
        when(card.daysOld()).thenReturn((long) daysOld);
        when(card.title()).thenReturn(str(R.string.view_because_you_read_card_title));
        when(card.pageTitle()).thenReturn(str(title));
        when(card.subtitle()).thenReturn(str(subtitle));
        when(card.image()).thenReturn(frescoUri(SecondaryTestImg.CHECKERBOARD.id()));

        List<BecauseYouReadItemCard> suggestions = mockBecauseYouReadItemCards(suggestion);
        when(card.items()).thenReturn(suggestions);
        return card;
    }

    @NonNull private List<BecauseYouReadItemCard> mockBecauseYouReadItemCards(int n) {
        List<BecauseYouReadItemCard> suggestions = new ArrayList<>();
        for (int i = 0; i < n; ++i) {
            BecauseYouReadItemCard suggestion = mockBecauseYouReadItemCard(str(SUGGESTION_TITLES[i]),
                    str(SUGGESTION_SUBTITLES[i]));
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    @NonNull private BecauseYouReadItemCard mockBecauseYouReadItemCard(@NonNull String title,
                                                                       @Nullable String subtitle) {
        PageTitle pageTitle = mockPageTitle(title, subtitle);
        BecauseYouReadItemCard mock = mock(BecauseYouReadItemCard.class);
        when(mock.pageTitle()).thenReturn(pageTitle);
        return mock;
    }

    @NonNull private PageTitle mockPageTitle(@NonNull String title, @Nullable String subtitle) {
        PageTitle mock = mock(PageTitle.class);
        when(mock.getDisplayText()).thenReturn(title);
        when(mock.getDescription()).thenReturn(subtitle);

        // todo: fix image. the correct uri that is working in PageTitleListCardItemViewTest is
        //       referenced from here but the image appears all white
        when(mock.getThumbUrl()).thenReturn(frescoUri(PrimaryTestImg.NONNULL.id()).toString());
        return mock;
    }

    private static class Subject extends BecauseYouReadCardView {
        Subject(Context context) {
            super(context);
        }

        @Nullable @Override public BecauseYouReadCard getCard() {
            return super.getCard();
        }
    }
}
