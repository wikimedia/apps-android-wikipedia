package org.wikipedia.beta.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import org.wikipedia.beta.WikipediaApp;

import java.util.Locale;

public class LanguageVariantTests extends ActivityUnitTestCase<TestDummyActivity> {

    private WikipediaApp app;

    public LanguageVariantTests() {
        super(TestDummyActivity.class);
    }

    /**
     * Ensure that the more specific dialect is first in the list
     * TODO: once Chinese is updated, update tests for more languages
     */
    public void testDefaultLocaleAndAcceptLanguageAgree() throws Throwable {
        startActivity(new Intent(), null, null);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
                Locale originalLocale = Locale.getDefault();

                cover("zh-tw,zh;q=0.9", "zh", Locale.TRADITIONAL_CHINESE);
                cover("zh-cn,zh;q=0.9", "zh", Locale.SIMPLIFIED_CHINESE);
                cover("zh,en-us;q=0.9,en;q=0.8", "zh", Locale.US);
                cover("zh,en;q=0.9", "zh", Locale.ENGLISH);
                cover("en,zh-cn;q=0.9,zh;q=0.8", "en", Locale.SIMPLIFIED_CHINESE);
                cover("test,zh-cn;q=0.9,zh;q=0.8", "test", Locale.SIMPLIFIED_CHINESE);

                Locale.setDefault(originalLocale);
            }

            private void cover(String expected, String primaryLanguage, Locale locale) {
                app.setPrimaryLanguage(primaryLanguage);
                Locale.setDefault(locale);
                assertEquals(expected, app.getAcceptLanguage());
            }
        });
    }
}
