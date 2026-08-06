package org.wikipedia.base

import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * On **any** test failure, reports which dialog(s) / bottom sheet(s) were actually on the screen at
 * that moment. This is the single most common hidden cause of a cryptic "no view found" /
 * "no node found" failure in this app, which fires many one-off dialogs from many screens.
 *
 * Why this exists: the suppression lists (`DataInjector` / `LiveDataComposeTest.suppressKnownDialogs`)
 * are a **denylist** — they only hide popups someone already knew about. When a *new* dialog is added
 * (e.g. the search-widget install sheet), no list has it yet, and a test that taps through that
 * screen fails with a misleading "couldn't find X". The natural-but-wrong reaction is to fiddle with
 * the locator or the navigation. This reporter removes the guesswork: it reads the **live fragment
 * tree** and names whatever modal is up — even a brand-new one nobody suppressed — so the failure log
 * tells you the real cause.
 *
 * When you see "covered the screen: [SomeDialog]" in a failure, the fix is almost always to suppress
 * or dismiss that dialog, NOT to change your locator.
 */
class ScreenStateReporter : TestWatcher() {

    override fun failed(e: Throwable, description: Description) {
        val dialogs = mutableListOf<String>()
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull()
                if (activity is FragmentActivity) {
                    collectShowingDialogs(activity.supportFragmentManager, dialogs)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not inspect on-screen dialogs after failure: ${t.message}")
            return
        }

        if (dialogs.isNotEmpty()) {
            Log.e(
                TAG,
                "⚠️ '${description.methodName}' failed while these dialog(s)/sheet(s) were covering " +
                    "the screen: $dialogs. If that wasn't expected, it is almost certainly the cause — " +
                    "suppress its Prefs flag (DataInjector / suppressKnownDialogs) or dismiss it before " +
                    "interacting. Do NOT just change your locator or navigation."
            )
        } else {
            Log.e(
                TAG,
                "'${description.methodName}' failed and no app dialog/sheet was on screen, so the cause " +
                    "is elsewhere (wrong screen, wrong locator, or a timing/sync issue)."
            )
        }
    }

    private fun collectShowingDialogs(fm: FragmentManager, out: MutableList<String>) {
        for (fragment in fm.fragments) {
            if (fragment is DialogFragment && fragment.dialog?.isShowing == true) {
                out.add(fragment::class.java.simpleName)
            }
            if (fragment.isAdded) {
                collectShowingDialogs(fragment.childFragmentManager, out)
            }
        }
    }

    companion object {
        private const val TAG = "ScreenStateReporter"
    }
}
