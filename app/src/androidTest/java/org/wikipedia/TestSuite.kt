package org.wikipedia

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.main.loggedinuser.ContributionScreenTest
import org.wikipedia.main.loggedinuser.NotificationScreenTest
import org.wikipedia.main.loggedinuser.SuggestedEditCardTest
import org.wikipedia.main.loggedinuser.WatchListTest
import org.wikipedia.main.loggedoutuser.EditArticleTest
import org.wikipedia.main.loggedoutuser.ExploreFeedTest
import org.wikipedia.main.loggedoutuser.HistoryScreenTest
import org.wikipedia.main.loggedoutuser.HomeScreenTest
import org.wikipedia.main.loggedoutuser.OnboardingTest
import org.wikipedia.main.loggedoutuser.PageTest
import org.wikipedia.main.loggedoutuser.SavedScreenTest
import org.wikipedia.robots.SettingsRobot

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
