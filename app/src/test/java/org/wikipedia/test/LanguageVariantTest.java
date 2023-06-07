package org.wikipedia.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.language.AppLanguageLookUpTable;

import java.util.Collections;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(RobolectricTestRunner.class) public class LanguageVariantTest {
    private Locale defaultLocale;
    private String appLanguage;

    /** Ensure that the more specific dialect is first in the list. */
    @Test public void testDefaultLocaleAndAcceptLanguageAgree() {
        preserveAppState();

        testDefaultLocaleAndAcceptLanguageAgree("zh,zh-Hant;q=0.8", "zh",
                Locale.TRADITIONAL_CHINESE);
        testDefaultLocaleAndAcceptLanguageAgree("zh,zh-Hans;q=0.8", "zh",
                Locale.SIMPLIFIED_CHINESE);
        testDefaultLocaleAndAcceptLanguageAgree("zh,en;q=0.8", "zh", Locale.US);
        testDefaultLocaleAndAcceptLanguageAgree("zh,en;q=0.8", "zh", Locale.ENGLISH);
        testDefaultLocaleAndAcceptLanguageAgree("en,zh-Hans;q=0.8", "en",
                Locale.SIMPLIFIED_CHINESE);
        testDefaultLocaleAndAcceptLanguageAgree("test,zh-Hans;q=0.8", "test",
                Locale.SIMPLIFIED_CHINESE);
        testDefaultLocaleAndAcceptLanguageAgree("es,zh-Hans;q=0.9,zh-Hant;q=0.8",
                AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE,
                Locale.TRADITIONAL_CHINESE, WikiSite.forLanguageCode("es"));
        testDefaultLocaleAndAcceptLanguageAgree("zh-Hant",
                AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE,
                Locale.TRADITIONAL_CHINESE);

        restoreAppState();
    }

    private void testDefaultLocaleAndAcceptLanguageAgree(String expected, String appLanguage,
            Locale systemLocale) {
        testDefaultLocaleAndAcceptLanguageAgree(expected, appLanguage, systemLocale, null);
    }

    private void testDefaultLocaleAndAcceptLanguageAgree(String expected,
             String appLanguage, Locale systemLocale, WikiSite wiki) {
        WikipediaApp.Companion.getInstance().getLanguageState().setAppLanguageCodes(Collections.singletonList(appLanguage));
        Locale.setDefault(systemLocale);
        assertThat(expected, is(WikipediaApp.Companion.getInstance().getAcceptLanguage(wiki)));
    }

    private void preserveAppState() {
        preserveDefaultLocale();
        preserveAppLanguage();
    }

    private void restoreAppState() {
        restoreAppLanguage();
        restoreDefaultLocale();
    }

    private void preserveAppLanguage() {
        appLanguage = WikipediaApp.Companion.getInstance().getLanguageState().getAppLanguageCode();
    }

    private void restoreAppLanguage() {
        WikipediaApp.Companion.getInstance().getLanguageState().setAppLanguageCodes(Collections.singletonList(appLanguage));
    }

    private void preserveDefaultLocale() {
        defaultLocale = Locale.getDefault();
    }

    private void restoreDefaultLocale() {
        Locale.setDefault(defaultLocale);
    }
}
