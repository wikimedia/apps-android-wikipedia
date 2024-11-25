package org.wikipedia

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.settings.ChangingLanguageTest

@RunWith(Suite::class)
@SuiteClasses(
    ChangingLanguageTest::class
)
class SettingsTestSuite
