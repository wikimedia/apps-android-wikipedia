package org.wikipedia.espresso.onboarding;

import android.support.test.espresso.ViewInteraction;
import android.support.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.espresso.util.ViewTools;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;

@RunWith(AndroidJUnit4.class)
public final class OnBoardingTest {

    public static void runOnBoarding() {
        if (ViewTools.viewIsDisplayed(R.id.fragment_pager)) {
            ViewInteraction viewPager1 = onView(
                    allOf(withId(R.id.fragment_pager),
                            ViewTools.childAtPosition(
                                    allOf(withId(R.id.fragment_onboarding_pager_container),
                                            ViewTools.childAtPosition(
                                                    withId(R.id.fragment_container),
                                                    0)),
                                    0),
                            isDisplayed()));
            viewPager1.perform(swipeLeft());

            ViewInteraction viewPager2 = onView(
                    allOf(withId(R.id.fragment_pager),
                            ViewTools.childAtPosition(
                                    allOf(withId(R.id.fragment_onboarding_pager_container),
                                            ViewTools.childAtPosition(
                                                    withId(R.id.fragment_container),
                                                    0)),
                                    0),
                            isDisplayed()));
            viewPager2.perform(swipeLeft());

            ViewInteraction viewPager3 = onView(
                    allOf(withId(R.id.fragment_pager),
                            ViewTools.childAtPosition(
                                    allOf(withId(R.id.fragment_onboarding_pager_container),
                                            ViewTools.childAtPosition(
                                                    withId(R.id.fragment_container),
                                                    0)),
                                    0),
                            isDisplayed()));
            viewPager3.perform(swipeLeft());

            ViewInteraction appCompatTextView = onView(
                    allOf(withId(R.id.fragment_onboarding_done_button), withText("Get started"),
                            ViewTools.childAtPosition(
                                    ViewTools.childAtPosition(
                                            withClassName(is("android.widget.FrameLayout")),
                                            2),
                                    1),
                            isDisplayed()));
            appCompatTextView.perform(click());
        }
    }

    private OnBoardingTest() { }
}
