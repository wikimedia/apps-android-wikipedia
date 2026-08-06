package org.wikipedia.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.explorefeed.HomeFeedLiveDataTest

@RunWith(Suite::class)
@SuiteClasses(
    HomeFeedLiveDataTest::class
)
class TestSuite
