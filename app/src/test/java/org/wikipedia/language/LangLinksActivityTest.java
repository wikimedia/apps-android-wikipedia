package org.wikipedia.language;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(RobolectricTestRunner.class)
public class LangLinksActivityTest{

    @Test public void testAddChineseEntriesForTraditionalChinese() {

        PageTitle title = new PageTitle(null, "洋基體育場 (1923年)", WikiSite.forLanguageCode("zh-hant"));
        List<PageTitle> list = getBaseLanguageEntries();

        LangLinksActivity.addVariantEntriesIfNeeded(WikipediaApp.getInstance().language(), title, list);
        assertThat(list, is(getExpectedZhHantResults()));
    }

    @Test public void testAddChineseEntriesForSimplifiedChinese() {

        PageTitle title = new PageTitle(null, "洋基体育场 (1923年)", WikiSite.forLanguageCode("zh-hans"));
        List<PageTitle> list = getBaseLanguageEntries();

        LangLinksActivity.addVariantEntriesIfNeeded(WikipediaApp.getInstance().language(), title, list);
        assertThat(list, is(getExpectedZhHansResults()));
    }

    @Test public void testSkipAddChineseEntries() {

        PageTitle title = new PageTitle(null, "Yankee Stadium (1923)", WikiSite.forLanguageCode("da"));
        List<PageTitle> list = getBaseLanguageEntriesWithZhVariants();

        LangLinksActivity.addVariantEntriesIfNeeded(WikipediaApp.getInstance().language(), title, list);
        assertThat(list, is(getExpectedGeneralResults()));
    }

    private List<PageTitle> getBaseLanguageEntries() {
        List<PageTitle> result = new ArrayList<>();
        result.add(new PageTitle("Yankee Stadium (1923)", WikiSite.forLanguageCode("en")));
        result.add(new PageTitle("ヤンキー・スタジアム (1923年)", WikiSite.forLanguageCode("ja")));
        result.add(new PageTitle("양키 스타디움 (1923년)", WikiSite.forLanguageCode("ko")));
        result.add(new PageTitle("Yankee Stadium (1923)", WikiSite.forLanguageCode("sv")));
        return result;
    }

    private List<PageTitle> getBaseLanguageEntriesWithZhVariants() {
        List<PageTitle> result = getBaseLanguageEntries();
        result.add(new PageTitle("洋基体育场 (1923年)", WikiSite.forLanguageCode("zh-hans")));
        result.add(new PageTitle("洋基体育场 (1923年)", WikiSite.forLanguageCode("zh-hant"))); // TODO: change to correct variant, an API issue
        return result;
    }

    private List<PageTitle> getExpectedGeneralResults() {
        List<PageTitle> result = getBaseLanguageEntries();
        result.add(new PageTitle("洋基体育场 (1923年)", WikiSite.forLanguageCode("zh-hans")));
        result.add(new PageTitle("洋基体育场 (1923年)", WikiSite.forLanguageCode("zh-hant"))); // TODO: change to correct variant, an API issue
        return result;
    }


    private List<PageTitle> getExpectedZhHantResults() {
        List<PageTitle> result = getBaseLanguageEntries();
        // this order follows the order in languages_list.xml
        List<String> variants = WikipediaApp.getInstance().language().getLanguageVariants("zh");
        if (variants != null) {
            for (String languageCode : variants) {
                if (!languageCode.equals("zh-hant")) {
                    result.add(new PageTitle("洋基體育場 (1923年)", WikiSite.forLanguageCode(languageCode)));
                }
            }
        }
        return result;
    }

    private List<PageTitle> getExpectedZhHansResults() {
        List<PageTitle> result = getBaseLanguageEntries();
        // this order follows the order in languages_list.xml
        List<String> variants = WikipediaApp.getInstance().language().getLanguageVariants("zh");
        if (variants != null) {
            for (String languageCode : variants) {
                if (!languageCode.equals("zh-hans")) {
                    result.add(new PageTitle("洋基体育场 (1923年)", WikiSite.forLanguageCode(languageCode)));
                }
            }
        }
        return result;
    }
}
