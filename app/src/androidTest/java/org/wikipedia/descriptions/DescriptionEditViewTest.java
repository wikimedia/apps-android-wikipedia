package org.wikipedia.descriptions;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.theories.TestedOnBool;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.PrimaryTestStr;
import org.wikipedia.test.view.SecondaryTestStr;
import org.wikipedia.test.view.TestStr;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wikipedia.descriptions.DescriptionEditView.Callback;

public class DescriptionEditViewTest extends ViewTest {
    private DescriptionEditView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_XL, WIDTH_DP_L}) int widthDp,
                                  @NonNull FontScale fontScale, @NonNull PrimaryTestStr title,
                                  @NonNull SecondaryTestStr description,
                                  @NonNull PrimaryTestStr error, @TestedOnBool boolean saving) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, title, description, error,
                saving);
        snap(subject, title + "_title", description + "_description", error + "_error",
                saving ? "saving" : "unsaved");
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        setUp(WIDTH_DP_L, direction, FontScale.DEFAULT, Theme.LIGHT, PrimaryTestStr.SHORT,
                SecondaryTestStr.SHORT, PrimaryTestStr.SHORT, false);
        snap(subject);
    }

    @Theory public void testTheme(@NonNull Theme theme, @TestedOnBool boolean saving) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, PrimaryTestStr.SHORT,
                SecondaryTestStr.SHORT, PrimaryTestStr.SHORT, saving);
        snap(subject, saving ? "saving" : "unsaved");
    }

    @Theory public void testFocus(@NonNull Theme theme, @TestedOnBool boolean saving) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, PrimaryTestStr.SHORT,
                SecondaryTestStr.SHORT, PrimaryTestStr.SHORT, saving);
        requestFocus(subject);
        snap(subject, saving ? "saving" : "unsaved");
    }

    @Theory public void testSetCallback(@TestedOnBool boolean nul) {
        defaultSetUp();
        Callback callback = nul ? null : mock(Callback.class);
        subject.setCallback(callback);

        subject.onSaveClick();
        if (callback != null) {
            verify(callback).onSaveClick();
        }
    }

    @Test public void testSetPageTitle() {
        defaultSetUp();
        PageTitle expected = mock(PageTitle.class);
        when(expected.getDisplayText()).thenReturn("title");
        when(expected.getDescription()).thenReturn("description");

        subject.setPageTitle(expected);
        assertThat(subject.pageTitleText.getText().toString(), is(expected.getDisplayText()));
        assertThat(subject.getDescription(), is(expected.getDescription()));
    }

    @Theory public void testSetSaveState(@TestedOnBool boolean saving) {
        defaultSetUp();
        subject.setSaveState(saving);
        assertThat(subject.saveButton.isEnabled(), is(!saving));
    }

    @Theory public void testGetDescription(@TestedOnBool boolean nul) {
        defaultSetUp();
        String expected = nul ? null : "text";
        subject.setDescription(expected);
        assertThat(subject.getDescription(), is(defaultString(expected)));
    }

    @Theory public void testSetTitle(@TestedOnBool boolean nul) {
        defaultSetUp();
        String expected = nul ? null : "text";
        subject.setTitle(expected);
        assertThat(subject.pageTitleText.getText().toString(), is(defaultString(expected)));
    }

    @Theory public void testSetDescription(@TestedOnBool boolean nul) {
        defaultSetUp();
        String expected = nul ? null : "text";
        subject.setDescription(expected);
        assertThat(subject.pageDescriptionText.getText().toString(), is(defaultString(expected)));
    }

    private void defaultSetUp() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT,
                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT, PrimaryTestStr.SHORT, false);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                       @NonNull FontScale fontScale, @NonNull Theme theme, @NonNull TestStr title,
                       @NonNull TestStr description, @NonNull TestStr error, boolean saving) {
        setUp(widthDp, layoutDirection, fontScale, theme);

        subject = new DescriptionEditView(ctx());
        subject.pageDescriptionLayout.setHintAnimationEnabled(false);

        subject.setTitle(str(title));
        subject.setDescription(str(description));
        subject.setError(str(error));

        // todo: vector drawables (when specified in xml with app:srcCompat) aren't rendering
        subject.setSaveState(saving);
    }
}
