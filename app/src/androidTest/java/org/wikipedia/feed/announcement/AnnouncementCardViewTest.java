package org.wikipedia.feed.announcement;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnnouncementCardViewTest extends ViewTest {
    private static final String TEXT = "Hey Android readers,<br /><br /><strong>This is an announcement</strong>. Click the button below to execute the action that this announcement calls for.";
    private static final String ACTION = "Click here";
    private static final String FOOTER = "Here's a link to our <a href=\"https://wikimediafoundation.org/wiki/Survey_Privacy_Statement\">Privacy statement</a>.";
    private static final String IMAGE = "https://fake.url";
    private AnnouncementCardView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_M}) int widthDp,
                                  @NonNull FontScale fontScale) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, IMAGE);
        snap(subject);
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        setUp(WIDTH_DP_M, direction, FontScale.DEFAULT, Theme.LIGHT, IMAGE);
        snap(subject);
    }

    @Theory public void testTheme(@NonNull Theme theme) {
        setUp(WIDTH_DP_M, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, IMAGE);
        snap(subject);
    }

    @Theory public void testFocus(@NonNull Theme theme) {
        setUp(WIDTH_DP_M, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, IMAGE);
        requestFocus(subject);
        snap(subject);
    }

    @Theory public void testNoImage() {
        setUp(WIDTH_DP_M, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT, null);
        snap(subject);
    }

    protected void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                         @NonNull FontScale fontScale, @NonNull Theme theme,
                         @Nullable String imageUrl) {
        super.setUp(widthDp, layoutDirection, fontScale, theme);
        subject = new AnnouncementCardView(ctx());
        subject.setCard(mockAnnouncementCard(TEXT, ACTION, FOOTER, imageUrl));
    }

    @NonNull private AnnouncementCard mockAnnouncementCard(@NonNull String text,
                                                           @NonNull String action,
                                                           @NonNull String footer,
                                                           @Nullable String imageUrl) {
        SurveyCard card = mock(SurveyCard.class);
        when(card.type()).thenReturn(CardType.ANNOUNCEMENT);
        when(card.actionTitle()).thenReturn(action);
        when(card.hasAction()).thenReturn(true);
        when(card.extract()).thenReturn(text);
        when(card.hasFooterCaption()).thenReturn(true);
        when(card.footerCaption()).thenReturn(footer);
        when(card.hasImage()).thenReturn(imageUrl != null);
        when(card.image()).thenReturn(imageUrl != null ? Uri.parse(imageUrl) : null);
        return card;
    }
}
