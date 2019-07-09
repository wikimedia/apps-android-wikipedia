package org.wikipedia.language;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters;

import java.util.Arrays;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class LanguageUtilTest {
    private static final String CHINESE_LANG = "zh";
    private static final String SIMPLIFIED_WIKI_LANG = AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE;
    private static final String TRADITIONAL_WIKI_LANG = AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE;

    public static class LanguageCodeToWikiLanguageCodeChineseScript extends LanguageCodeToWikiLanguageCodeChinese<String> {
        @Parameters(name = "{0}") public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    {CHINESE_LANG, SIMPLIFIED_WIKI_LANG},
                    {"zh-Hans", SIMPLIFIED_WIKI_LANG},
                    {"zh-Hant", TRADITIONAL_WIKI_LANG},
                    {"zh-Hans-HK", SIMPLIFIED_WIKI_LANG},
                    {"zh-Hant-HK", TRADITIONAL_WIKI_LANG}
            });
        }

        public LanguageCodeToWikiLanguageCodeChineseScript(@NonNull String input,
                                                           @NonNull String expected) {
            super(input, expected);
        }

        @Test public void test() {
            Locale locale = Locale.forLanguageTag(input());
            test(locale, expected());
        }
    }

    public static class LanguageCodeToWikiLanguageCodeChineseCountry extends LanguageCodeToWikiLanguageCodeChinese<Locale> {
        @Parameters(name = "{0}") public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    {Locale.CHINESE, SIMPLIFIED_WIKI_LANG},
                    {Locale.CHINA, SIMPLIFIED_WIKI_LANG},
                    {Locale.TAIWAN, TRADITIONAL_WIKI_LANG}
            });
        }

        public LanguageCodeToWikiLanguageCodeChineseCountry(@NonNull Locale input,
                                                            @NonNull String expected) {
            super(input, expected);
        }

        @Test public void test() {
            test(input(), expected());
        }
    }

    // these tests could be much simpler with junit theories but the subject has a dependency on api
    // level which requires robolectric to change
    @RunWith(ParameterizedRobolectricTestRunner.class)
    public abstract static class LanguageCodeToWikiLanguageCodeChinese<T> {
        private static Locale PREV_DEFAULT_LOCALE;

        @NonNull private final T input;
        @NonNull private final String expected;

        @BeforeClass public static void setUpClass() {
            PREV_DEFAULT_LOCALE = Locale.getDefault();
        }

        @AfterClass public static void tearDownClass() {
            Locale.setDefault(PREV_DEFAULT_LOCALE);
        }

        LanguageCodeToWikiLanguageCodeChinese(@NonNull T input, @NonNull String expected) {
            this.input = input;
            this.expected = expected;
        }

        @NonNull T input() {
            return input;
        }

        @NonNull String expected() {
            return expected;
        }

        void test(@NonNull Locale defaultLocale, @Nullable String expected) {
            Locale.setDefault(defaultLocale);
            String wikiLang = LanguageUtil.localeToWikiLanguageCode(defaultLocale);
            assertThat(wikiLang, is(expected));
        }
    }
}
