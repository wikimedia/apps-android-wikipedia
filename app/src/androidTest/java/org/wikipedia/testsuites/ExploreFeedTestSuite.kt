package org.wikipedia.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.explorefeed.BecauseYouReadTest
import org.wikipedia.tests.explorefeed.NavigationItemTest

@RunWith(Suite::class)
@SuiteClasses(
    NavigationItemTest::class,
    BecauseYouReadTest::class,
    // FeedScreenSuggestedEditTest::class, TODO: uncomment when login test in CI/CD is resolved
)
class ExploreFeedTestSuite
