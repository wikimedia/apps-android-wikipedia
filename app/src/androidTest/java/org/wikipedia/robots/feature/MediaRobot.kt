package org.wikipedia.robots.feature

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.allOf
import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.GifMatchers
import org.wikipedia.base.TestConfig

class MediaRobot : BaseRobot() {

    fun verifyLeadImageHasGif() = apply {
        onView(withId(R.id.view_page_header_image))
            .perform(waitForAsyncLoading())
            .check(matches(GifMatchers.hasGifDrawable()))
    }

    fun pinchZoomAction(context: Context, uiDevice: UiDevice) = apply {
        val imageView = uiDevice.findObject(UiSelector().resourceId("${context.packageName}:id/imageView"))
        imageView.pinchIn(75, 20)
        imageView.pinchOut(75, 20)
        imageView.dragTo(500, 700, 20)
    }

    fun doubleTapToZoomOut() = apply {
        doubleClickOnViewWithId(viewId = R.id.imageView)
        delay(TestConfig.DELAY_SHORT)
    }

    fun toggleOverlayVisibility() = apply {
        onView(
            allOf(
                withId(R.id.imageView),
                isDisplayed()
            )
        ).perform(click())
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyOverlayVisibility(isVisible: Boolean) = apply {
        if (isVisible) {
            checkViewExists(R.id.toolbar_container)
            delay(TestConfig.DELAY_MEDIUM)
            return@apply
        }

        checkViewDoesNotExist(R.id.toolbar_container)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun navigateUp() = apply {
        clickOnDisplayedViewWithContentDescription("Navigate up")
    }

    fun clickCC() = apply {
        clickOnViewWithId(R.id.license_icon)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyCCisClicked() = apply {
        checkPartialString("CC")
        delay(TestConfig.DELAY_SHORT)
    }

    fun tapHamburger(context: Context) = apply {
        openActionBarOverflowOrOptionsMenu(context)
        delay(TestConfig.DELAY_SHORT)
    }

    fun goToImagePage(context: Context) = apply {
        clickOnViewWithText(context.getString(R.string.menu_gallery_visit_image_page))
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyImagePageIsVisible() = apply {
        try {
            clickOnViewWithId(R.id.filePageView)
        } catch (e: Exception) {
            Log.e("MediaRobot:", "filePageView must not be visible.")
        }
    }

    fun clickShareButton() = apply {
        // we are doing this because if we open the share dialog which is a system dialog and
        // espresso cannot interact with it, so this tells the Espresso
        // when the share dialog opens, pretend the user immediately pressed back
        // or made a selection
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        )
        clickOnViewWithId(R.id.menu_gallery_share)
        delay(TestConfig.DELAY_SHORT)
    }
}
