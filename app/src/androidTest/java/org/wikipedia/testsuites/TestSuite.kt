package org.wikipedia.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.createaccount.CreateAccountEncourageTest

@RunWith(Suite::class)
@SuiteClasses(
    CreateAccountEncourageTest::class
)
class TestSuite
