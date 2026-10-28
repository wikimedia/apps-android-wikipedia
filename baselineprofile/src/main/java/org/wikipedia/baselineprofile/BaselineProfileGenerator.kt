package org.wikipedia.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the app's Baseline Profile and Startup Profile. Run with:
 *
 *   ./gradlew :app:generateBaselineProfile
 *
 * The results are written to app/src/main/generated/baselineProfiles/, and should be committed.
 * See baselineprofile/README.md for details.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Before
    fun setUp() {
        skipOnboarding(targetPackageName)
    }

    /**
     * The cold-start path of a returning user: launching the app into the home feed. The code
     * exercised here goes into the startup profile (used for DEX layout) as well as the baseline profile.
     */
    @Test
    fun startup() = rule.collect(
        packageName = targetPackageName,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        waitForHomeFeed()
    }

    /**
     * The most common journeys after startup. These are added to the baseline profile only,
     * so that they're precompiled, without crowding the startup classes out of the primary DEX file.
     */
    @Test
    fun browseAndRead() = rule.collect(
        packageName = targetPackageName,
        includeInStartupProfile = false
    ) {
        pressHome()
        startActivityAndWait()
        waitForHomeFeed()
        scrollHomeFeed()
        readArticle()
    }
}
