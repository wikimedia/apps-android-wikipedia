package org.wikipedia.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.DeepLinkingTest
import org.wikipedia.tests.SearchTest

@RunWith(Suite::class)
@SuiteClasses(
    OfflineTestSuite::class, // TODO: Update tests to support Jetpack Compose-based UI.
    // SuggestedEditScreenTest::class, // TODO: uncomment when login test in CI/CD is resolved
    SettingsTestSuite::class,
    ExploreFeedTestSuite::class, // TODO: Update tests to support Jetpack Compose-based UI.
    SearchTest::class, // TODO: Update tests to support Jetpack Compose-based UI.
    DeepLinkingTest::class,
    ArticlesTestSuite::class // TODO: Update tests to support Jetpack Compose-based UI.
)
class TestSuite
