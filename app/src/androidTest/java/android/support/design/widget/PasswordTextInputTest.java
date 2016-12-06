package android.support.design.widget;

import android.support.test.filters.SmallTest;

import org.junit.Before;
import org.junit.experimental.theories.Theory;
import org.wikipedia.test.theories.TestedOnBool;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import static android.support.design.widget.PasswordTextInput.OnShowPasswordClickListener;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SmallTest public class PasswordTextInputTest extends ViewTest {
    private PasswordTextInput subject;

    @Before public void setUp() {
        setUp(WIDTH_DP_S, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT);
        subject = new PasswordTextInput(ctx());
        subject.setPasswordVisibilityToggleEnabled(true);
    }

    @Theory public void testIsPasswordVisible(@TestedOnBool boolean visible) {
        if (visible) {
            subject.passwordVisibilityToggleRequested();
        }
        assertThat(subject.isPasswordVisible(), is(visible));
    }

    @Theory public void testSetOnShowPasswordListener(@TestedOnBool boolean nul,
                                                      @TestedOnBool boolean visible) {
        OnShowPasswordClickListener listener = nul ? null : mock(OnShowPasswordClickListener.class);
        if (visible) {
            subject.passwordVisibilityToggleRequested();
        }
        subject.setOnShowPasswordListener(listener);
        subject.passwordVisibilityToggleRequested();
        if (listener != null) {
            verify(listener).onShowPasswordClick(eq(!visible));
        }
    }
}