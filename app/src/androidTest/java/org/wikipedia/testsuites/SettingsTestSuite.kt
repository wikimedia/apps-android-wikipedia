package org.wikipedia.testsuites

import DownloadReadingListTest
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.settings.AboutSettingsTest
import org.wikipedia.tests.settings.AppThemeTest
import org.wikipedia.tests.settings.ChangingLanguageTest
import org.wikipedia.tests.settings.CollapseTablesTest
import org.wikipedia.tests.settings.CustomizeExploreFeedTest
import org.wikipedia.tests.settings.FontChangeTest
import org.wikipedia.tests.settings.FontSizeTest
import org.wikipedia.tests.settings.LinkPreviewTest
import org.wikipedia.tests.settings.ReadingFocusModeTest
import org.wikipedia.tests.settings.ShowImageTest

@RunWith(Suite::class)
@SuiteClasses(
    ChangingLanguageTest::class,
    CustomizeExploreFeedTest::class,
    LinkPreviewTest::class,
    CollapseTablesTest::class,
    AppThemeTest::class,
    FontSizeTest::class,
    FontChangeTest::class,
    ReadingFocusModeTest::class,
    DownloadReadingListTest::class,
    ShowImageTest::class,
    AboutSettingsTest::class
)
class SettingsTestSuite
