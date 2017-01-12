package org.wikipedia.descriptions;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;

import com.rd.PageIndicatorView;

import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.R;
import org.wikipedia.test.theories.TestedOnBool;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import butterknife.ButterKnife;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SmallTest public class DescriptionEditTutorialPageViewTest extends ViewTest {
    private DescriptionEditTutorialPageView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_XL, WIDTH_DP_L}) int widthDp, @NonNull FontScale fontScale) {
        for (DescriptionEditTutorialPage page : DescriptionEditTutorialPage.values()) {
            setUp(page, widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT);
            snap(subject, page.name());
        }
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        for (DescriptionEditTutorialPage page : DescriptionEditTutorialPage.values()) {
            setUp(page, WIDTH_DP_L, direction, FontScale.DEFAULT, Theme.LIGHT);
            snap(subject, page.name());
        }
    }

    @Theory public void testTheme(@NonNull Theme theme) {
        for (DescriptionEditTutorialPage page : DescriptionEditTutorialPage.values()) {
            setUp(page, WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme);
            snap(subject, page.name());
        }
    }

    @Theory public void testFocus(@NonNull Theme theme) {
        for (DescriptionEditTutorialPage page : DescriptionEditTutorialPage.values()) {
            setUp(page, WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme);
            requestFocus(subject);
            snap(subject, page.name());
        }
    }

    @Theory public void testSetCallback(@TestedOnBool boolean nul) {
        for (DescriptionEditTutorialPage page : DescriptionEditTutorialPage.values()) {
            setUp(page, WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT);
            subject = new DescriptionEditTutorialPageView(ctx());
            DescriptionEditTutorialPageView.Callback callback = nul ? null : mock(DescriptionEditTutorialPageView.Callback.class);
            subject.setCallback(callback);

            subject.onButtonClick();
            if (callback != null) {
                verify(callback).onButtonClick(subject);
            }
        }
    }

    private void setUp(@NonNull DescriptionEditTutorialPage page, int widthDp, @NonNull LayoutDirection layoutDirection, @NonNull FontScale fontScale, @NonNull Theme theme) {
        super.setUp(widthDp, layoutDirection, fontScale, theme);
        LayoutInflater inflater = LayoutInflater.from(ctx());
        subject = (DescriptionEditTutorialPageView) inflater.inflate(page.getLayout(), null, false);
        ButterKnife.bind(subject);

        ViewPager viewPager = new ViewPager(ctx());
        viewPager.setAdapter(new DescriptionEditTutorialPagerAdapter(mock(DescriptionEditTutorialPagerAdapter.Callback.class)));

        PageIndicatorView pageIndicatorView = ButterKnife.findById(subject, R.id.view_description_edit_tutorial_page_indicator);
        pageIndicatorView.setViewPager(viewPager);
        pageIndicatorView.setSelection(page.code());
    }
}
