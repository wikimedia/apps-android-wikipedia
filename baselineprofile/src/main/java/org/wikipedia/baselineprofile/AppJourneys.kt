package org.wikipedia.baselineprofile

import android.content.Intent
import android.net.Uri
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

// These journeys look for English UI strings, so the device should be set to English.
private const val UI_TIMEOUT_MILLIS = 10_000L
private const val FEED_TIMEOUT_MILLIS = 30_000L
private const val MAX_ONBOARDING_SCREENS = 5
private val FEED_CARD_HEADERS = Pattern.compile("Featured article|Top read|In the news|On this day|Picture of the day")
private const val ARTICLE_URL = "https://en.wikipedia.org/wiki/Earth"

val targetPackageName: String
    get() = InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: throw IllegalStateException("targetAppId not passed as an instrumentation runner argument")

/**
 * Gets a freshly installed app past the first-run onboarding and personalization screens, so that
 * subsequent launches go straight to the home feed like they do for returning users. This runs
 * outside of profile collection so that onboarding code isn't part of the startup profile.
 */
fun skipOnboarding(packageName: String) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.pressHome()
    // Launcher intent filters have no DEFAULT category, so the launcher component must be resolved before starting it.
    val launcherComponent = device.executeShellCommand(
        "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $packageName"
    ).lines().last { it.isNotBlank() }.trim()
    device.executeShellCommand("am start -W -n $launcherComponent")

    if (device.wait(Until.hasObject(By.desc("Forward")), UI_TIMEOUT_MILLIS)) {
        repeat(MAX_ONBOARDING_SCREENS) {
            device.findObject(By.desc("Forward"))?.click() ?: return@repeat
            device.waitForIdle()
        }
        device.wait(Until.findObject(By.text("Skip")), UI_TIMEOUT_MILLIS)?.click()
        device.wait(Until.hasObject(By.text(FEED_CARD_HEADERS)), FEED_TIMEOUT_MILLIS)
    }
    device.executeShellCommand("am force-stop $packageName")
}

fun MacrobenchmarkScope.waitForHomeFeed() {
    check(device.wait(Until.hasObject(By.text(FEED_CARD_HEADERS)), FEED_TIMEOUT_MILLIS)) {
        "Home feed did not load. Make sure the device is online and onboarding was skipped."
    }
}

fun MacrobenchmarkScope.scrollHomeFeed() {
    val feed = device.findObject(By.scrollable(true).hasDescendant(By.text(FEED_CARD_HEADERS))) ?: return
    feed.setGestureMargin(device.displayWidth / 5)
    repeat(3) {
        feed.fling(Direction.DOWN)
        device.waitForIdle()
    }
}

fun MacrobenchmarkScope.readArticle() {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ARTICLE_URL)).setPackage(packageName)
    startActivityAndWait(intent)
    val webView = device.wait(Until.findObject(By.res(packageName, "page_web_view")), UI_TIMEOUT_MILLIS) ?: return
    device.waitForIdle()
    webView.setGestureMargin(device.displayWidth / 5)
    repeat(3) {
        webView.fling(Direction.DOWN)
        device.waitForIdle()
    }
}
