package org.wikipedia

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.robots.feature.SettingsRobot
import org.wikipedia.test.loggedinuser.ContributionScreenTest
import org.wikipedia.test.loggedinuser.NotificationScreenTest
import org.wikipedia.test.loggedoutuser.EditArticleTest
import org.wikipedia.test.loggedoutuser.ExploreFeedTest
import org.wikipedia.test.loggedoutuser.HistoryScreenTest
import org.wikipedia.test.loggedoutuser.HomeScreenTest
import org.wikipedia.test.loggedoutuser.OnboardingTest
import org.wikipedia.test.loggedoutuser.PageTest
import org.wikipedia.test.loggedoutuser.SavedScreenTest

@RunWith(Suite::class)
@SuiteClasses(
    EditArticleTest::class,
    ExploreFeedTest::class,
    HistoryScreenTest::class,
    HomeScreenTest::class,
    OnboardingTest::class,
    PageTest::class,
    SavedScreenTest::class,
    SettingsRobot::class,
    ContributionScreenTest::class,
    NotificationScreenTest::class
)
class TestSuite
