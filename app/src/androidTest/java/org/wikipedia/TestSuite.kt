package org.wikipedia

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.test.loggedoutuser.ExploreFeedTest
import org.wikipedia.test.loggedoutuser.OnboardingTest

@RunWith(Suite::class)
@SuiteClasses(
    OnboardingTest::class,
    ExploreFeedTest::class,
)
class TestSuite
