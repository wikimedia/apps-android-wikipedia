package org.wikipedia.espresso.util;

import android.app.Activity;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.TabLayout;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.ViewActions;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@SuppressWarnings("checkstyle:magicnumber")
public final class ViewTools {

    public static final int WAIT_FOR_6000 = 6000;
    public static final int WAIT_FOR_5000 = 5000;
    public static final int WAIT_FOR_2000 = 2000;
    public static final int WAIT_FOR_3000 = 3000;
    public static final int WAIT_FOR_1000 = 1000;
    public static final int WAIT_FOR_500 = 500;


    public interface WhileCondition {
        boolean condition();
    }

    public interface WhileBody {
        void body();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    public static void whileWithMaxSteps(WhileCondition condition, WhileBody body) {
        whileWithMaxSteps(condition, body, 5);
    }

    public static void whileWithMaxSteps(WhileCondition condition, WhileBody body, int maxSteps) {
        int steps = 0;
        while (condition.condition() && ++steps < maxSteps) {
            body.body();
        }
        if (steps >= maxSteps) {
            throw new RuntimeException("Loop condition exceeded maximum steps.");
        }
    }

    public static boolean viewIsDisplayed(int viewId) {
        final boolean[] isDisplayed = {true};

        onView(withId(viewId))
                .withFailureHandler((Throwable error, Matcher<View> viewMatcher) -> isDisplayed[0] = false)
                .check(matches(isDisplayed()));

        return isDisplayed[0];
    }

    public static boolean viewWithTextIsDisplayed(String text) {
        final boolean[] isDisplayed = {true};

        onView(withText(text))
                .withFailureHandler((Throwable error, Matcher<View> viewMatcher) -> isDisplayed[0] = false)
                .check(matches(isDisplayed()));

        return isDisplayed[0];
    }
    public static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }


    public static void pressBack() {
        onView(isRoot()).perform(ViewActions.pressBack());
    }

    private static ViewAction doWaitFor(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Wait for " + millis + " milliseconds.";
            }

            @Override
            public void perform(UiController uiController, final View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }

    public static void waitFor(final long millis) {
        onView(isRoot()).perform(doWaitFor(millis));
    }

    private static ViewAction doRotateScreen(final Activity activity, final int orientation) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Do screen rotation";
            }

            @Override
            public void perform(UiController uiController, final View view) {
                activity.setRequestedOrientation(orientation);
            }
        };
    }

    public static void rotateScreen(final Activity activity, final int orientation) {
        onView(isRoot()).perform(doRotateScreen(activity, orientation));
    }

    public static Matcher<View> first(Matcher<View> expected) {

        return new TypeSafeMatcher<View>() {
            private boolean first = false;

            @Override
            protected boolean matchesSafely(View item) {

                if (expected.matches(item) && !first) {
                    first = true;
                    return true;
                }

                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Matcher.first( " + expected.toString() + " )");
            }
        };
    }

    public static ViewAction setTextInTextView(final String text) {
        return new ViewAction() {
            @SuppressWarnings("unchecked")
            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(TextView.class));
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((TextView) view).setText(text);
            }

            @Override
            public String getDescription() {
                return "replace text";
            }
        };
    }

    public static Matcher<View> matchPosition(final Matcher<View> matcher, final int position) {
        return new BaseMatcher<View>() {
            private int counter = 0;

            @Override
            public boolean matches(final Object item) {
                if (matcher.matches(item)) {
                    if (counter == position) {
                        counter++;
                        return true;
                    }
                    counter++;
                }
                return false;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Element at hierarchy position " + position);
            }
        };
    }

    public static ViewAction selectTab(final int tab) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayingAtLeast(90);
            }

            @Override
            public String getDescription() {
                return "Select a tab in TabLayout";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();
                if (view instanceof TabLayout) {
                    TabLayout tabLayout = (TabLayout) view;
                    tabLayout.getTabAt(tab).select();
                } else if (view instanceof BottomNavigationView) {
                    BottomNavigationView tabLayout = (BottomNavigationView) view;
                    tabLayout.setSelectedItemId(tab);
                }
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                v.performClick();
            }
        };
    }

    private ViewTools() {
    }
}
