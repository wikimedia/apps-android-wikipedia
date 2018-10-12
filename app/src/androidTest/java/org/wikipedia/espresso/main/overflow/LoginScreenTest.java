package org.wikipedia.espresso.main.overflow;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.espresso.util.ScreenshotTools;
import org.wikipedia.login.LoginActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;
import static org.wikipedia.espresso.util.CompareTools.assertScreenshotWithinTolerance;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_1000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_2000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_500;
import static org.wikipedia.espresso.util.ViewTools.childAtPosition;
import static org.wikipedia.espresso.util.ViewTools.rotateScreen;
import static org.wikipedia.espresso.util.ViewTools.waitFor;
import static org.wikipedia.login.LoginActivity.LOGIN_REQUEST_SOURCE;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("checkstyle:magicnumber")
public class LoginScreenTest {

    @Rule
    public ActivityTestRule<LoginActivity> activityTestRule =
            new ActivityTestRule<LoginActivity>(LoginActivity.class) {
                @Override
                protected Intent getActivityIntent() {
                    Context targetContext = InstrumentationRegistry.getInstrumentation()
                            .getTargetContext();
                    Intent result = new Intent(targetContext, LoginActivity.class);
                    result.putExtra(LOGIN_REQUEST_SOURCE, "Test");
                    return result;
                }
            };

    @Rule
    public GrantPermissionRule runtimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void loginScreenTest() throws Exception {
        waitFor(WAIT_FOR_2000);

        ViewInteraction plainPasteEditText = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.login_username_text),
                                0),
                        0),
                        isDisplayed()));
        plainPasteEditText.perform(replaceText("xxx"), closeSoftKeyboard());

        ViewInteraction plainPasteEditText2 = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.login_password_input),
                                0),
                        0),
                        isDisplayed()));
        plainPasteEditText2.perform(replaceText("xxx"), closeSoftKeyboard());
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("LoginScreen");

        rotateScreen(activityTestRule.getActivity(), ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("LoginScreen_Landscape");
        rotateScreen(activityTestRule.getActivity(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        waitFor(WAIT_FOR_500);
        onView(withText("Join Wikipedia")).perform(click());
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("CreateAccountScreen");
        runComparisons();
    }

    private void runComparisons() throws Exception {
        assertScreenshotWithinTolerance("LoginScreen");
        assertScreenshotWithinTolerance("LoginScreen_Landscape");
        assertScreenshotWithinTolerance("CreateAccountScreen");
    }
}
