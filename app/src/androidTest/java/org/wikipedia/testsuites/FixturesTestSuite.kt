package org.wikipedia.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.personalization.PersonalizationUiFixturesTest

@RunWith(Suite::class)
@SuiteClasses(
    PersonalizationUiFixturesTest::class
)
class FixturesTestSuite
